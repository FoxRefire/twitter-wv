(function () {
  "use strict";
  
  let adCount = 0;
  
  function log() {
      return console.info("[Twitter AD Filter]", ...arguments);
  }
  
  function hideAd(node) {
      try {
          if (
              !node ||
              node.nodeName !== "DIV" ||
              node.getAttribute("data-testid") !== "cellInnerDiv"
          ) {
              return;
          }
          
          const adArticle = node.querySelector(
              "div[data-testid='placementTracking'] > article"
          );
          if (!adArticle) {
              return;
          }
          
          const userName = adArticle.querySelector("div[data-testid='User-Name']");
          log("发现广告:", ++adCount, userName ? userName.innerText : "未知用户");
          
          node.style.cssText += "display: none;";
      } catch (err) {
          log("发生错误:", err.message);
      }
  }
  
  const pageObserver = new MutationObserver(function (mutations) {
      mutations.forEach(function (mutation) {
          mutation.addedNodes.forEach(hideAd);
      });
      
      const sidebarAd = document.querySelector("#react-root > div > div > div.css-175oi2r.r-1f2l425.r-13qz1uu.r-417010.r-18u37iz > main > div > div > div > div.css-175oi2r.r-aqfbo4.r-10f7w94.r-1hycxz > div > div.css-175oi2r.r-1hycxz.r-gtdqiz > div > div > div > div:nth-child(3) > div > aside");
      if (sidebarAd) {
          sidebarAd.style.display = 'none';
          log("已隐藏右侧栏广告");
      }
  });
  
  pageObserver.observe(document.body, {
      childList: true,
      subtree: true,
  });
  
  document.querySelectorAll("div[data-testid='cellInnerDiv']").forEach(hideAd);
  
  log("--- 广告过滤器已启动 ---");
})();
