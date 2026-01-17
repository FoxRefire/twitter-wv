(function() {
    "use strict";
    
    // Prevent multiple executions
    if (window.__swipeToCtxMenuExecuted) {
        return;
    }
    window.__swipeToCtxMenuExecuted = true;
    
    try {
        let touchStartX = null;
        let touchStartY = null;
        const SWIPE_THRESHOLD = 100; // Minimum swipe distance in pixels
        const LEFT_EDGE_THRESHOLD = 50; // Maximum X position to consider as left edge
        const VERTICAL_THRESHOLD = 50; // Maximum vertical movement to consider as horizontal swipe
        
        const targetSelector = '#layers > div:nth-child(2) > div > div > div > div.css-175oi2r.r-105ug2t.r-1e5uvyk.r-5zmot > div > div.css-175oi2r.r-136ojw6 > div > div > div > div > div.css-175oi2r.r-1pz39u2.r-1777fci.r-1vsu8ta.r-1habvwh.r-2j7rtt.r-1t2qqvi.r-16y2uox.r-1wbh5a2 > div > button';
        
        function clickTargetElement() {
            const element = document.querySelector(targetSelector);
            if (element) {
                element.click();
                console.log('swipeToCtxMenu: Target element clicked');
            } else {
                console.log('swipeToCtxMenu: Target element not found');
            }
        }
        
        function handleTouchStart(e) {
            const touch = e.touches[0];
            touchStartX = touch.clientX;
            touchStartY = touch.clientY;
        }
        
        function handleTouchEnd(e) {
            if (touchStartX === null || touchStartY === null) {
                return;
            }
            
            const touch = e.changedTouches[0];
            const touchEndX = touch.clientX;
            const touchEndY = touch.clientY;
            
            const deltaX = touchEndX - touchStartX;
            const deltaY = Math.abs(touchEndY - touchStartY);
            
            // Check if swipe started from left edge and moved right
            if (touchStartX <= LEFT_EDGE_THRESHOLD && 
                deltaX >= SWIPE_THRESHOLD && 
                deltaY <= VERTICAL_THRESHOLD) {
                clickTargetElement();
            }
            
            // Reset
            touchStartX = null;
            touchStartY = null;
        }
        
        // Add event listeners
        document.addEventListener('touchstart', handleTouchStart, { passive: true });
        document.addEventListener('touchend', handleTouchEnd, { passive: true });
        
        console.log('swipeToCtxMenu: Swipe detection initialized');
    } catch (err) {
        console.error('swipeToCtxMenu.js error:', err);
        window.__swipeToCtxMenuExecuted = false; // Reset on error to allow retry
    }
})();
