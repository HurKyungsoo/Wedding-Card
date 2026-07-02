'use strict';

/* ── 날짜·시간 포맷 ── */
function formatDate(s) {
    if (!s) return '';
    const d = new Date(s);
    const wd = ['일','월','화','수','목','금','토'][d.getDay()];
    return d.getFullYear() + '년 ' + (d.getMonth()+1) + '월 ' + d.getDate() + '일 ' + wd + '요일';
}
function formatTime(t) {
    if (!t) return '';
    const [h, m] = t.split(':').map(Number);
    const ampm = h >= 12 ? '오후' : '오전';
    const hh = h > 12 ? h - 12 : h === 0 ? 12 : h;
    return ampm + ' ' + hh + '시 ' + (m === 0 ? '00' : m) + '분';
}

/* ── 달력 ── */
function buildCalendar(dateStr) {
    const grid  = document.getElementById('calGrid');
    const title = document.getElementById('calTitle');
    if (!grid || !title || !dateStr) return;
    const d = new Date(dateStr);
    const y = d.getFullYear(), m = d.getMonth(), day = d.getDate();
    const first = new Date(y, m, 1).getDay();
    const last  = new Date(y, m+1, 0).getDate();
    title.textContent = y + '년 ' + (m+1) + '월';
    const dows = ['일','월','화','수','목','금','토'];
    let html = dows.map((w, i) =>
        '<div class="cal-dow" style="' + (i===0?'color:#d4845a':i===6?'color:#7a8ab8':'') + '">' + w + '</div>'
    ).join('');
    for (let i = 0; i < first; i++) html += '<div class="cal-cell empty"></div>';
    for (let n = 1; n <= last; n++) {
        const dow = (first + n - 1) % 7;
        let cls = 'cal-cell';
        if (n === day)      cls += ' wed';
        else if (dow === 0) cls += ' sun';
        else if (dow === 6) cls += ' sat';
        html += '<div class="' + cls + '">' + n + '</div>';
    }
    grid.innerHTML = html;
}

/* ── D-Day ── */
function calcDday(dateStr) {
    const pill = document.getElementById('ddayPill');
    if (!pill || !dateStr) return;
    const today = new Date(); today.setHours(0,0,0,0);
    const wed   = new Date(dateStr); wed.setHours(0,0,0,0);
    const diff  = Math.round((wed - today) / 86400000);
    pill.textContent = diff > 0 ? 'D-' + diff : diff === 0 ? 'D-DAY' : 'D+' + Math.abs(diff);
}

/* ── 갤러리 슬라이더 ── */
/* ── 청첩장 지도 초기화 (카카오 JS SDK 방식) ── */
var _invMap = null;
var _invMarker = null;

function initInvitationMap() {
    var mapDiv = document.getElementById('invitationMap');
    if (!mapDiv) return;

    var lat       = parseFloat(mapDiv.getAttribute('data-lat'));
    var lng       = parseFloat(mapDiv.getAttribute('data-lng'));
    var place     = mapDiv.getAttribute('data-place') || '';
    var kakaoLink = mapDiv.getAttribute('data-kakaolink') || '';

    if (!lat || !lng || isNaN(lat) || isNaN(lng)) return;

    if (window._kakaoReady && typeof kakao !== 'undefined' && kakao.maps) {
        _drawInvitationMap(mapDiv, lat, lng, place, kakaoLink);
    } else {
        /* SDK 로딩 대기 */
        var tries = 0;
        var poll = setInterval(function() {
            tries++;
            if (window._kakaoReady) {
                clearInterval(poll);
                _drawInvitationMap(mapDiv, lat, lng, place, kakaoLink);
            } else if (tries > 40) {
                clearInterval(poll);
            }
        }, 150);
    }
}

function _drawInvitationMap(mapDiv, lat, lng, place, kakaoLink) {
    var pos = new kakao.maps.LatLng(lat, lng);

    if (_invMap) {
        _invMap.relayout();
        _invMap.setCenter(pos);
        if (_invMarker) _invMarker.setPosition(pos);
        mapDiv.onclick = function() { if (kakaoLink) window.open(kakaoLink, '_blank'); };
        return;
    }

    mapDiv.style.height = '280px';
    mapDiv.style.cursor = 'pointer';
    mapDiv.innerHTML = '';

    if (!mapDiv.offsetWidth) {
        requestAnimationFrame(function() { _drawInvitationMap(mapDiv, lat, lng, place, kakaoLink); });
        return;
    }

    _invMap = new kakao.maps.Map(mapDiv, {
        center: pos,
        level: 3,
        draggable: false,
        scrollwheel: false,
        disableDoubleClick: true,
        disableDoubleClickZoom: true
    });

    setTimeout(function() {
        if (_invMap) { _invMap.relayout(); _invMap.setCenter(pos); }
    }, 100);

    _invMarker = new kakao.maps.Marker({ map: _invMap, position: pos });

    if (place) {
        var info = new kakao.maps.InfoWindow({
            content: '<div style="padding:4px 8px;font-size:12px;white-space:nowrap;font-family:sans-serif;">' + place + '</div>'
        });
        info.open(_invMap, _invMarker);
    }

    mapDiv.onclick = function() { if (kakaoLink) window.open(kakaoLink, '_blank'); };
}

function initGallery() {
    const outer  = document.getElementById('galleryOuter');
    const track  = document.getElementById('galleryTrack');
    const dotsEl = document.getElementById('galleryDots');
    const btnP   = document.getElementById('galleryPrev');
    const btnN   = document.getElementById('galleryNext');
    if (!outer || !track) return;

    /* 갤러리 타입별 처리 */
    var galType = outer.getAttribute('data-galtype') || 'slide';

    /* 썸네일 스와이프형 — invitation.html renderGallery와 동일 구조로 초기화 */
    if (galType === 'thumb_swipe') {
        var imgs = [];
        track.querySelectorAll('img').forEach(function(img) { imgs.push(img.src); });
        if (!imgs.length) return;
        var arrows = outer.querySelector('.gallery-arrows');
        if (arrows) arrows.style.display = '';
        var tsIdx = 0;

        /* 초기 DOM 구성 (1회만) */
        track.innerHTML =
            '<div class="ts-main-wrap">' +
            '  <img class="ts-main-img" src="' + imgs[0] + '" alt="갤러리">' +
            '</div>' +
            '<div class="ts-thumb-row" id="tsThumbRowInit">' +
            imgs.map(function(u, i) {
                return '<img class="ts-thumb' + (i === 0 ? ' active' : '') + '" src="' + u + '" data-idx="' + i + '" alt="썸네일">';
            }).join('') +
            '</div>';

        var mainImg = track.querySelector('.ts-main-img');
        var thumbs  = track.querySelectorAll('.ts-thumb');

        /* 메인 이미지만 교체 (DOM 재생성 없이 부드럽게) */
        function updateTS(idx) {
            tsIdx = idx;
            if (mainImg) {
                mainImg.style.opacity = '0';
                mainImg.style.transition = 'opacity 0.2s';
                setTimeout(function() {
                    mainImg.src = imgs[tsIdx];
                    mainImg.style.opacity = '1';
                }, 180);
            }
            thumbs.forEach(function(th) {
                th.classList.toggle('active', parseInt(th.dataset.idx) === tsIdx);
            });
            /* 선택 썸네일 스크롤 */
            if (thumbs[tsIdx]) {
                thumbs[tsIdx].scrollIntoView({behavior:'smooth', block:'nearest', inline:'center'});
            }
        }

        /* 썸네일 클릭 */
        thumbs.forEach(function(th) {
            th.onclick = function() { updateTS(parseInt(this.dataset.idx)); };
        });

        /* 화살표 버튼 */
        var btnP2 = document.getElementById('galleryPrev');
        var btnN2 = document.getElementById('galleryNext');
        if (btnP2) btnP2.onclick = function() { updateTS(Math.max(0, tsIdx-1)); };
        if (btnN2) btnN2.onclick = function() { updateTS(Math.min(imgs.length-1, tsIdx+1)); };

        /* 메인 스와이프 */
        var mainWrap = track.querySelector('.ts-main-wrap');
        if (mainWrap) {
            var sx = 0;
            mainWrap.ontouchstart = function(e) { sx = e.touches[0].clientX; };
            mainWrap.ontouchend   = function(e) {
                var dx = e.changedTouches[0].clientX - sx;
                if (Math.abs(dx) > 40) { updateTS(dx < 0 ? Math.min(imgs.length-1, tsIdx+1) : Math.max(0, tsIdx-1)); }
            };
        }
        return;
    }

    if (galType !== 'slide') {
        var arrows = outer.querySelector('.gallery-arrows');
        if (arrows) arrows.style.display = 'none';
        return;
    }

    const slides = track.querySelectorAll('.gallery-slide');
    const total  = slides.length;
    const maxIdx = Math.max(0, total - 1);
    let gIdx = 0;

    function update() {
        if (total === 0) return;
        const w = slides[0].offsetWidth || outer.offsetWidth;  /* fallback */
        track.style.transform = 'translateX(-' + (gIdx * w) + 'px)';
        if (!dotsEl) return;
        let dots = '';
        for (let i = 0; i <= maxIdx; i++) {
            dots += '<button class="gdot ' + (i === gIdx ? 'active' : '') + '" data-idx="' + i + '" aria-label="슬라이드 ' + (i+1) + '"></button>';
        }
        dotsEl.innerHTML = dots;
        dotsEl.querySelectorAll('.gdot').forEach(function(btn) {
            btn.addEventListener('click', function() { gIdx = parseInt(btn.dataset.idx); update(); });
        });
    }
    if (btnP) btnP.addEventListener('click', function() { gIdx = Math.max(0, gIdx-1); update(); });
    if (btnN) btnN.addEventListener('click', function() { gIdx = Math.min(maxIdx, gIdx+1); update(); });

    // 터치 스와이프
    var startX = 0;
    outer.addEventListener('touchstart', function(e) { startX = e.touches[0].clientX; }, { passive: true });
    outer.addEventListener('touchend',   function(e) {
        var dx = e.changedTouches[0].clientX - startX;
        if (Math.abs(dx) > 40) { gIdx = dx < 0 ? Math.min(maxIdx, gIdx+1) : Math.max(0, gIdx-1); update(); }
    }, { passive: true });

    /* 이미지 로딩 후에도 재계산 (offsetWidth 0 방지) */
    update();
    setTimeout(update, 100);
    window.addEventListener('resize', update);
    /* 첫 이미지 로드 완료 시 재계산 */
    var firstImg = track.querySelector('img');
    if (firstImg && !firstImg.complete) {
        firstImg.addEventListener('load', update);
    }
}

/* ── 꽃잎 애니메이션 ── */
function initPetals() {
    var container = document.getElementById('petals');
    if (!container) return;
    var symbols = ['❀','✿','❁','✾','❃','✽'];
    var colors  = ['#f5c9d4','#f9dfe8','#f7b8c9','#fce8f0','#e8d5e0','#f2dde5'];
    for (var i = 0; i < 22; i++) {
        var el = document.createElement('div');
        el.className   = 'petal';
        el.textContent = symbols[i % symbols.length];
        el.style.cssText = 'left:' + (Math.random()*100) + '%;font-size:' + (7+Math.random()*11) + 'px;color:' + colors[i%colors.length] + ';animation-duration:' + (7+Math.random()*10) + 's;animation-delay:' + (Math.random()*12) + 's;';
        container.appendChild(el);
    }
}

/* ── 스크롤 인터섹션 옵저버 (핵심 스크롤 효과) ── */
function initScrollEffects() {
    // 0) 고급 스크롤 효과 자동 적용 (모바일 청첩장 스타일)
    initAdvancedScrollEffects();

    // 1) 일반 reveal 요소들 (모든 타입 포함)
    var revealEls = document.querySelectorAll('.reveal, .reveal-left, .reveal-right, .reveal-scale, .reveal-blur, .reveal-zoom, .reveal-fade, .section-divider');

    var revealObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('on');
                revealObs.unobserve(entry.target); // 한 번 보이면 해제
            }
        });
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }); // 요소가 좀 더 보일 때 등장

    revealEls.forEach(function(el) {
        // 이미 뷰포트 안에 있는 요소는 즉시 표시
        var rect = el.getBoundingClientRect();
        if (rect.top < window.innerHeight && rect.bottom > 0) {
            el.classList.add('on');
        } else {
            revealObs.observe(el);
        }
    });

    // 2) 정보 아이템 순차 등장
    var infoItems = document.querySelectorAll('.info-item');
    var infoObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                var items = entry.target.querySelectorAll('.info-item');
                items.forEach(function(item, i) {
                    setTimeout(function() { item.classList.add('on'); }, i * 120);
                });
                infoObs.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });
    var infoBlock = document.querySelector('.info-block');
    if (infoBlock) infoObs.observe(infoBlock);

    // 3) 달력 섹션 트리거
    var calSec = document.getElementById('calGrid');
    if (calSec) {
        var calObs = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('cal-visible');
                    calObs.unobserve(entry.target);
                }
            });
        }, { threshold: 0.3 });
        calObs.observe(calSec);
    }

    // 4) 패럴랙스 히어로 (스크롤 시 안쪽 콘텐츠 천천히 올라감)
    var heroInner = document.querySelector('.hero-inner');
    function onScroll() {
        var sy = window.scrollY;
        if (heroInner && sy < window.innerHeight) {
            heroInner.style.transform = 'translateY(' + (sy * 0.3) + 'px)';
        }
    }
    window.addEventListener('scroll', onScroll, { passive: true });

    // 5) 섹션 배경 색상 전환 (스크롤 진행에 따라)
    var sections = document.querySelectorAll('.sec');
    var secObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.style.transition = 'background 0.6s ease';
            }
        });
    }, { threshold: 0.3 });
    sections.forEach(function(s) { secObs.observe(s); });
}

/* ════════════════════════════════════════
   고급 스크롤 효과 (모바일 청첩장 스타일)
   ════════════════════════════════════════ */
function initAdvancedScrollEffects() {
    /* 1) 사진 패럴랙스 — 스크롤 시 사진이 천천히 움직임 */
    var heroImg = document.querySelector('.hero-fullphoto-img');
    var galleryImgs = document.querySelectorAll('.gallery-img, .gallery-slide img');

    function parallaxScroll() {
        var sy = window.scrollY;
        if (heroImg) {
            var rect = heroImg.getBoundingClientRect();
            /* 사진이 화면에 있을 때만 */
            if (rect.bottom > 0 && rect.top < window.innerHeight) {
                var offset = (rect.top) * 0.10;
                heroImg.style.transform = 'translateY(' + offset + 'px) scale(1.15)';
            }
        }
    }
    window.addEventListener('scroll', parallaxScroll, { passive: true });
    parallaxScroll();

    /* 2) 단어별 순차 등장 — .reveal-words 요소 */
    document.querySelectorAll('.reveal-words').forEach(function(el) {
        if (el.dataset.wordsReady) return;
        el.dataset.wordsReady = '1';
        var text = el.textContent.trim();
        var words = text.split(/\s+/);
        el.innerHTML = words.map(function(w, i) {
            return '<span class="word" style="transition-delay:' + (i * 0.08) + 's">' + w + '</span>';
        }).join(' ');
    });

    var wordObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('on');
                wordObs.unobserve(entry.target);
            }
        });
    }, { threshold: 0.3 });
    document.querySelectorAll('.reveal-words').forEach(function(el) { wordObs.observe(el); });

    /* 3) reveal-blur 요소 */
    var blurObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('on');
                blurObs.unobserve(entry.target);
            }
        });
    }, { threshold: 0.15 });
    document.querySelectorAll('.reveal-blur').forEach(function(el) {
        var rect = el.getBoundingClientRect();
        if (rect.top < window.innerHeight && rect.bottom > 0) el.classList.add('on');
        else blurObs.observe(el);
    });

    /* 4) 스크롤 진행률 표시 (상단 가는 바) */
    var progressBar = document.getElementById('scrollProgress');
    if (!progressBar) {
        progressBar = document.createElement('div');
        progressBar.id = 'scrollProgress';
        progressBar.style.cssText = 'position:fixed;top:0;left:0;height:2px;background:linear-gradient(90deg,#c4748a,#e8b090);z-index:9999;width:0%;transition:width 0.1s ease;pointer-events:none;';
        document.body.appendChild(progressBar);
    }
    window.addEventListener('scroll', function() {
        var h = document.documentElement.scrollHeight - window.innerHeight;
        var scrolled = h > 0 ? (window.scrollY / h) * 100 : 0;
        progressBar.style.width = scrolled + '%';
    }, { passive: true });

    /* 5) reveal-zoom / reveal-fade 요소 관찰 */
    var extraObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('on');
                extraObs.unobserve(entry.target);
            }
        });
    }, { threshold: 0.12 });
    document.querySelectorAll('.reveal-zoom, .reveal-fade').forEach(function(el) {
        var rect = el.getBoundingClientRect();
        if (rect.top < window.innerHeight && rect.bottom > 0) el.classList.add('on');
        else extraObs.observe(el);
    });

    /* 6) 갤러리 사진 순차 등장 */
    var galleryItems = document.querySelectorAll('.gallery-img, .gallery-grid img, .gallery-slide');
    var galObs = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                var imgs = entry.target.querySelectorAll('img');
                if (imgs.length) {
                    imgs.forEach(function(img, i) {
                        img.style.opacity = '0';
                        img.style.transform = 'scale(1.05)';
                        img.style.transition = 'opacity 0.8s ease ' + (i*0.1) + 's, transform 0.8s cubic-bezier(0.16,1,0.3,1) ' + (i*0.1) + 's';
                        setTimeout(function() {
                            img.style.opacity = '1';
                            img.style.transform = 'scale(1)';
                        }, 50);
                    });
                }
                galObs.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });
    var galGrid = document.querySelector('.gallery-grid, #galleryOuter');
    if (galGrid) galObs.observe(galGrid);

    /* 7) 히어로 스크롤 힌트 화살표 (사진 디자인 테마에서만) */
    var hero = document.getElementById('heroSection');
    if (hero && /design-(our_story|married|forever)/.test(hero.className)) {
        if (!document.querySelector('.scroll-hint')) {
            var hint = document.createElement('div');
            hint.className = 'scroll-hint';
            hint.innerHTML = '⌄';
            hero.appendChild(hint);
            /* 스크롤 시작하면 숨김 */
            window.addEventListener('scroll', function hideHint() {
                if (window.scrollY > 50) {
                    hint.style.opacity = '0';
                    hint.style.transition = 'opacity 0.4s';
                    window.removeEventListener('scroll', hideHint);
                }
            }, { passive: true });
        }
    }

    /* 8) D-Day 실시간 카운트다운 */
    initDdayCountdown();
}

/* D-Day 실시간 카운트다운 */
function initDdayCountdown() {
    var ddayCard = document.getElementById('ddayCard');
    if (!ddayCard) return;
    var dateAttr = ddayCard.getAttribute('data-date');
    if (!dateAttr) return;
    var target = new Date(dateAttr + 'T00:00:00');
    if (isNaN(target)) return;

    function update() {
        var now = new Date();
        var diff = target - now;
        if (diff < 0) diff = 0;
        var days = Math.floor(diff / 86400000);
        var hours = Math.floor((diff % 86400000) / 3600000);
        var mins = Math.floor((diff % 3600000) / 60000);
        var secs = Math.floor((diff % 60000) / 1000);

        /* 시계형 숫자 업데이트 */
        var nums = ddayCard.querySelectorAll('.dp-num, .dp-in');
        if (nums.length >= 4) {
            var vals = [days, hours, mins, secs];
            nums.forEach(function(el, i) {
                if (i < 4 && el.textContent != vals[i]) {
                    el.textContent = vals[i];
                }
            });
        }
    }
    update();
    setInterval(update, 1000);
}


/* ── 토스트 ── */
function showToast(msg) {
    var t = document.getElementById('toast');
    if (!t) return;
    t.textContent = msg || '복사되었습니다 ✓';
    t.classList.add('show');
    clearTimeout(t._tid);
    t._tid = setTimeout(function() { t.classList.remove('show'); }, 2200);
}

/* ── 클립보드 복사 ── */
function copyText(txt) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(txt).then(function() { showToast('복사되었습니다 ✓'); });
    } else {
        var ta = document.createElement('textarea');
        ta.value = txt;
        ta.style.cssText = 'position:fixed;opacity:0;';
        document.body.appendChild(ta);
        ta.focus(); ta.select();
        try { document.execCommand('copy'); } catch(e) {}
        document.body.removeChild(ta);
        showToast('복사되었습니다 ✓');
    }
}

/* ── 이벤트 바인딩 (data-* 방식) ── */
function bindEvents() {
    // 전화 (구 방식 — 하위 호환)
    document.querySelectorAll('[data-phone]').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var phone = btn.dataset.value;
            if (phone) window.location.href = 'tel:' + phone.replace(/[^0-9]/g, '');
        });
    });

    // 연락하기 팝업 (새 방식)
    document.querySelectorAll('[data-contact]').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var name  = btn.dataset.name  || '';
            var phone = btn.dataset.phone || '';
            openContactModal(name, phone);
        });
    });
    // 복사 버튼 (계좌)
    document.querySelectorAll('[data-copy]').forEach(function(btn) {
        btn.addEventListener('click', function() {
            copyText(btn.dataset.copy);
            btn.classList.add('copied');
            setTimeout(function() { btn.classList.remove('copied'); }, 1500);
        });
    });
    // 네이버 지도
    var btnNaver = document.getElementById('btnNaver');
    if (btnNaver) {
        btnNaver.addEventListener('click', function() {
            window.open('https://map.naver.com/v5/search/' + encodeURIComponent(btnNaver.dataset.addr || ''), '_blank');
        });
    }
    // 카카오 지도
    var btnKakao = document.getElementById('btnKakao');
    if (btnKakao) {
        btnKakao.addEventListener('click', function() {
            window.open('https://map.kakao.com/?q=' + encodeURIComponent(btnKakao.dataset.addr || ''), '_blank');
        });
    }
    // 주소 복사
    var btnCopyAddr = document.getElementById('btnCopyAddr');
    if (btnCopyAddr) {
        btnCopyAddr.addEventListener('click', function() { copyText(btnCopyAddr.dataset.addr || ''); });
    }
}

/* ── 메인 진입점 ── */
window.initPage = function(data) {
    var dateStr = data.weddingDate;
    var timeStr = data.weddingTime;

    // 상단 큰 날짜 (M.DD 형식)
    var heroDateShort = document.getElementById('heroDateShort');
    if (heroDateShort && dateStr) {
        var d = new Date(dateStr);
        var m = d.getMonth() + 1;
        var day = String(d.getDate()).padStart(2, '0');
        heroDateShort.textContent = m + '.' + day;
    }

    // 하단 풀 날짜 (2026. 10. 24. Saturday 12:30 PM)
    var heroDate = document.getElementById('heroDate');
    if (heroDate && dateStr) {
        var d2 = new Date(dateStr);
        var daysEn = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
        var dow = daysEn[d2.getDay()];
        var y2 = d2.getFullYear();
        var m2 = String(d2.getMonth()+1).padStart(2,'0');
        var day2 = String(d2.getDate()).padStart(2,'0');
        var timePart = '';
        if (timeStr) {
            var parts = timeStr.split(':').map(Number);
            var h = parts[0], mi = parts[1];
            var ampm = h >= 12 ? 'PM' : 'AM';
            var hh = h > 12 ? h - 12 : h === 0 ? 12 : h;
            timePart = ' ' + hh + ':' + String(mi).padStart(2,'0') + ' ' + ampm;
        }
        heroDate.textContent = y2 + '. ' + m2 + '. ' + day2 + '. ' + dow + timePart;
    }

    // 정보 섹션 날짜/시간
    var infoDate = document.getElementById('infoDate');
    var infoTime = document.getElementById('infoTime');
    if (infoDate) infoDate.textContent = formatDate(data.weddingDate);
    if (infoTime) infoTime.textContent = formatTime(data.weddingTime);

    // 달력
    if (data.calendarVisible) {
        buildCalendar(data.weddingDate);
        if (data.ddayVisible) calcDday(data.weddingDate);
    }

    initGallery();
    initPetals();
    initScrollEffects();
    bindEvents();

    /* 안전장치: 화면에 이미 보이는 reveal 요소만 표시 (화면 밖은 스크롤 시 등장) */
    setTimeout(function() {
        function isInView(el) {
            var r = el.getBoundingClientRect();
            return r.top < window.innerHeight && r.bottom > 0;
        }
        document.querySelectorAll('.reveal, .reveal-left, .reveal-right, .reveal-scale, .reveal-blur, .reveal-zoom, .section-divider').forEach(function(el) {
            if (isInView(el)) el.classList.add('on');
        });
        document.querySelectorAll('.info-item').forEach(function(el, i) {
            if (isInView(el)) setTimeout(function() { el.classList.add('on'); }, i * 80);
        });
    }, 300);
};

/* ── 카카오 지도 초기화 ── */
function initKakaoMap(lat, lng, zoomLevel) {
    var mapEl = document.getElementById('invitationMap');
    if (!mapEl || typeof kakao === 'undefined') return;
    var zoomMap = {'20M':3,'30M':4,'50M':5,'100M':6,'250M':7,'500M':8};
    var level = zoomMap[zoomLevel] || 5;
    var center = new kakao.maps.LatLng(lat || 37.5009, lng || 127.0363);
    var map = new kakao.maps.Map(mapEl, { center: center, level: level });
    var marker = new kakao.maps.Marker({ position: center, map: map });
}

/* ── 내비게이션 앱 연결 ── */
function bindNaviButtons() {
    var naviKakao = document.getElementById('naviKakao');
    var naviTmap  = document.getElementById('naviTmap');
    var naviNaver = document.getElementById('naviNaver');
    if (naviKakao) {
        naviKakao.addEventListener('click', function() {
            var addr = naviKakao.dataset.addr || '';
            window.open('kakaomap://search?q=' + encodeURIComponent(addr), '_blank');
        });
    }
    if (naviTmap) {
        naviTmap.addEventListener('click', function() {
            var addr = naviTmap.dataset.addr || '';
            window.open('tmap://search?name=' + encodeURIComponent(addr), '_blank');
        });
    }
    if (naviNaver) {
        naviNaver.addEventListener('click', function() {
            var addr = naviNaver.dataset.addr || '';
            window.open('nmap://search?query=' + encodeURIComponent(addr) + '&appname=com.wedding', '_blank');
        });
    }
}

/* ── 슬라이드 카운터 업데이트 ── */
function updateSlideCounter(current, total) {
    var counter = document.getElementById('slideCounter');
    if (counter) counter.textContent = (current + 1) + ' / ' + total;
}

/* ════════════════════════════════════════
   공유하기 (Share)
   ════════════════════════════════════════ */
function initShare(groomName, brideName) {
    var pageUrl   = window.location.href;
    var shareTitle = groomName + ' ♥ ' + brideName + ' 결혼합니다';
    var shareText  = '저희 결혼식에 초대합니다. 청첩장을 확인해 주세요 💌';

    // 카카오톡
    document.getElementById('shareKakao').addEventListener('click', function() {
        if (typeof Kakao !== 'undefined' && Kakao.isInitialized()) {
            Kakao.Share.sendDefault({
                objectType: 'feed',
                content: {
                    title: shareTitle,
                    description: shareText,
                    imageUrl: '',
                    link: { mobileWebUrl: pageUrl, webUrl: pageUrl }
                }
            });
        } else {
            // 카카오 SDK 없을 때 URL 복사로 대체
            copyTextSilent(pageUrl);
            showToast('링크가 복사되었습니다. 카카오톡에 붙여넣기 하세요 ✓');
        }
    });

    // URL 복사
    document.getElementById('shareUrl').addEventListener('click', function() {
        copyTextSilent(pageUrl);
        showToast('링크가 복사되었습니다 ✓');
    });

    // 문자
    document.getElementById('shareSms').addEventListener('click', function() {
        var body = encodeURIComponent(shareTitle + '\n' + shareText + '\n' + pageUrl);
        window.location.href = 'sms:?body=' + body;
    });

    // QR 코드
    document.getElementById('shareQr').addEventListener('click', function() {
        var qrWrap = document.getElementById('qrWrap');
        if (qrWrap.style.display === 'none') {
            qrWrap.style.display = 'block';
            var canvas = document.getElementById('qrCanvas');
            if (typeof QRCode !== 'undefined') {
                QRCode.toCanvas(canvas, pageUrl, { width: 180, margin: 2, color: { dark: '#3a2e22', light: '#fdf9f5' } });
            } else {
                qrWrap.style.display = 'none';
                showToast('QR 라이브러리 로딩 중입니다.');
            }
        } else {
            qrWrap.style.display = 'none';
        }
    });

    document.getElementById('qrCloseBtn').addEventListener('click', function() {
        document.getElementById('qrWrap').style.display = 'none';
    });

    // Web Share API (모바일 네이티브 공유)
    if (navigator.share) {
        // 네이티브 공유가 있으면 URL 복사 버튼을 네이티브 공유로 교체
        document.getElementById('shareUrl').addEventListener('click', function(e) {
            e.stopImmediatePropagation();
            navigator.share({ title: shareTitle, text: shareText, url: pageUrl })
                .catch(function() {});
        }, true);
    }
}

function copyTextSilent(txt) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(txt);
    } else {
        var ta = document.createElement('textarea');
        ta.value = txt;
        ta.style.cssText = 'position:fixed;opacity:0;';
        document.body.appendChild(ta);
        ta.focus(); ta.select();
        try { document.execCommand('copy'); } catch(e) {}
        document.body.removeChild(ta);
    }
}

/* initPage에 방명록·공유 연결 */


/* ════════════════════════════════════════
   계좌 탭 전환 (청첩장)
   ════════════════════════════════════════ */
function initAccountTabs() {
    var tabGroom = document.getElementById('acctTabGroom');
    var tabBride = document.getElementById('acctTabBride');
    if (!tabGroom || !tabBride) return;

    tabGroom.addEventListener('click', function() {
        tabGroom.classList.add('active');
        tabBride.classList.remove('active');
        document.getElementById('acctPanelGroom').style.display = 'block';
        document.getElementById('acctPanelBride').style.display = 'none';
    });
    tabBride.addEventListener('click', function() {
        tabBride.classList.add('active');
        tabGroom.classList.remove('active');
        document.getElementById('acctPanelBride').style.display = 'block';
        document.getElementById('acctPanelGroom').style.display = 'none';
    });
}

/* initPage에 계좌 탭 연결 */
var _origInitPage2 = window.initPage;
window.initPage = function(data) {
    _origInitPage2(data);
    initAccountTabs();
};

/* ════════════════════════════════════════
   캘린더 스타일 렌더링
   ════════════════════════════════════════ */
function initCalendarStyle(data) {
    var card = document.getElementById('calCard');
    if (!card) return;

    var style = data.calendarStyle || 'basic';
    var dateStr = data.weddingDate;
    var timeStr = data.weddingTime;

    if (!dateStr) return;
    var d = new Date(dateStr);
    var y = d.getFullYear();
    var m = d.getMonth();
    var day = d.getDate();
    var dows   = ['일','월','화','수','목','금','토'];
    var dowsEn = ['SUN','MON','TUE','WED','THU','FRI','SAT'];
    var monthsEn = ['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE',
                    'JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'];

    /* 공통 달력 그리드 빌드 */
    function buildGrid(isEn) {
        var labels = isEn ? dowsEn : dows;
        var first  = new Date(y, m, 1).getDay();
        var last   = new Date(y, m+1, 0).getDate();
        var html   = labels.map(function(w, i){
            var cl = i===0?'cal-dow-sun':i===6?'cal-dow-sat':'';
            return '<div class="cal-dow '+cl+'">' + w + '</div>';
        }).join('');
        for (var i=0; i<first; i++) html += '<div class="cal-cell empty"></div>';
        for (var n=1; n<=last; n++) {
            var dow = (first+n-1) % 7;
            var cls = 'cal-cell';
            if (n===day) cls += ' wed';
            else if (dow===0) cls += ' sun';
            else if (dow===6) cls += ' sat';
            html += '<div class="'+cls+'">'+n+'</div>';
        }
        return html;
    }

    var calTitle = document.getElementById('calTitle');
    var calGrid  = document.getElementById('calGrid');

    if (style === 'basic') {
        if (calTitle) calTitle.textContent = y + '년 ' + (m+1) + '월';
        if (calGrid)  calGrid.innerHTML = buildGrid(false);

    } else if (style === 'date') {
        var dateHeader = document.getElementById('calDateHeader');
        var dateMain   = document.getElementById('calDateMain');
        var dateSub    = document.getElementById('calDateSub');
        if (dateHeader) dateHeader.style.display = 'block';
        if (calTitle)   calTitle.style.display   = 'none';
        var mm = String(m+1).padStart(2,'0');
        var dd = String(day).padStart(2,'0');
        if (dateMain) dateMain.textContent = y + '.' + mm + '.' + dd;
        if (dateSub)  dateSub.textContent  = dows[d.getDay()] + '요일 ' + formatTime(timeStr);
        if (calGrid)  calGrid.innerHTML = buildGrid(false);

    } else if (style === 'en') {
        var enHeader = document.getElementById('calEnHeader');
        var enMonth  = document.getElementById('calEnMonth');
        if (enHeader) enHeader.style.display = 'block';
        if (calTitle) calTitle.style.display  = 'none';
        if (enMonth)  enMonth.textContent = monthsEn[m];
        if (calGrid) {
            calGrid.innerHTML = buildGrid(true);
            calGrid.classList.add('cal-grid-en');
        }

    } else if (style === 'text') {
        var textHeader = document.getElementById('calTextHeader');
        var textMsg    = document.getElementById('calTextMsg');
        if (textHeader) textHeader.style.display = 'block';
        if (calTitle)   calTitle.style.display   = 'none';
        if (textMsg) {
            var korMonths = ['일월','이월','삼월','사월','오월','유월',
                             '칠월','팔월','구월','시월','십일월','십이월'];
            var korDays   = ['','첫','두','세','네','다섯','여섯','일곱','여덟','아홉',
                             '열','열한','열두','열세','열네','열다섯','열여섯','열일곱','열여덟','열아홉',
                             '스무','스물한','스물두','스물세','스물네','스물다섯','스물여섯','스물일곱','스물여덟','스물아홉',
                             '서른','서른한'];
            var mon    = korMonths[m] || (m + 1) + '월';
            var dayOrd = korDays[day] ? korDays[day] + '번째' : day + '번째';
            textMsg.innerHTML = mon + '의<br>' + dayOrd + ' 날.';
        }
        if (calGrid) calGrid.innerHTML = buildGrid(false);
    }
}

/* ════════════════════════════════════════
   D-Day 카운트다운 스타일별 렌더링
   ════════════════════════════════════════ */
var _ddayTimer = null;

function initDdayStyle(data) {
    var card = document.getElementById('ddayCard');
    if (!card) return;

    /* 이전 스타일의 카운트다운 타이머가 남아있으면 정리 (재호출마다 누적되는 것 방지) */
    if (_ddayTimer) {
        clearInterval(_ddayTimer);
        _ddayTimer = null;
    }

    var style   = data.ddayStyle || 'text';
    var dateStr = data.weddingDate;
    var groomRaw = data.groomName || '신랑';
    var brideRaw = data.brideName || '신부';
    /* 표시 순서 반영 */
    var heroNamesEl = document.querySelector('.hero-top-names');
    var ord = heroNamesEl ? (heroNamesEl.getAttribute('data-order') || data.displayOrder || 'groom') : (data.displayOrder || 'groom');
    var groom = (ord === 'bride') ? brideRaw : groomRaw;
    var bride = (ord === 'bride') ? groomRaw : brideRaw;
    if (!dateStr) return;

    /* 텍스트형 */
    if (style === 'text') {
        var el = document.getElementById('ddayStyleText');
        var msgEl = document.getElementById('ddayTextMsg');
        if (el) el.style.display = 'block';
        if (msgEl) {
            var today = new Date(); today.setHours(0,0,0,0);
            var wed   = new Date(dateStr); wed.setHours(0,0,0,0);
            var diff  = Math.round((wed-today)/86400000);
            msgEl.innerHTML = groom + ' ♥ ' + bride + ' 예식일까지, <strong>' + diff + '일</strong>';
        }
        return;
    }

    /* 시계형 공통 함수 */
    function getCountdown() {
        var now = new Date();
        var wed = new Date(dateStr);
        var diff = wed - now;
        if (diff < 0) diff = 0;
        var days  = Math.floor(diff / 86400000);
        var hours = Math.floor((diff % 86400000) / 3600000);
        var mins  = Math.floor((diff % 3600000) / 60000);
        var secs  = Math.floor((diff % 60000) / 1000);
        return {
            days:  String(days).padStart(2,'0'),
            hours: String(hours).padStart(2,'0'),
            mins:  String(mins).padStart(2,'0'),
            secs:  String(secs).padStart(2,'0'),
            daysNum: days
        };
    }

    function pad(v) { return String(v).padStart(2,'0'); }

    if (style === 'clock') {
        var el = document.getElementById('ddayStyleClock');
        if (el) el.style.display = 'block';
        function tick() {
            var c = getCountdown();
            var d = document.getElementById('clockDays');
            var h = document.getElementById('clockHours');
            var mi = document.getElementById('clockMins');
            var s = document.getElementById('clockSecs');
            if (d) d.textContent = c.days;
            if (h) h.textContent = c.hours;
            if (mi) mi.textContent = c.mins;
            if (s) s.textContent = c.secs;
        }
        tick();
        _ddayTimer = setInterval(tick, 1000);

    } else if (style === 'clock_bottom') {
        var el = document.getElementById('ddayStyleClockBottom');
        var sub = document.getElementById('ddaySubText2');
        if (el) el.style.display = 'block';
        function tick2() {
            var c = getCountdown();
            var d = document.getElementById('clockDays2');
            var h = document.getElementById('clockHours2');
            var mi = document.getElementById('clockMins2');
            var s = document.getElementById('clockSecs2');
            if (d) d.textContent = c.days;
            if (h) h.textContent = c.hours;
            if (mi) mi.textContent = c.mins;
            if (s) s.textContent = c.secs;
            if (sub) sub.innerHTML = groom + ' ♥ ' + bride + ' 예식일까지, <strong>' + c.daysNum + '일</strong>';
        }
        tick2();
        _ddayTimer = setInterval(tick2, 1000);

    } else if (style === 'clock_inline') {
        var el = document.getElementById('ddayStyleInline');
        var sub2 = document.getElementById('ddaySubTextInline');
        if (el) el.style.display = 'block';
        function tick3() {
            var c = getCountdown();
            var d = document.getElementById('inDays');
            var h = document.getElementById('inHours');
            var mi = document.getElementById('inMins');
            var s = document.getElementById('inSecs');
            if (d) d.textContent = c.days;
            if (h) h.textContent = c.hours;
            if (mi) mi.textContent = c.mins;
            if (s) s.textContent = c.secs;
            if (sub2) sub2.innerHTML = '예식일까지, <strong>' + c.daysNum + '일</strong>';
        }
        tick3();
        _ddayTimer = setInterval(tick3, 1000);
    }
}

/* initPage에 연결 */
var _origInitPage3 = window.initPage;
window.initPage = function(data) {
    _origInitPage3(data);
    initCalendarStyle(data);
    initDdayStyle(data);
};
/* ══ 연락하기 팝업 모달 ══ */
function openContactModal(name, phone) {
    if (!phone || phone.trim() === '') return;   /* 번호 없으면 팝업 안 띄움 */

    var modal    = document.getElementById('contactModal');
    var nameEl   = document.getElementById('contactModalName');
    var phoneEl  = document.getElementById('contactModalPhone');
    var callBtn  = document.getElementById('contactModalCallBtn');
    var smsBtn   = document.getElementById('contactModalSmsBtn');
    if (!modal) return;

    var clean = phone.replace(/[^0-9]/g, '');
    var display = phone.replace(/[^0-9]/g, '')
        .replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');

    nameEl.textContent  = name  || '연락처';
    phoneEl.textContent = display || phone;
    callBtn.href = 'tel:' + clean;
    smsBtn.href  = 'sms:' + clean;

    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';

    /* 애니메이션 */
    var box = modal.querySelector('.contact-modal-box');
    if (box) {
        box.style.transform = 'translateY(60px)';
        box.style.opacity   = '0';
        requestAnimationFrame(function() {
            box.style.transition = 'transform 0.28s cubic-bezier(0.34,1.56,0.64,1), opacity 0.22s ease';
            box.style.transform  = 'translateY(0)';
            box.style.opacity    = '1';
        });
    }
}

function closeContactModal(e) {
    /* 오버레이 클릭 또는 닫기 버튼 */
    if (e && e.target && !e.target.classList.contains('contact-modal-overlay')) return;
    var modal = document.getElementById('contactModal');
    if (!modal) return;
    var box = modal.querySelector('.contact-modal-box');
    if (box) {
        box.style.transition = 'transform 0.2s ease, opacity 0.18s ease';
        box.style.transform  = 'translateY(40px)';
        box.style.opacity    = '0';
        setTimeout(function() {
            modal.style.display = 'none';
            document.body.style.overflow = '';
        }, 200);
    } else {
        modal.style.display = 'none';
        document.body.style.overflow = '';
    }
}
/* ══ 갤러리 이미지 팝업 뷰어 ══ */
var _gpImgs = [], _gpIdx = 0, _gpCounterTimer = null;

function openGalleryPopup(imgs, idx) {
    _gpImgs = imgs;
    _gpIdx  = idx || 0;
    var overlay = document.getElementById('galleryPopup');
    if (!overlay) return;
    overlay.style.display = 'flex';
    document.body.style.overflow = 'hidden';

    /* 이미지만 업데이트 (카운터는 열릴 때 1회만) */
    _updateGpImg();
    _showCounter();

    /* 좌우 버튼 — 이미지만 교체, 카운터 미표시 */
    var btnP = document.getElementById('gpPrev');
    var btnN = document.getElementById('gpNext');
    if (btnP) btnP.onclick = function(e) { e.stopPropagation(); _gpIdx = Math.max(0, _gpIdx-1); _updateGpImg(); };
    if (btnN) btnN.onclick = function(e) { e.stopPropagation(); _gpIdx = Math.min(_gpImgs.length-1, _gpIdx+1); _updateGpImg(); };

    /* 배경(overlay) 클릭 → 닫기 */
    overlay.onclick = function(e) {
        if (e.target === overlay) closeGalleryPopup();
    };

    /* X 버튼 — 버튼 안 아이콘 클릭도 처리 */
    var closeBtn = document.getElementById('gpCloseBtn');
    if (closeBtn) closeBtn.onclick = function(e) { e.stopPropagation(); closeGalleryPopup(); };

    /* 키보드 */
    document.addEventListener('keydown', _gpKeyHandler);

    /* 스와이프 */
    var imgWrap = overlay.querySelector('.gp-img-wrap');
    var sx = 0;
    if (imgWrap) {
        imgWrap.ontouchstart = function(e) { sx = e.touches[0].clientX; };
        imgWrap.ontouchend   = function(e) {
            var dx = e.changedTouches[0].clientX - sx;
            if (Math.abs(dx) > 40) {
                _gpIdx = dx < 0 ? Math.min(_gpImgs.length-1, _gpIdx+1) : Math.max(0, _gpIdx-1);
                _updateGpImg();
            }
        };
    }
}

function _updateGpImg() {
    var img  = document.getElementById('gpImg');
    var btnP = document.getElementById('gpPrev');
    var btnN = document.getElementById('gpNext');
    if (img) img.src = _gpImgs[_gpIdx].trim();
    if (btnP) btnP.style.opacity = _gpIdx === 0 ? '0.3' : '1';
    if (btnN) btnN.style.opacity = _gpIdx === _gpImgs.length-1 ? '0.3' : '1';
}

function _showCounter() {
    var counter = document.getElementById('gpCounter');
    if (!counter) return;
    /* 카운터 텍스트 설정 + 표시 */
    counter.textContent = (_gpIdx + 1) + ' / ' + _gpImgs.length;
    counter.classList.remove('fade-out');
    counter.style.opacity = '1';
    /* 기존 타이머 취소 */
    if (_gpCounterTimer) clearTimeout(_gpCounterTimer);
    /* 1.8초 후 페이드아웃 */
    _gpCounterTimer = setTimeout(function() {
        counter.classList.add('fade-out');
    }, 1800);
}

function _gpKeyHandler(e) {
    if (e.key === 'ArrowLeft')  { _gpIdx = Math.max(0, _gpIdx-1); _updateGpImg(); }
    if (e.key === 'ArrowRight') { _gpIdx = Math.min(_gpImgs.length-1, _gpIdx+1); _updateGpImg(); }
    if (e.key === 'Escape')     { closeGalleryPopup(); }
}

function closeGalleryPopup(e) {
    var overlay = document.getElementById('galleryPopup');
    if (overlay) overlay.style.display = 'none';
    document.body.style.overflow = '';
    document.removeEventListener('keydown', _gpKeyHandler);
    if (_gpCounterTimer) { clearTimeout(_gpCounterTimer); _gpCounterTimer = null; }
}
