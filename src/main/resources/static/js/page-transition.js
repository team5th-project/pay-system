/**
 * 같은 출처 페이지 이동 시 몽환적 전환(오버레이 + 본문 페이드) 후 라우팅.
 * prefers-reduced-motion 이면 브라우저 기본 동작만 사용.
 */
(function () {
    var EXIT_MS = 580;

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function isInternalNavigation(anchor) {
        var href = anchor.getAttribute('href');
        if (!href || href.indexOf('#') === 0 || href.indexOf('javascript:') === 0) return false;
        if (anchor.target === '_blank' || anchor.hasAttribute('download')) return false;
        if (anchor.hasAttribute('data-no-transition')) return false;
        try {
            var u = new URL(anchor.href, window.location.href);
            return u.origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    function runExitTransition(done) {
        if (prefersReducedMotion()) {
            done();
            return;
        }
        var overlay = document.getElementById('page-transition-overlay');
        document.body.classList.add('page-transition-exiting');
        if (overlay) {
            overlay.classList.add('is-active');
            overlay.setAttribute('aria-hidden', 'false');
        }
        setTimeout(done, EXIT_MS);
    }

    window.navigateWithMagicTransition = function (url) {
        if (!url) return;
        if (prefersReducedMotion()) {
            window.location.href = url;
            return;
        }
        runExitTransition(function () {
            window.location.href = url;
        });
    };

    document.addEventListener(
        'click',
        function (e) {
            if (prefersReducedMotion()) return;
            var a = e.target.closest && e.target.closest('a[href]');
            if (!a || !isInternalNavigation(a)) return;
            if (e.defaultPrevented) return;
            if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;

            var next;
            try {
                next = new URL(a.href, window.location.href);
            } catch (err) {
                return;
            }
            var cur = new URL(window.location.href);
            if (next.pathname === cur.pathname && next.search === cur.search) {
                return;
            }

            e.preventDefault();
            runExitTransition(function () {
                window.location.href = next.href;
            });
        },
        true
    );

    document.addEventListener('DOMContentLoaded', function () {
        var ov = document.getElementById('page-transition-overlay');
        if (ov) ov.setAttribute('aria-hidden', 'true');
    });
})();
