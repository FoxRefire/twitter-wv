(function() {
    const BIRD_IMG_B64 = `data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJAAAACQAgMAAACaKAorAAAACXBIWXMAAAsTAAALEwEAmpwYAAAACVBMVEVHcEwDqfQCqfQiXeLhAAAAA3RSTlMA/4ZtiR+5AAAByUlEQVRYw+2Xv27DIBDGwZIHZ273LpaqPoUfgSEgK5PHPgZdu7tzFg/NU9Z/1HAHB3dSupWbAvoFPr77HBOlatWq9S+rWeyVhWZrrWeYbmXsefv0Wl5oX6oZ4fQLUrQzdgUWA+ef4KA9ILduO8H5Lzh4PiD7GalHIxsKq5gSSceOCBojA+7aQ2n4nTZAEzy2tna4D/oA+dOAdjDJ4VbTm9kjyEV+b3VByjvYUOAAUr5BJnUAijggR0FDnA1/PyntuAb7a5s1MywFoSmFxrgrOAQNmAPQGQcWSOCgXYPOQnMQkYd+m+quBShk6PaRheC5s5CWQIqERkUrL0CdbwXQpRFA1lGQETgQQfoBaFJEnv4Aog1HDxR6tAsQ2ZX4p5zsilWSPClBnpwS5CnOHKk8gVpBMEnPTebNVOoKbcKQvuQWzvCtTjzUCbykLD9LoDGVJLGJ8Cm1iWgeYVPPpYn0wFF3GN4BQrmhoJ4/XLqfV/x+LnP94nXHYTHFG1je71RV9k4okIScMoKFshfInncJLzQKjpYxoFnYLN2+BTGZJQno2CcuXcrzrc2eHy7lvKC5E3Ob3+q9dOd/EzDrjqujF/5PRq1atWo9XD/equsjspFMDgAAAABJRU5ErkJggg==`
    const LOADING_SELECTOR = `div[aria-label="Loading…"] > svg`;
    const HEADER_SELECTOR = `#layers > div:nth-child(2) > div > div > div > div.css-175oi2r.r-105ug2t.r-1e5uvyk.r-5zmot > div > div.css-175oi2r.r-136ojw6 > div > div > div > div > div.css-175oi2r.r-16y2uox.r-1wbh5a2.r-1pi2tsx.r-1777fci.r-1awozwy > svg`;

    const iv1 = setInterval(() => {
        const loading = document.querySelector(LOADING_SELECTOR);
        const img = Object.assign(new Image(72), { src: BIRD_IMG_B64 });
        if (loading) {
            loading.replaceWith(img)
            clearInterval(iv1);
        }
    }, 50);
    const iv2 = setInterval(() => {
        const header = document.querySelector(HEADER_SELECTOR);
        const img = Object.assign(new Image(30), { src: BIRD_IMG_B64 });
        if (header) {
            header.replaceWith(img)
            clearInterval(iv2);
        }
    }, 50);
})();