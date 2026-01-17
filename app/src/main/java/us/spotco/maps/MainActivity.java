/*
Copyright (c) 2017-2019 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package us.spotco.maps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import org.woheller69.freeDroidWarn.FreeDroidWarn;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private WebView twitterWebView = null;
    private WebSettings twitterWebSettings = null;
    private CookieManager twitterCookieManager = null;
    private final Context context = this;
    private LocationManager locationManager;

    private static final ArrayList<String> allowedDomains = new ArrayList<String>();
    private static final ArrayList<String> allowedDomainsStart = new ArrayList<String>();
    private static final ArrayList<String> allowedDomainsEnd = new ArrayList<String>();
    private static final ArrayList<String> blockedURLs = new ArrayList<String>();

    private static final String TAG = "XWV";
    private static LocationListener locationListenerGPS;
    private static final boolean canUseLocation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    private static int locationRequestCount = 0;

    @Override
    protected void onPause() {
        super.onPause();
        if (canUseLocation && locationListenerGPS != null) removeLocationListener();
        // Flush cookies to ensure persistence
        if (twitterCookieManager != null) {
            twitterCookieManager.flush();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (canUseLocation) {
            locationListenerGPS = getNewLocationListener();
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListenerGPS);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= 29) {
            boolean nightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(twitterWebSettings, nightMode);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String urlToLoad = "https://x.com";
        try {
            Intent intent = getIntent();
            Uri data = intent.getData();
            urlToLoad = data.toString();
            if (data.toString().startsWith("https://")) {
                urlToLoad = data.toString();
            } else if (data.toString().startsWith("twitter://")) {
                // Convert twitter:// URLs to https://x.com or https://twitter.com
                String twitterUrl = data.toString().replace("twitter://", "https://x.com/");
                urlToLoad = twitterUrl;
            }
        } catch (Exception e) {
            Log.d(TAG, "No or Invalid URL passed. Opening homepage instead.");
        }

        //Create the WebView
        twitterWebView = findViewById(R.id.mapsWebView);

        //Set cookie options
        twitterCookieManager = CookieManager.getInstance();
        // Don't reset WebView on create to preserve cookies and login session
        // Only initialize settings without clearing cookies
        twitterCookieManager.setAcceptCookie(true);
        twitterCookieManager.setAcceptThirdPartyCookies(twitterWebView, false);
        initURLs();

        //Lister for Link sharing
        initShareLinkListener();

        //Give location access
        twitterWebView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (canUseLocation) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if(locationRequestCount < 2) { //Don't annoy the user
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.title_location_permission)
                                    .setMessage(R.string.text_location_permission)
                                    .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {
                                        //Disable prompts
                                        locationRequestCount = 100;
                                    }).setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                        //Prompt the user once explanation has been shown
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                                    })
                                    .create()
                                    .show();
                        }
                        locationRequestCount++;
                    } else {
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            Toast.makeText(context, R.string.error_no_gps, Toast.LENGTH_LONG).show();
                        }
                    }
                }
                if (origin.contains("x.com") || origin.contains("twitter.com")) {
                    callback.invoke(origin, true, false);
                }
            }
        });

        twitterWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().equals("about:blank")) {
                    return null;
                }
                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldInterceptRequest][NON-HTTPS] Blocked access to " + request.getUrl().toString());
                    return new WebResourceResponse("text/javascript", "UTF-8", null); //Deny URLs that aren't HTTPS
                }
                boolean allowed = false;
                for (String url : allowedDomains) {
                    if (request.getUrl().getHost().equals(url)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    for (String url : allowedDomainsStart) {
                        if (request.getUrl().getHost().startsWith(url)) {
                            allowed = true;
                            break;
                        }
                    }
                }
                if (!allowed) {
                    for (String url : allowedDomainsEnd) {
                        if (request.getUrl().getHost().endsWith(url)) {
                            allowed = true;
                            break;
                        }
                    }
                }
                if (!allowed) {
                    Log.d(TAG, "[shouldInterceptRequest][NOT ON ALLOWLIST] Blocked access to " + request.getUrl().getHost());
                    return new WebResourceResponse("text/javascript", "UTF-8", null); //Deny URLs not on ALLOWLIST
                }
                for (String url : blockedURLs) {
                    if (request.getUrl().toString().contains(url)) {
                        if (request.getUrl().toString().contains("/log204?")) {
                            Log.d(TAG, "[shouldInterceptRequest][ON DENYLIST] Blocked access to a log204 request");
                        } else {
                            Log.d(TAG, "[shouldInterceptRequest][ON DENYLIST] Blocked access to " + request.getUrl().toString());
                        }
                        return new WebResourceResponse("text/javascript", "UTF-8", null); //Deny URLs on DENYLIST
                    }
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().equals("about:blank")) {
                    return false;
                }
                if (request.getUrl().toString().startsWith("tel:")) {
                    Intent dial = new Intent(Intent.ACTION_DIAL, request.getUrl());
                    startActivity(dial);
                    return true;
                }
                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NON-HTTPS] Blocked access to " + request.getUrl().toString());
                    if (request.getUrl().toString().startsWith("intent://t.co/")){
                        String url = request.getUrl().toString();
                        String encodedURL = url.split("intent://t\\.co/")[1];
                        try {
                            String decodedURL = "https://t.co/" + URLDecoder.decode(encodedURL, "UTF-8");
                            twitterWebView.loadUrl(decodedURL);
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (request.getUrl().toString().startsWith("http://")){
                        new AlertDialog.Builder(context)
                            .setTitle(R.string.title_open_link)
                            .setIcon(R.drawable.ic_warning) // Set the alert icon
                            .setMessage(context.getString(R.string.text_warning_link) + "\n\n" + context.getString(R.string.text_open_link, request.getUrl().toString()))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(
                                    android.R.string.ok,
                                    (dialogInterface, i) ->
                                            startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()))
                            )
                            .create()
                            .show();
                    }
                    return true; //Deny URLs that aren't HTTPS
                }
                boolean allowed = false;
                for (String url : allowedDomains) {
                    if (request.getUrl().getHost().equals(url)) {
                        allowed = true;
                    }
                }
                for (String url : allowedDomainsStart) {
                    if (request.getUrl().getHost().startsWith(url)) {
                        allowed = true;
                    }
                }
                for (String url : allowedDomainsEnd) {
                    if (request.getUrl().getHost().endsWith(url)) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NOT ON ALLOWLIST] Blocked access to " + request.getUrl().getHost());
                    if (request.getUrl().toString().startsWith("https://")) {
                        new AlertDialog.Builder(context)
                            .setTitle(R.string.title_open_link)
                            .setMessage(context.getString(R.string.text_open_link, request.getUrl().toString()))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(
                                android.R.string.ok,
                                (dialogInterface, i) ->
                                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()))
                            )
                            .create()
                            .show();
                    }

                    return true; //Deny URLs not on ALLOWLIST
                }
                for (String url : blockedURLs) {
                    if (request.getUrl().toString().contains(url)) {
                        Log.d(TAG, "[shouldOverrideUrlLoading][ON DENYLIST] Blocked access to " + request.getUrl().toString());
                        return true; //Deny URLs on DENYLIST
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Flush cookies after page load to ensure persistence
                if (url.contains("x.com") || url.contains("twitter.com")) {
                    twitterCookieManager.flush();
                }
                //Remove banners and improve mobile experience
                view.evaluateJavascript("(function(){" +
                        "var head = document.getElementsByTagName('head');" +
                        "if (head.length > 0) {" +
                        "    var appBarStyle = document.createElement('style');" +
                        "    appBarStyle.setAttribute('type', 'text/css');" +
                        "    appBarStyle.textContent = `[data-testid='app-bar-cover'] {" +
                        "        display: none !important;" +
                        "    }`;" +
                        "    head[0].appendChild(appBarStyle);" +
                        "}" +
                        "})();",null);
                
                // Inject scripts for X (Twitter)
                if (url.contains("x.com") || url.contains("twitter.com")) {
                    injectScript(view, "adRemove.js");
                    injectScript(view, "premiumRemove.js");
                    injectScript(view, "swipeToCtxMenu.js");
                    injectScript(view, "returnBird.js");
                }
            }
        });

        //Set more options
        twitterWebSettings = twitterWebView.getSettings();
        if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= 29) {
            boolean nightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(twitterWebSettings, nightMode);
        }
        //Enable some WebView features
        twitterWebSettings.setJavaScriptEnabled(true);
        twitterWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        twitterWebSettings.setGeolocationEnabled(true);
        //Disable some WebView features
        twitterWebSettings.setAllowContentAccess(false);
        twitterWebSettings.setAllowFileAccess(false);
        twitterWebSettings.setBuiltInZoomControls(false);
        twitterWebSettings.setDatabaseEnabled(false);
        twitterWebSettings.setDisplayZoomControls(false);
        //Enable DomStorage for X (Twitter) - it requires LocalStorage/SessionStorage
        twitterWebSettings.setDomStorageEnabled(true);
        twitterWebSettings.setSaveFormData(false);
        //Change the User-Agent
        twitterWebSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36");

        //Load X (Twitter)
        twitterWebView.loadUrl(urlToLoad);
        FreeDroidWarn.showWarningOnUpgrade(this, BuildConfig.VERSION_CODE);
        if (GithubStar.shouldShowStarDialog(this)) GithubStar.starDialog(this, "https://github.com/woheller69/maps");
    }

    @Override
    protected void onDestroy() {
        resetWebView(true);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Credit (CC BY-SA 3.0): https://stackoverflow.com/a/6077173
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (twitterWebView.canGoBack() && !twitterWebView.getUrl().equals("about:blank")) {
                        twitterWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void resetWebView(boolean exit) {
        if (twitterWebView == null) return;
        
        twitterWebView.clearFormData();
        twitterWebView.clearHistory();
        twitterWebView.clearMatches();
        twitterWebView.clearSslPreferences();
        twitterWebView.clearCache(true);
        
        // FIX: Don't remove cookies and WebStorage on exit to preserve login session
        // Only clear cookies on initial setup, not when app is destroyed
        if (!exit) {
            // Only clear cookies during initial setup
            twitterCookieManager.removeSessionCookie();
            twitterCookieManager.removeAllCookie();
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            WebStorage.getInstance().deleteAllData();
        }
        // When exit=true (onDestroy), preserve cookies and WebStorage to maintain login
        // Ensure cookies are flushed before destroying WebView
        if (exit) {
            twitterCookieManager.flush();
            twitterWebView.loadUrl("about:blank");
            twitterWebView.removeAllViews();
            twitterWebSettings.setJavaScriptEnabled(false);
            twitterWebView.destroyDrawingCache();
            twitterWebView.destroy();
            twitterWebView = null;
        }
    }

    private static void initURLs() {
        //Allowed Domains
        allowedDomains.add("x.com");
        allowedDomains.add("twitter.com");
        allowedDomains.add("www.x.com");
        allowedDomains.add("www.twitter.com");
        allowedDomains.add("mobile.twitter.com");
        allowedDomains.add("mobile.x.com");
        allowedDomains.add("t.co");
        allowedDomains.add("twimg.com");
        allowedDomains.add("abs.twimg.com");
        allowedDomains.add("pbs.twimg.com");
        allowedDomains.add("video.twimg.com");
        allowedDomains.add("ton.twimg.com");
        allowedDomains.add("api.twitter.com");
        allowedDomains.add("api.x.com");
        allowedDomains.add("syndication.twitter.com");
        allowedDomains.add("cdn.syndication.twimg.com");
        allowedDomainsEnd.add(".twimg.com");
        allowedDomainsEnd.add(".twitter.com");
        allowedDomainsEnd.add(".x.com");

        //Blocked Domains
        blockedURLs.add("ads-twitter.com");
        blockedURLs.add("analytics.twitter.com");
        blockedURLs.add("ads-api.twitter.com");
        blockedURLs.add("ads-api.x.com");
        blockedURLs.add("ads.x.com");
        blockedURLs.add("ads.twitter.com");
        blockedURLs.add("advertising.twitter.com");
        blockedURLs.add("advertising.x.com");
        blockedURLs.add("adsrvr.org");
        blockedURLs.add("advertising.com");
        blockedURLs.add("doubleclick.net");
        blockedURLs.add("googleadservices.com");
        blockedURLs.add("googlesyndication.com");

        //Blocked URLs
        blockedURLs.add("/i/adsct");
        blockedURLs.add("/i/adsct?");
        blockedURLs.add("/analytics");
        blockedURLs.add("/analytics?");
        blockedURLs.add("/1.1/guest/activate.json");
        blockedURLs.add("/2/timeline/home.json");
    }

    private LocationListener getNewLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
            }

            @Deprecated
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }

    private void removeLocationListener() {
        if (locationListenerGPS != null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationListenerGPS != null) locationManager.removeUpdates(locationListenerGPS);
        }
        locationListenerGPS = null;
    }

    private void initShareLinkListener() {
        // Clipboard listener removed for X (Twitter) - not needed for this use case
    }
    
    private void injectScript(WebView view, String scriptFileName) {
        try {
            InputStream inputStream = getAssets().open(scriptFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder script = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                script.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            
            // Inject the script into the page
            view.evaluateJavascript(script.toString(), null);
            Log.d(TAG, "Script injected successfully: " + scriptFileName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load script: " + scriptFileName, e);
        }
    }
}
