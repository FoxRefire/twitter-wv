(function() {
    "use strict";
    
    // Prevent multiple executions
    if (window.__premiumRemoveExecuted) {
        return;
    }
    window.__premiumRemoveExecuted = true;
    
    try {
        const premiumStyle = document.createElement('style');
        premiumStyle.setAttribute('id', 'premium-remove-style');
        
        premiumStyle.textContent = `
            div.r-1pz39u2:nth-child(3) > div:nth-child(1),
            div.css-175oi2r:nth-child(8),
            div.css-175oi2r:nth-child(9),
            div.jmffnto0:nth-child(2),
            div.jy49p8z0:nth-child(3) > div:nth-child(2) > button:nth-child(3) {
                display: none !important;
            }
        `;
        
        // Remove existing style if present
        const existingStyle = document.getElementById('premium-remove-style');
        if (existingStyle) {
            existingStyle.remove();
        }
        
        document.head.appendChild(premiumStyle);
    } catch (err) {
        console.error('premiumRemove.js error:', err);
        window.__premiumRemoveExecuted = false; // Reset on error to allow retry
    }
})();