/* ════════════════════════════════════════
   Wedding Editor JS
   ════════════════════════════════════════ */
'use strict';

var liveFrame   = document.getElementById('liveFrame');
var INVITATION_URL = liveFrame ? liveFrame.getAttribute('src') : '/';
var previewReady = false;
var liveTimer    = null;
var saveTimer    = null;
var scrollSync   = true;
var acctData     = { groom:[], bride:[] };
var BANKS = ['은행 선택','국민은행','신한은행','우리은행','하나은행','농협은행',
             '카카오뱅크','토스뱅크','케이뱅크','기업은행','SC제일은행',
             '씨티은행','대구은행','부산은행','광주은행','전북은행','경남은행','제주은행'];

/* ──────────────────────────────────────
   임시저장/게시 상태 표시
   — "게시하기"를 눌러야만 게스트 화면에 반영됨
────────────────────────────────────── */
var lastDraftSavedAt = (WEDDING.hasDraft && WEDDING.draftSavedAt) ? new Date(WEDDING.draftSavedAt) : null;

function relativeTimeFrom(date) {
    var diffSec = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));
    if (diffSec < 60) return '방금 전';
    var diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return diffMin + '분 전';
    var diffHour = Math.floor(diffMin / 60);
    return diffHour + '시간 전';
}

function renderDraftStatus() {
    var label = document.getElementById('draftStatusLabel');
    if (!label) return;
    label.textContent = lastDraftSavedAt
        ? '임시저장됨 · ' + relativeTimeFrom(lastDraftSavedAt)
        : '게시됨';
}

function markDraftSaved() {
    lastDraftSavedAt = new Date();
    renderDraftStatus();
}

function markPublished() {
    lastDraftSavedAt = null;
    renderDraftStatus();
}

renderDraftStatus();
setInterval(renderDraftStatus, 30000);

/* ──────────────────────────────────────
   섹션별 되돌리기 (마지막 저장 상태로 복원)
   — 히든 입력의 초기 HTML 기본값이 아니라, 페이지 로드가 끝난 시점의
     실제 값(서버에서 복원된 값)을 스냅샷으로 저장해 두고 사용한다.
────────────────────────────────────── */

/* data-* 속성으로 활성 카드를 표시하는 탭/카드형 픽커들 — 히든 입력값 기준으로 active 클래스만 재동기화 */
var PICKER_GROUPS = [
    { hiddenId: 'mainDesignVal',    itemSelector: '.ed-design-card',        dataAttr: 'design' },
    { hiddenId: 'calStyleInput',    itemSelector: '.cal-style-card',        dataAttr: 'cal' },
    { hiddenId: 'ddayStyleInput',   itemSelector: '.style-type-item',       dataAttr: 'dday' },
    { hiddenId: 'orderVal',         itemSelector: '#orderTabs .ed-tab',     dataAttr: 'val' },
    { hiddenId: 'alignInput',       itemSelector: '#alignTabs .ed-tab',     dataAttr: 'val' },
    { hiddenId: 'deceasedInput',    itemSelector: '#deceasedTabs .ed-tab',  dataAttr: 'val' },
    { hiddenId: 'contactInput',     itemSelector: '#contactTabs .ed-tab',   dataAttr: 'val' },
    { hiddenId: 'mapLockVal',       itemSelector: '#mapLockTabs .ed-tab',   dataAttr: 'val' },
    { hiddenId: 'mapDetailVal',     itemSelector: '#mapDetailTabs .ed-tab', dataAttr: 'val' },
    { hiddenId: 'mapZoomInput',     itemSelector: '.ed-zoom-btn',           dataAttr: 'val' },
    { hiddenId: 'galTypeVal',       itemSelector: '.gal-type-card',         dataAttr: 'galtype' },
    { hiddenId: 'galScrollVal',     itemSelector: '#galScrollTabs .ed-tab', dataAttr: 'val' }
];

function resyncPickerVisuals() {
    PICKER_GROUPS.forEach(function(g) {
        var hidden = document.getElementById(g.hiddenId);
        if (!hidden) return;
        var val = String(hidden.value);
        document.querySelectorAll(g.itemSelector).forEach(function(el) {
            el.classList.toggle('active', String(el.dataset[g.dataAttr]) === val);
        });
    });
}

/* 히든 입력값만으로는 갱신되지 않는, 섹션별 추가 시각 동기화 */
var SECTION_REVERT_HOOKS = {
    main: function() {
        var thumbImg  = document.getElementById('mainThumbImg');
        var posXInput = document.getElementById('mainPhotoPosXInput');
        var posYInput = document.getElementById('mainPhotoPosYInput');
        if (thumbImg && posXInput && posYInput) {
            thumbImg.style.objectPosition = (posXInput.value !== '' && posYInput.value !== '')
                ? (posXInput.value + '% ' + posYInput.value + '%') : '';
        }
        var b64 = document.getElementById('mainPhotoBase64');
        var thumbWrap = document.getElementById('mainPhotoThumb');
        var hint = document.getElementById('mainUploadHint');
        if (b64 && thumbImg) {
            if (b64.value) {
                thumbImg.src = 'data:image/jpeg;base64,' + b64.value;
                if (thumbWrap) thumbWrap.style.display = 'block';
                if (hint) hint.style.display = 'none';
            } else {
                if (thumbWrap) thumbWrap.style.display = 'none';
                if (hint) hint.style.display = '';
            }
        }
    },
    map: function() {
        var lati = document.getElementById('mapLatInput');
        var lngi = document.getElementById('mapLngInput');
        var ni   = document.getElementById('mapPlaceNameInput');
        var lat = lati ? parseFloat(lati.value) : NaN;
        var lng = lngi ? parseFloat(lngi.value) : NaN;
        if (lat && lng && typeof showStaticMap === 'function') showStaticMap(lng, lat, ni ? ni.value : '');
    }
};

/* 페이지 로드(서버 값 복원)가 모두 끝난 뒤 폼 필드 값을 스냅샷으로 저장 */
function captureSectionSnapshot() {
    var form = document.getElementById('editForm');
    if (!form) return;
    form.querySelectorAll('input, textarea, select').forEach(function(el) {
        if (el.type === 'checkbox' || el.type === 'radio') {
            el._savedChecked = el.checked;
        } else {
            el._savedValue = el.value;
        }
    });
}

/** 섹션 헤더의 되돌리기 버튼에서 호출 — 그 섹션만 마지막 저장 상태로 복원 */
function revertSection(evt, sectionKey) {
    evt.stopPropagation();
    var section = document.getElementById('sec-' + sectionKey);
    if (!section) return;
    if (!confirm('이 섹션을 마지막 저장 상태로 되돌릴까요? 저장하지 않은 변경사항은 사라집니다.')) return;

    section.querySelectorAll('input, textarea, select').forEach(function(el) {
        var hasChecked = el._savedChecked !== undefined;
        var hasValue   = el._savedValue !== undefined;
        if (!hasChecked && !hasValue) return; /* 스냅샷 이후 새로 생긴 요소(계좌 행 등)는 건드리지 않음 */
        if (el.type === 'checkbox' || el.type === 'radio') {
            el.checked = el._savedChecked;
        } else {
            el.value = el._savedValue;
        }
        el.dispatchEvent(new Event('input',  { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
    });

    resyncPickerVisuals();
    if (SECTION_REVERT_HOOKS[sectionKey]) SECTION_REVERT_HOOKS[sectionKey]();

    scheduleLive(100);
    showEditorToast('✓ 되돌렸습니다');
}

/* ──────────────────────────────────────
   첫 방문 온보딩 가이드
   — 섹션이 한꺼번에 펼쳐져 있어 어디서부터 시작할지 안내가 필요한 문제 대응
────────────────────────────────────── */
(function() {
    var overlay   = document.getElementById('onboardingOverlay');
    var startBtn  = document.getElementById('onboardingStartBtn');
    var reopenBtn = document.getElementById('onboardingReopenBtn');
    if (!overlay) return;

    var storageKey = 'wc_onboarded_' + (WEDDING.id || 'unknown');

    function hasSeenOnboarding() {
        try { return !!localStorage.getItem(storageKey); } catch (e) { return true; }
    }
    function markOnboardingSeen() {
        try { localStorage.setItem(storageKey, '1'); } catch (e) {}
    }

    function openOnboarding()  { overlay.classList.add('open'); }
    function closeOnboarding() { overlay.classList.remove('open'); markOnboardingSeen(); }

    if (!hasSeenOnboarding()) openOnboarding();

    if (startBtn) startBtn.addEventListener('click', closeOnboarding);
    overlay.addEventListener('click', function(e) { if (e.target === overlay) closeOnboarding(); });
    document.querySelectorAll('.onboarding-step').forEach(function(step) {
        step.addEventListener('click', function() {
            var sec = step.dataset.sec;
            closeOnboarding();
            if (sec && typeof scrollToSection === 'function') {
                setTimeout(function() { scrollToSection(sec); }, 200);
            }
        });
    });
    if (reopenBtn) reopenBtn.addEventListener('click', openOnboarding);
})();

/* ──────────────────────────────────────
   섹션 접기/펼치기
────────────────────────────────────── */
document.querySelectorAll('.ed-sec-hd').forEach(function(hd) {
    hd.addEventListener('click', function(e) {
        if (e.target.closest('.ed-toggle-wrap') || e.target.closest('.ed-sec-undo-btn')) return;
        var sec = this.closest('.ed-section');
        var bdId = 'bd-' + this.dataset.sec;
        var bd   = document.getElementById(bdId);
        var chev = this.querySelector('.ed-chevron');
        if (!bd) return;
        var open = sec.classList.contains('open');
        if (open) {
            sec.classList.remove('open');
            sec.classList.add('collapsed');
            bd.style.display = 'none';
            if (chev) chev.style.transform = 'rotate(-90deg)';
        } else {
            sec.classList.add('open');
            sec.classList.remove('collapsed');
            bd.style.display = '';
            if (chev) chev.style.transform = 'rotate(0deg)';
            if (hd.dataset.sec === 'rsvp' && typeof loadRsvpList === 'function') loadRsvpList();
        }
    });
});

/* ──────────────────────────────────────
   탭 선택 공통
────────────────────────────────────── */
/* 관계 드롭다운 → 직접입력 연동 */
function syncRelation(side, val) {
    var inputId = side === 'groom' ? 'groomRelationInput' : 'brideRelationInput';
    var input = document.getElementById(inputId);
    if (!input) return;
    if (val) {
        input.value = val;
        input.dispatchEvent(new Event('input', {bubbles:true}));
    }
    scheduleLive(100);
}

function pickTab(el, tabGroupId, hiddenId) {
    var group = document.getElementById(tabGroupId);
    if (group) group.querySelectorAll('.ed-tab').forEach(function(t){ t.classList.remove('active'); });
    el.classList.add('active');
    var hidden = document.getElementById(hiddenId);
    if (hidden) {
        hidden.value = el.dataset.val;
        hidden.dispatchEvent(new Event('change', {bubbles:true}));
    }
    scheduleLive(100);
}

/* 캘린더 스타일 */
function pickCalStyle(el, val) {
    document.querySelectorAll('.cal-style-card').forEach(function(c){ c.classList.remove('active'); });
    el.classList.add('active');
    var ci = document.getElementById('calStyleInput');
    if (ci) ci.value = val;
    scheduleLive(100);
}

/* D-Day 스타일 */
function pickDdayStyle(el, val) {
    document.querySelectorAll('.style-type-item').forEach(function(i){ i.classList.remove('active'); });
    el.classList.add('active');
    var di = document.getElementById('ddayStyleInput');
    if (di) di.value = val;
    scheduleLive(100);
}

/* ──────────────────────────────────────
   메인 화면 — 디자인/효과/글꼴/색상
────────────────────────────────────── */

/* 테마별 디폴트 글자색 — THEME_COLORS 첫 번째 값과 일치 */
var DESIGN_DEFAULT_COLORS = {
    basic:          '#2c2822',
    our_story:      '#000000',
    our_story_pink: '#d9527a',
    married:        '#ffffff',
    forever:        '#2c2822'
};

/** 해당 테마의 기본 글자색을 색상 입력에 적용 */
function applyDesignDefaultColor(design) {
    var colorHex    = document.getElementById('fontColorHex');
    var colorPicker = document.getElementById('fontColorPicker');
    if (!colorHex || !colorPicker) return;
    var defaultColor = DESIGN_DEFAULT_COLORS[design] || '#2c2822';
    colorHex.value    = defaultColor;
    colorPicker.value = defaultColor;
    colorHex.dispatchEvent(new Event('input', { bubbles: true }));
}

/**
 * 테마 선택.
 * opts.preserveColor = true 이면 테마 기본색으로 덮어쓰지 않는다
 * (페이지 로드 시 저장된 사용자 색상을 복원할 때 사용).
 */
function pickDesign(el, val, opts) {
    document.querySelectorAll('.ed-design-card').forEach(function(c){ c.classList.remove('active'); });
    el.classList.add('active');
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
    var input = document.getElementById('mainDesignVal');
    if (input) input.value = val;

    /* 테마를 실제로 "전환"할 때만 기본 글자색 자동 적용 */
    if (!(opts && opts.preserveColor)) {
        var prevDesign = (input && input.dataset.prevDesign) || 'basic';
        if (prevDesign !== val) applyDesignDefaultColor(val);
    }
    if (input) input.dataset.prevDesign = val;

    /* 테마 선택 시 폼에 값이 없으면 샘플 데이터로 미리보기 채우기 */
    var groomInput = document.querySelector('[name="groomName"]');
    var brideInput = document.querySelector('[name="brideName"]');
    var isEmpty = !groomInput || !groomInput.value.trim();
    if (isEmpty) {
        /* 폼 입력값 임시 채우기 */
        var sampleData = {
            groomName: '이준서', brideName: '김은재',
            weddingDate: '2025-10-18', weddingTime: '14:00',
            weddingPlace: 'MARRIAGE WEDDING HALL', mapPlaceName: 'POCKET HALL',
            weddingAddress: '서울특별시 강남구 테헤란로 123'
        };
        Object.keys(sampleData).forEach(function(k) {
            var el = document.querySelector('[name="' + k + '"]');
            if (el && !el.value.trim()) el.value = sampleData[k];
        });
    }

    /* 디자인 변경 즉시 미리보기 갱신 */
    /* 디자인별 추천 색상 프리셋 표시 */
    updateThemeColorPresets(val);
    scheduleLive(150);
    scrollPreviewTo('main');
}

/* 디자인 테마별 추천 글꼴 색상 */
var THEME_COLORS = {
    basic:           ['#2c2822', '#c4748a', '#7a6a54'],
    our_story:       ['#000000', '#2c2822', '#5a4e40'],
    our_story_pink:  ['#d9527a', '#e68a9a', '#c4748a'],
    married:         ['#ffffff', '#f0ece6', '#e8e0d0'],
    forever:         ['#2c2822', '#5a4e40', '#8a7a64']
};

function updateThemeColorPresets(design) {
    var container = document.getElementById('themeColorPresets');
    if (!container) return;
    var colors = (THEME_COLORS[design] || THEME_COLORS.basic).slice();
    /* 항상 검정/흰색을 보장하되, 테마 추천색에 이미 있으면 중복 표시하지 않음 */
    ['#000000', '#ffffff'].forEach(function(fallback) {
        if (colors.map(function(c){ return c.toLowerCase(); }).indexOf(fallback) === -1) colors.push(fallback);
    });
    container.innerHTML = '';
    colors.forEach(function(color) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'ed-color-swatch';
        btn.style.background = color;
        if (color.toLowerCase() === '#ffffff') btn.style.border = '1px solid #ddd';
        btn.addEventListener('click', function() { pickFontColor(color); });
        container.appendChild(btn);
    });
}

/* 글꼴 색상 적용 */
function pickFontColor(color) {
    var hex = document.getElementById('fontColorHex');
    var picker = document.getElementById('fontColorPicker');
    if (hex) hex.value = color;
    if (picker) picker.value = color;
    scheduleLive(100);
}

/* 색상 관련 이벤트 바인딩 */
(function bindColorControls() {
    var hex = document.getElementById('fontColorHex');
    var picker = document.getElementById('fontColorPicker');
    var reset = document.getElementById('fontColorReset');

    if (picker) {
        picker.addEventListener('input', function() {
            if (hex) hex.value = this.value;
            scheduleLive(100);
        });
    }
    if (hex) {
        hex.addEventListener('input', function() {
            var v = this.value;
            if (/^#[0-9a-fA-F]{6}$/.test(v) && picker) picker.value = v;
            scheduleLive(100);
        });
    }
    if (reset) {
        reset.addEventListener('click', function() {
            /* 무조건 검정이 아니라 현재 테마의 기본색으로 — Getting Married(흰 글자)처럼
               어두운/사진 배경 테마에서 검정으로 리셋하면 글자가 안 보이게 되는 문제 방지 */
            var designInput = document.getElementById('mainDesignVal');
            var currentDesign = (designInput && designInput.value) || 'basic';
            pickFontColor(DESIGN_DEFAULT_COLORS[currentDesign] || '#2c2822');
        });
    }
})();

/* 디자인 스크롤 드래그 */
/* 가로 드래그 스크롤 — 여러 컨테이너에 적용 */
function enableDragScroll(el) {
    if (!el) return;
    var isDown = false, startX, scrollLeft, moved = false, downX = 0;
    el.addEventListener('mousedown', function(e) {
        isDown = true; moved = false;
        el.classList.add('grabbing');
        startX = e.pageX - el.offsetLeft;
        scrollLeft = el.scrollLeft;
        downX = e.pageX;
    });
    el.addEventListener('mouseleave', function(){ isDown=false; el.classList.remove('grabbing'); });
    el.addEventListener('mouseup',    function(){ isDown=false; el.classList.remove('grabbing'); });
    el.addEventListener('mousemove', function(e) {
        if (!isDown) return;
        e.preventDefault();
        /* 실제로 일정 거리 이상 움직였을 때만 드래그로 간주 — 미세한 떨림(트랙패드 등)에는 클릭이 씹히지 않도록 */
        if (Math.abs(e.pageX - downX) > 5) moved = true;
        var x = e.pageX - el.offsetLeft;
        el.scrollLeft = scrollLeft - (x - startX) * 1.5;
    });
    /* 드래그 중 클릭 방지 (실제 드래그로 판정된 경우에만) */
    el.addEventListener('click', function(e) {
        if (moved) { e.preventDefault(); e.stopPropagation(); }
    }, true);
}

/* 디자인 카드 + 캘린더 스타일 드래그 스크롤 적용 */
(function initDragScrolls() {
    enableDragScroll(document.getElementById('designScroll'));
    enableDragScroll(document.getElementById('calStyleGrid'));
    enableDragScroll(document.getElementById('galTypeScroll'));
})();

/* 갤러리 타입 선택 */
function pickGalType(el, val) {
    document.querySelectorAll('.gal-type-card').forEach(function(c){ c.classList.remove('active'); });
    el.classList.add('active');
    var input = document.getElementById('galTypeVal');
    if (input) input.value = val;
    scheduleLive(150);
}

/* ──────────────────────────────────────
   날짜 미니 카드 업데이트
────────────────────────────────────── */
function updateDateCard() {
    var DAYS = ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'];
    var MM   = ['01','02','03','04','05','06','07','08','09','10','11','12'];
    /* dc* 요소는 새 디자인에서 제거됨 — 존재할 때만 갱신 */
    function setT(id, text) { var el = document.getElementById(id); if (el) el.textContent = text; }

    var dateEl = document.querySelector('[name="weddingDate"]');
    if (dateEl && dateEl.value) {
        var d = new Date(dateEl.value);
        setT('dcDate', d.getFullYear() + ' / ' + MM[d.getMonth()] + ' / ' + String(d.getDate()).padStart(2,'0'));
        setT('dcDow', DAYS[d.getDay()]);
    }
    var gn = document.querySelector('[name="groomName"]');
    var bn = document.querySelector('[name="brideName"]');
    setT('dcNames', (gn&&gn.value||'신랑') + ' · ' + (bn&&bn.value||'신부'));
    var pl = document.querySelector('[name="weddingPlace"]');
    setT('dcPlace', pl&&pl.value || '예식장');
}

['weddingDate','weddingTime','groomName','brideName','weddingPlace'].forEach(function(nm) {
    var el = document.querySelector('[name="'+nm+'"]');
    if (el) { el.addEventListener('input', updateDateCard); el.addEventListener('change', updateDateCard); }
});
updateDateCard();

/* ──────────────────────────────────────
   메인 사진 업로드
────────────────────────────────────── */
/* 갤러리와 같은 리사이즈 적용 — 원본을 그대로 base64로 저장하면 이 청첩장에서
   가장 자주(전체 화면 히어로) 로드되는 사진이 제일 큰 용량으로 남는 문제 방지.
   히어로 사진은 갤러리 썸네일보다 크게 표시되므로 폭을 조금 더 넉넉히 잡는다. */
function handleMainPhotoFile(file) {
    if (!file) return;
    resizeImage(file, 1440, 0.85, function(dataUrl) {
        var b64 = document.getElementById('mainPhotoBase64');
        if (b64) b64.value = dataUrl.split(',')[1];
        var thumb = document.getElementById('mainThumbImg');
        if (thumb) { thumb.src = dataUrl; thumb.style.objectPosition = ''; }
        /* 새 사진이므로 이전 위치(초점) 조정값 초기화 */
        var posXEl = document.getElementById('mainPhotoPosXInput');
        var posYEl = document.getElementById('mainPhotoPosYInput');
        if (posXEl) posXEl.value = '';
        if (posYEl) posYEl.value = '';
        var hint = document.getElementById('mainUploadHint');
        if (hint) hint.style.display = 'none';
        var thumbWrap = document.getElementById('mainPhotoThumb');
        if (thumbWrap) thumbWrap.style.display = 'block';
        /* 날짜 카드 사진 (있을 때만) */
        var dcImg = document.getElementById('dcImg');
        var dcPh  = document.getElementById('dcPlaceholder');
        if (dcPh)  dcPh.style.display = 'none';
        if (dcImg) { dcImg.src = dataUrl; dcImg.style.display = 'block'; }
        scheduleLive(200);
    });
}

var mainPhotoZoneEl = document.getElementById('mainPhotoZone');
mainPhotoZoneEl.addEventListener('click', function(e) {
    if (e.target.closest('#removeMainPhoto')) return;
    document.getElementById('mainPhotoFile').click();
});
document.getElementById('mainPhotoFile').addEventListener('change', function() {
    handleMainPhotoFile(this.files[0]);
});
/* 드래그 업로드 — 안내 문구("클릭 또는 드래그하여 업로드")는 있었지만
   실제로 드롭을 받는 핸들러가 없어 드래그로는 아무 반응이 없던 문제 */
['dragenter', 'dragover'].forEach(function(evt) {
    mainPhotoZoneEl.addEventListener(evt, function(e) {
        e.preventDefault();
        e.stopPropagation();
        mainPhotoZoneEl.classList.add('drag-over');
    });
});
['dragleave', 'dragend'].forEach(function(evt) {
    mainPhotoZoneEl.addEventListener(evt, function(e) {
        e.preventDefault();
        e.stopPropagation();
        mainPhotoZoneEl.classList.remove('drag-over');
    });
});
mainPhotoZoneEl.addEventListener('drop', function(e) {
    e.preventDefault();
    e.stopPropagation();
    mainPhotoZoneEl.classList.remove('drag-over');
    var file = e.dataTransfer.files && e.dataTransfer.files[0];
    if (file && file.type.indexOf('image/') === 0) handleMainPhotoFile(file);
});
document.getElementById('removeMainPhoto').addEventListener('click', function(e) {
    e.stopPropagation();
    var b64 = document.getElementById('mainPhotoBase64');
    if (b64) b64.value = '';
    var fileEl = document.getElementById('mainPhotoFile');
    if (fileEl) fileEl.value = '';
    var thumbWrap = document.getElementById('mainPhotoThumb');
    if (thumbWrap) thumbWrap.style.display = 'none';
    var hint = document.getElementById('mainUploadHint');
    if (hint) hint.style.display = '';
    var dcImg = document.getElementById('dcImg');
    var dcPh  = document.getElementById('dcPlaceholder');
    if (dcImg) dcImg.style.display = 'none';
    if (dcPh)  dcPh.style.display = '';
    var posXEl = document.getElementById('mainPhotoPosXInput');
    var posYEl = document.getElementById('mainPhotoPosYInput');
    if (posXEl) posXEl.value = '';
    if (posYEl) posYEl.value = '';
    scheduleLive(200);
});

/* ──────────────────────────────────────
   메인 사진 위치(초점) 드래그 조정 — object-position을 드래그로 지정
────────────────────────────────────── */
(function() {
    var thumbImg  = document.getElementById('mainThumbImg');
    var posXInput = document.getElementById('mainPhotoPosXInput');
    var posYInput = document.getElementById('mainPhotoPosYInput');
    if (!thumbImg || !posXInput || !posYInput) return;

    var dragging = false;
    var wasDragged = false;
    var startClientX, startClientY, startPosX, startPosY;

    function currentPos() {
        var x = parseFloat(posXInput.value);
        var y = parseFloat(posYInput.value);
        return { x: isNaN(x) ? 50 : x, y: isNaN(y) ? 50 : y };
    }

    function applyPos(x, y) {
        x = Math.max(0, Math.min(100, x));
        y = Math.max(0, Math.min(100, y));
        thumbImg.style.objectPosition = x + '% ' + y + '%';
        posXInput.value = x.toFixed(1);
        posYInput.value = y.toFixed(1);
    }

    /* 저장된 위치가 있으면 썸네일에도 반영 */
    if (posXInput.value !== '' && posYInput.value !== '') {
        var initial = currentPos();
        thumbImg.style.objectPosition = initial.x + '% ' + initial.y + '%';
    }

    thumbImg.style.cursor = 'move';
    thumbImg.title = '드래그하여 사진 위치를 조정하세요';

    thumbImg.addEventListener('pointerdown', function(e) {
        dragging = true;
        wasDragged = false;
        startClientX = e.clientX; startClientY = e.clientY;
        var p = currentPos();
        startPosX = p.x; startPosY = p.y;
        try { thumbImg.setPointerCapture(e.pointerId); } catch (err) {}
        e.preventDefault();
    });

    thumbImg.addEventListener('pointermove', function(e) {
        if (!dragging) return;
        var dx = e.clientX - startClientX;
        var dy = e.clientY - startClientY;
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) wasDragged = true;
        var rect = thumbImg.getBoundingClientRect();
        if (!rect.width || !rect.height) return;
        /* 커서를 따라 사진이 움직이는 것처럼 보이도록 초점은 반대 방향으로 이동 */
        var newX = startPosX - (dx / rect.width) * 100;
        var newY = startPosY - (dy / rect.height) * 100;
        applyPos(newX, newY);
    });

    function endDrag() {
        if (!dragging) return;
        dragging = false;
        if (wasDragged) scheduleLive(150);
    }
    thumbImg.addEventListener('pointerup', endDrag);
    thumbImg.addEventListener('pointercancel', endDrag);

    /* 드래그 직후 발생하는 click이 업로드 창을 다시 여는 것을 방지 */
    thumbImg.addEventListener('click', function(e) {
        if (wasDragged) { e.stopPropagation(); wasDragged = false; }
    });
})();

/* ──────────────────────────────────────
   갤러리 업로드
────────────────────────────────────── */
document.getElementById('galZone').addEventListener('click', function() {
    document.getElementById('galFile').click();
});
document.getElementById('galFile').addEventListener('change', function() {
    var files = Array.from(this.files);
    files.forEach(function(file) {
        resizeImage(file, 1200, 0.82, function(dataUrl) {
            addGalThumb(dataUrl);
        });
    });
    this.value = ''; /* 같은 파일 재선택 가능하게 */
});

/* 이미지 리사이즈 — base64 크기 줄이기 */
function resizeImage(file, maxWidth, quality, callback) {
    var reader = new FileReader();
    reader.onload = function(ev) {
        var img = new Image();
        img.onload = function() {
            var canvas = document.createElement('canvas');
            var w = img.width, h = img.height;
            if (w > maxWidth) {
                h = Math.round(h * (maxWidth / w));
                w = maxWidth;
            }
            canvas.width = w;
            canvas.height = h;
            var ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0, w, h);
            callback(canvas.toDataURL('image/jpeg', quality));
        };
        img.onerror = function() { callback(ev.target.result); };
        img.src = ev.target.result;
    };
    reader.readAsDataURL(file);
}

var galImages = [];
function addGalThumb(dataUrl) {
    galImages.push(dataUrl);
    renderGalThumbs();
    document.getElementById('galleryImagesInput').value = galImages.join('|||');
    scheduleLive(300);
}
function renderGalThumbs() {
    var c = document.getElementById('galThumbs');
    if (!c) return;

    /* 기존 Sortable 인스턴스 제거 */
    if (c._sortable) { c._sortable.destroy(); c._sortable = null; }

    c.innerHTML = '';
    var row = document.getElementById('galThumbsRow');
    if (row) row.style.display = galImages.length ? 'flex' : 'none';

    galImages.forEach(function(url) {
        var wrap = document.createElement('div');
        wrap.className = 'ed-thumb';
        wrap.innerHTML = '<img src="' + url + '" alt=""><button type="button" class="gal-del-btn">✕</button>';
        wrap.querySelector('.gal-del-btn').addEventListener('click', function() {
            var idx = Array.from(c.children).indexOf(wrap);
            galImages.splice(idx, 1);
            renderGalThumbs();
            document.getElementById('galleryImagesInput').value = galImages.join('|||');
            scheduleLive(300);
        });
        c.appendChild(wrap);
    });

    /* 드래그앤드롭 정렬 (2장 이상일 때만) */
    if (window.Sortable && galImages.length > 1) {
        c._sortable = Sortable.create(c, {
            animation: 150,
            ghostClass: 'ed-thumb-ghost',
            onEnd: function() {
                galImages = Array.from(c.querySelectorAll('img')).map(function(img) { return img.src; });
                document.getElementById('galleryImagesInput').value = galImages.join('|||');
                scheduleLive(300);
            }
        });
    }
}

/* ──────────────────────────────────────
   계좌 관리
────────────────────────────────────── */
function switchAcctTab(side) {
    ['groom','bride'].forEach(function(s) {
        document.getElementById('acctPanel'+cap(s)).style.display = s===side?'block':'none';
        document.getElementById('acctAddBtn'+cap(s)).style.display = s===side?'flex':'none';
        document.getElementById('acctTab'+cap(s)).classList.toggle('active', s===side);
    });
}
function cap(s) { return s.charAt(0).toUpperCase()+s.slice(1); }

function addAcctRow(side) {
    acctData[side].push({side:side,owner:'',bank:'은행 선택',accountNumber:'',kakaoPayUrl:'',sortOrder:acctData[side].length});
    renderAcctList(side);
}
function renderAcctList(side) {
    var c = document.getElementById('acctPanel'+cap(side));
    c.innerHTML = '';
    acctData[side].forEach(function(acct, idx) {
        var div = document.createElement('div');
        div.className = 'ed-acct-row';
        var opts = BANKS.map(function(b){ return '<option'+(acct.bank===b?' selected':'')+'>'+b+'</option>'; }).join('');
        div.innerHTML =
            '<div style="display:flex;justify-content:space-between;margin-bottom:5px;"><span style="font-size:11px;color:#b89870;">'+(idx+1)+'</span>'+
            '<button type="button" class="ed-acct-del" data-idx="'+idx+'" data-side="'+side+'"><i class="ti ti-trash"></i></button></div>'+
            '<div class="ed-acct-fields">'+
                '<div class="ed-acct-field-row"><span class="ed-acct-label">예금주</span><input class="ed-input" type="text" placeholder="예금주" value="'+(acct.owner||'')+'" data-field="owner"></div>'+
                '<div class="ed-acct-field-row"><span class="ed-acct-label">은행</span><select class="ed-select" data-field="bank">'+opts+'</select></div>'+
                '<div class="ed-acct-field-row"><span class="ed-acct-label">계좌번호</span><input class="ed-input" type="text" placeholder="계좌번호" value="'+(acct.accountNumber||'')+'" data-field="accountNumber"></div>'+
                '<div class="ed-acct-field-row"><span class="ed-acct-label">카카오페이 링크 <span style="color:#bbb">(선택)</span></span><input class="ed-input" type="text" placeholder="https://qr.kakaopay.com/..." value="'+(acct.kakaoPayUrl||'')+'" data-field="kakaoPayUrl"></div>'+
            '</div>';
        div.querySelector('.ed-acct-del').addEventListener('click', function() {
            acctData[this.dataset.side].splice(parseInt(this.dataset.idx),1);
            renderAcctList(this.dataset.side);
        });
        div.querySelectorAll('[data-field]').forEach(function(el) {
            el.addEventListener('input',  function(){ acctData[side][idx][this.dataset.field]=this.value; });
            el.addEventListener('change', function(){ acctData[side][idx][this.dataset.field]=this.value; });
        });
        c.appendChild(div);
    });
}

function saveAllAccounts() {
    var all = acctData.groom.concat(acctData.bride);
    var btn = document.querySelector('.ed-btn-save-acct');
    var msg = document.getElementById('acctSaveMsg');
    btn.disabled = true; btn.textContent = '저장 중...';
    fetch('/api/account/bulk?weddingId=' + WEDDING.id, {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(all)})
        .then(function(r){ return r.json(); })
        .then(function(d) {
            if (d.success) { msg.textContent='✓ 저장되었습니다'; msg.style.color='#6a8a5a'; setTimeout(function(){ msg.textContent=''; },3000); }
            else { msg.textContent=d.error||'저장 실패'; msg.style.color='#c4748a'; }
        })
        .finally(function() { btn.disabled=false; btn.innerHTML='<i class="ti ti-device-floppy"></i> 계좌 저장'; });
}
fetch('/api/account?weddingId=' + WEDDING.id).then(function(r){ return r.json(); }).then(function(data) {
    acctData.groom = data.filter(function(a){ return a.side==='groom'; });
    acctData.bride = data.filter(function(a){ return a.side==='bride'; });
    renderAcctList('groom'); renderAcctList('bride');
});

/* ──────────────────────────────────────
   실시간 미리보기 — postMessage
────────────────────────────────────── */
if (liveFrame) {
    liveFrame.addEventListener('load', function() {
        previewReady = false;
        setTimeout(function() {
            previewReady = true;
            /* 첫 sendLive는 스크롤 없이 — scrollSync 잠시 차단 */
            var saved = scrollSync;
            scrollSync = false;
            sendLive();
            setTimeout(function() { scrollSync = saved; }, 1500);
        }, 400);
    });

    /* fallback — iframe load 이벤트가 오지 않을 경우 3초 후 강제 활성화 */
    setTimeout(function() {
        if (!previewReady) {
            previewReady = true;
            sendLive();
        }
    }, 3000);
}

function collectData() {
    var form = document.getElementById('editForm');
    var data = {};
    new FormData(form).forEach(function(v,k){ data[k]=v; });

    /* 체크박스 — id 우선, 없으면 name으로 찾기 */
    var TOGGLES = {
        'greetingVisible': 'chkGreet',
        'hostsVisible':    'chkHosts',
        'calendarVisible': 'chkCal',
        'ddayVisible':     'chkDday',
        'galleryVisible':  'chkGal',
        'mapVisible':      'chkMap',
        'accountVisible':  'chkAcct',
        'rsvpEnabled':     'chkRsvp'
    };
    Object.keys(TOGGLES).forEach(function(name) {
        var el = document.getElementById(TOGGLES[name])
                 || document.querySelector('[name="' + name + '"]');
        data[name] = el ? el.checked : false;
    });

    /* 나머지 체크박스 */
    ['groomFatherDeceased','groomMotherDeceased','brideFatherDeceased','brideMotherDeceased',
     'mapNaviKakao','mapNaviTmap','mapNaviNaver'].forEach(function(f) {
        var el = document.querySelector('[name="' + f + '"]');
        data[f] = el ? el.checked : false;
    });

    /* contactPopupEnabled — hidden input (값: "true"/"false" 문자열) */
    var contactEl = document.getElementById('contactInput');
    data.contactPopupEnabled = contactEl ? (contactEl.value === 'true') : true;

    if (data.id) data.id = parseInt(data.id) || null;

    /* mainPhotoBase64 — hidden input 직접 읽기 (FormData에서 누락 방지) */
    var b64El = document.getElementById('mainPhotoBase64');
    if (b64El) data.mainPhotoBase64 = b64El.value || '';

    /* photoFilter — hidden input 직접 읽기 */
    var pfEl = document.getElementById('photoFilterInput');
    if (pfEl) data.photoFilter = pfEl.value || 'none';

    /* deceasedDisplayType — 혼주섹션 탭 값 명시 읽기 */
    var ddEl = document.getElementById('deceasedInput');
    if (ddEl) data.deceasedDisplayType = ddEl.value || 'hanja';

    return data;
}

function sendLive() {
    if (!previewReady || !liveFrame) return;
    var data = collectData();

    /* base64 사진은 payload 크기 문제로 별도 메시지로 전송 */
    var photoB64 = data.mainPhotoBase64 || '';
    var photoFilter = data.photoFilter || 'none';
    var photoPosX = data.mainPhotoPosX !== undefined && data.mainPhotoPosX !== '' ? parseFloat(data.mainPhotoPosX) : null;
    var photoPosY = data.mainPhotoPosY !== undefined && data.mainPhotoPosY !== '' ? parseFloat(data.mainPhotoPosY) : null;
    delete data.mainPhotoBase64;  /* 메인 payload에서 제거 */

    try {
        /* 1) 일반 데이터 먼저 전송 */
        liveFrame.contentWindow.postMessage({type:'WEDDING_LIVE_UPDATE', payload:data}, window.location.origin);
        /* 2) 사진 + 필터 + 위치는 별도 메시지로 전송 (약간 딜레이로 순서 보장) */
        setTimeout(function() {
            try {
                liveFrame.contentWindow.postMessage({
                    type: 'WEDDING_PHOTO_UPDATE',
                    payload: { mainPhotoBase64: photoB64, photoFilter: photoFilter, mainPhotoPosX: photoPosX, mainPhotoPosY: photoPosY }
                }, window.location.origin);
            } catch(e2) {}
        }, 50);
    } catch(e) {}
}

function scheduleLive(ms) {
    clearTimeout(liveTimer);
    liveTimer = setTimeout(sendLive, ms||80);
    /* 백그라운드 자동저장 (조용히, 새로고침 없음) */
    clearTimeout(saveTimer);
    saveTimer = setTimeout(autoSaveAndRefresh, 2500);
}

function autoSave() {
    fetch('/api/admin/autosave', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(collectData())})
        .then(function(r){ return r.json(); })
        .then(function(res){ if (res && res.success) markDraftSaved(); })
        .catch(function(){});
}

/* 백그라운드 자동저장 — iframe 새로고침 없이 조용히 임시저장만 (게스트 화면에는 반영되지 않음) */
function autoSaveAndRefresh() {
    var data = collectData();
    fetch('/api/admin/autosave', {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify(data)
    })
        .then(function(r){ return r.json(); })
        .then(function(res){ if (res && res.success) markDraftSaved(); })
        .catch(function(){});
    /* iframe 리로드 제거 — postMessage로 미리보기는 이미 갱신됨 (깜빡임 방지) */
}

/* 전체 미리보기/저장 시에만 명시적 새로고침 (스크롤 위치 보존) */
function refreshPreview() {
    if (!liveFrame) return;

    /* 현재 스크롤 위치 저장 */
    var scrollY = 0;
    try { scrollY = liveFrame.contentWindow.scrollY || 0; } catch(e){}

    /* 방법1: src 재로드 + 스크롤 복원 (가장 확실) */
    window._pendingScrollY = scrollY;
    liveFrame.onload = function() {
        liveFrame.onload = null;
        var target = window._pendingScrollY || 0;
        /* rAF 두 번 — 렌더링 완료 후 스크롤 */
        requestAnimationFrame(function() {
            requestAnimationFrame(function() {
                try { liveFrame.contentWindow.scrollTo(0, target); } catch(e) {}
                try {
                    liveFrame.contentWindow.postMessage({
                        type: 'WEDDING_SCROLL_RESTORE', scrollY: target
                    }, '*');
                } catch(e) {}
            });
        });
    };
    liveFrame.src = INVITATION_URL + (INVITATION_URL.indexOf('?') >= 0 ? '&' : '?') + 't=' + Date.now();
}

/* 모든 폼 변경 감지 */
document.getElementById('editForm').addEventListener('input',  function() { scheduleLive(80); });
document.getElementById('editForm').addEventListener('change', function() { scheduleLive(30); });

/* 이름 필드 — 타이핑 즉시(10ms) 전송 */
['groomName','brideName'].forEach(function(name) {
    var el = document.querySelector('[name="' + name + '"]');
    if (!el) return;
    el.addEventListener('input', function() {
        clearTimeout(liveTimer);
        liveTimer = setTimeout(sendLive, 10); /* 거의 즉시 */
    });
});

/* 섹션 표시/숨김 토글 — postMessage로 즉시 반영 (iframe 재로드 없음) */
['chkGreet','chkHosts','chkCal','chkDday','chkGal','chkMap','chkAcct','chkRsvp'].forEach(function(id) {
    var el = document.getElementById(id);
    if (!el) return;
    el.addEventListener('change', function() {
        clearTimeout(saveTimer);
        /* postMessage로 즉시 반영 — 스크롤 건드리지 않음 */
        sendLive();
        /* 백그라운드 자동저장 (조용히) */
        saveTimer = setTimeout(autoSave, 500);
    });
});

/* ──────────────────────────────────────
   게시 버튼 → 임시저장 내용을 게스트 화면에 실제로 반영
────────────────────────────────────── */
document.getElementById('topSaveBtn').addEventListener('click', function() {
    var btn = this;
    btn.disabled = true;
    btn.textContent = '게시 중...';

    saveAllAccounts();
    var data = collectData();

    fetch('/api/admin/publish', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify(data)
    })
    .then(function(r){ return r.json(); })
    .then(function(res) {
        btn.disabled = false;
        btn.textContent = '게시하기';
        if (res.success) {
            showEditorToast('✓ 게시되었습니다');
            markPublished();
            sendLive();
        } else {
            showEditorToast('게시 실패: ' + (res.error || ''), 'error');
        }
    })
    .catch(function() {
        btn.disabled = false;
        btn.textContent = '게시하기';
        showEditorToast('게시 실패. 다시 시도해 주세요.', 'error');
    });
});

/* ── 하단 게시하기 — 게시 후 새 창으로 청첩장 열기 ── */
var bottomSaveBtn = document.getElementById('bottomSaveBtn');
if (bottomSaveBtn) {
    bottomSaveBtn.addEventListener('click', function() {
        var btn = this;
        btn.disabled = true;
        btn.innerHTML = '<i class="ti ti-loader-2" style="font-size:14px;animation:spin 1s linear infinite;"></i> 게시 중...';

        saveAllAccounts();
        var data = collectData();

        fetch('/api/admin/publish', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify(data)
        })
        .then(function(r){ return r.json(); })
        .then(function(res) {
            btn.disabled = false;
            btn.innerHTML = '<i class="ti ti-external-link" style="font-size:14px;"></i> 게시하기';
            if (res.success) {
                markPublished();
                showEditorToast('✓ 게시 완료! 청첩장을 엽니다.');
                /* 저장 완료 후 새 창으로 청첩장 열기 */
                setTimeout(function() {
                    var viewLink = document.getElementById('viewInviteLink');
                    window.open(viewLink ? viewLink.href : '/', '_blank');
                }, 400);
            } else {
                showEditorToast('게시 실패: ' + (res.error || ''), 'error');
            }
        })
        .catch(function() {
            btn.disabled = false;
            btn.innerHTML = '<i class="ti ti-external-link" style="font-size:14px;"></i> 게시하기';
            showEditorToast('게시 실패. 다시 시도해 주세요.', 'error');
        });
    });
}

/* 에디터 토스트 알림 */
function showEditorToast(msg, type) {
    var t = document.getElementById('editorToast');
    if (!t) {
        t = document.createElement('div');
        t.id = 'editorToast';
        t.style.cssText = 'position:fixed;bottom:30px;left:50%;transform:translateX(-50%);' +
            'background:#2c2822;color:#fff;padding:10px 22px;border-radius:22px;' +
            'font-size:13px;font-family:Noto Sans KR,sans-serif;z-index:9999;' +
            'opacity:0;transition:opacity .3s;pointer-events:none;white-space:nowrap;';
        document.body.appendChild(t);
    }
    t.textContent = msg;
    t.style.background = type === 'error' ? '#c4748a' : '#2c2822';
    t.style.opacity = '1';
    clearTimeout(t._timer);
    t._timer = setTimeout(function(){ t.style.opacity = '0'; }, 2500);
}

/* ──────────────────────────────────────
   전체화면 미리보기
────────────────────────────────────── */
document.getElementById('fullPrevBtn').addEventListener('click', function() {
    var overlay = document.getElementById('fullPrevOverlay');
    var frame = document.getElementById('fullPrevFrame');

    frame.onload = function() {
        frame.onload = null;
        setTimeout(function() {
            var data = collectData();
            var photoB64 = data.mainPhotoBase64 || '';
            var photoFilter = data.photoFilter || 'none';
            var photoPosX = data.mainPhotoPosX !== undefined && data.mainPhotoPosX !== '' ? parseFloat(data.mainPhotoPosX) : null;
            var photoPosY = data.mainPhotoPosY !== undefined && data.mainPhotoPosY !== '' ? parseFloat(data.mainPhotoPosY) : null;
            delete data.mainPhotoBase64;
            try {
                frame.contentWindow.postMessage({type:'WEDDING_LIVE_UPDATE', payload:data}, window.location.origin);
                setTimeout(function() {
                    try {
                        frame.contentWindow.postMessage({
                            type:'WEDDING_PHOTO_UPDATE',
                            payload:{mainPhotoBase64:photoB64, photoFilter:photoFilter, mainPhotoPosX:photoPosX, mainPhotoPosY:photoPosY}
                        }, window.location.origin);
                    } catch(e2) {}
                }, 60);
            } catch(e) {}
        }, 400);
    };

    frame.src = INVITATION_URL;
    overlay.classList.add('open');
    document.body.style.overflow = 'hidden';
    var t = new Date(); document.getElementById('fullPrevTime').textContent = t.getHours()+':'+String(t.getMinutes()).padStart(2,'0');
});
document.getElementById('fullPrevClose').addEventListener('click', function() {
    document.getElementById('fullPrevOverlay').classList.remove('open');
    document.body.style.overflow = '';
    document.getElementById('fullPrevFrame').src = '';
});
document.getElementById('fullPrevOverlay').addEventListener('click', function(e) {
    if (e.target===this) document.getElementById('fullPrevClose').click();
});
document.addEventListener('keydown', function(e){ if(e.key==='Escape') document.getElementById('fullPrevClose').click(); });

/* ──────────────────────────────────────
   "편집 시 해당 화면으로 이동" 토글
────────────────────────────────────── */
var scrollToggle = document.getElementById('scrollToggle');
scrollToggle.addEventListener('click', function() {
    scrollSync = !scrollSync;
    this.classList.toggle('on', scrollSync);
});

/* ──────────────────────────────────────
   섹션 클릭 → 미리보기 해당 위치로 이동
────────────────────────────────────── */
var SECTION_SCROLL_MAP = {
    'main':    null,
    'basic':   null,
    'wedding': '.info-block',
    'greet':   '.greet-card',
    'hosts':   '.hosts-wrap',
    'cal':     '#calCard',
    'dday':    '#ddayCard',
    'gal':     '#galleryOuter',
    'map':     '.map-card',
    'acct':    '.acct-tab-wrap',
    'rsvp':    '#rsvpCard',
};

function scrollPreviewTo(secId) {
    if (!scrollSync || !previewReady) return;
    try {
        liveFrame.contentWindow.postMessage({
            type: 'WEDDING_SCROLL_TO',
            section: secId
        }, window.location.origin);
    } catch(e) {}
}

/* 섹션 헤더 클릭 시 미리보기 이동 */
document.querySelectorAll('.ed-sec-hd').forEach(function(hd) {
    hd.addEventListener('click', function(e) {
        if (e.target.closest('.ed-toggle-wrap')) return;
        if (!scrollSync) return;
        var secId = this.dataset.sec;
        if (secId) scrollPreviewTo(secId);
    });
});

/* 입력 필드 포커스/입력 시 해당 섹션으로 미리보기 자동 스크롤 */
(function bindFieldScrollSync() {
    function getSectionKey(el) {
        var section = el.closest('.ed-section');
        if (!section) return null;
        var id = section.id || '';
        return id.replace(/^sec-/, '');
    }

    var lastScrolled = null;
    /* 페이지 로드 직후 1.5초간 스크롤 차단 (초기화로 인한 오작동 방지) */
    var _readyForScroll = false;
    setTimeout(function() { _readyForScroll = true; }, 1500);

    function syncToField(el) {
        if (!_readyForScroll) return;   /* 로드 직후 차단 */
        if (!scrollSync) return;         /* 토글 중 차단 */
        var key = getSectionKey(el);
        if (!key || key === lastScrolled) return;
        lastScrolled = key;
        scrollPreviewTo(key);
        setTimeout(function(){ lastScrolled = null; }, 800);
    }

    /* input, textarea — 텍스트 입력/스타일 변경 시 스크롤 */
    var form = document.getElementById('editForm');
    if (form) {
        form.addEventListener('focusin', function(e) {
            var t = e.target;
            if (t.matches('input[type="checkbox"], input[type="radio"], button')) return;
            if (t.closest('.ed-toggle-wrap')) return;
            if (t.closest('.ed-tabs')) return;
            if (t.closest('.ed-zoom-tabs')) return;
            if (t.closest('.ed-navi-chip')) return;
            if (t.matches('input[type="text"], input[type="date"], input[type="time"], textarea, select')) {
                syncToField(t);
            }
        });
    }

    /* 폼 밖 동적 영역 */
    document.addEventListener('focusin', function(e) {
        var t = e.target;
        if (t.matches('input[type="checkbox"], input[type="radio"], button')) return;
        if (t.closest('.ed-toggle-wrap')) return;
        if (t.closest('.ed-navi-chip')) return;
        if (t.matches('input[type="text"], textarea') && t.closest('.ed-section')) {
            syncToField(t);
        }
    });

    /* 섹션 내 값 변경(select·radio·checkbox 포함) 시 미리보기 해당 섹션으로 스크롤 */
    document.addEventListener('change', function(e) {
        var t = e.target;
        if (!t.closest('.ed-section')) return;
        if (t.closest('.ed-navi-chip')) return;
        syncToField(t);
    });
})();

/* ──────────────────────────────────────
   슬라이드 메뉴 패널
────────────────────────────────────── */
/* 순서는 청첩장(invitation.html)의 실제 렌더 순서와 일치시킨다 —
   여기가 sectionOrder가 비어 있을 때의 기본값이 된다 */
var NAV_SECTIONS = [
    {id:'greet',  label:'인사말',            chk:'chkGreet'},
    {id:'cal',    label:'캘린더',            chk:'chkCal'},
    {id:'dday',   label:'D-Day',             chk:'chkDday'},
    {id:'hosts',  label:'혼주정보 & 연락처', chk:'chkHosts'},
    {id:'gal',    label:'이미지 갤러리',     chk:'chkGal'},
    {id:'map',    label:'지도',              chk:'chkMap'},
    {id:'acct',   label:'계좌 송금',         chk:'chkAcct'},
    {id:'rsvp',   label:'참석 여부',         chk:'chkRsvp'},
];

/* 저장된 sectionOrder를 NAV_SECTIONS에 적용한 목록.
   저장값에 없는 섹션은 원래 순서를 유지하며 뒤에 붙는다(섹션이 추가돼도 유실되지 않도록). */
function orderedNavSections() {
    var input = document.getElementById('sectionOrderInput');
    var saved = (input && input.value ? input.value.split(',') : [])
                .map(function(s) { return s.trim(); }).filter(Boolean);
    if (!saved.length) return NAV_SECTIONS.slice();

    var byId = {};
    NAV_SECTIONS.forEach(function(def) { byId[def.id] = def; });

    var out = [];
    saved.forEach(function(id) {
        if (byId[id]) { out.push(byId[id]); delete byId[id]; }
    });
    NAV_SECTIONS.forEach(function(def) { if (byId[def.id]) out.push(def); });
    return out;
}

/* 패널 열기/닫기 */
function openNavPanel() {
    buildNavPanel();
    document.getElementById('navPanel').classList.add('open');
    document.querySelector('.ed-main').classList.add('panel-open');   /* 편집영역 오른쪽 패딩 확보 */
    document.querySelector('.ed-float').classList.add('panel-open');
    document.getElementById('floatIconBtn').classList.add('active');
}
function closeNavPanel() {
    document.getElementById('navPanel').classList.remove('open');
    document.querySelector('.ed-main').classList.remove('panel-open');
    document.querySelector('.ed-float').classList.remove('panel-open');
    document.getElementById('floatIconBtn').classList.remove('active');
}

/* 패널 내용 구성 */
function buildNavPanel() {
    var toggleContainer = document.getElementById('navPanelToggles');
    if (toggleContainer.children.length > 0) {
        /* 이미 구성됨 — 체크박스 상태만 동기화 */
        toggleContainer.querySelectorAll('[data-chk]').forEach(function(row) {
            var chkId = row.dataset.chk;
            var chk = chkId ? document.getElementById(chkId) : null;
            var mirror = row.querySelector('input[type="checkbox"]');
            if (chk && mirror) mirror.checked = chk.checked;
        });
        return;
    }

    /* 고정 섹션 목록 클릭 — 메뉴 유지 */
    document.querySelectorAll('.nav-panel-sec-item').forEach(function(item) {
        item.addEventListener('click', function() {
            scrollToSection(this.dataset.sec);
            document.querySelectorAll('.nav-panel-sec-item').forEach(function(i){ i.classList.remove('active'); });
            this.classList.add('active');
        });
    });

    /* 토글 가능 섹션 목록 — 저장된 순서가 있으면 그 순서로 (없으면 NAV_SECTIONS 기본 순서) */
    orderedNavSections().forEach(function(def) {
        var chk = def.chk ? document.getElementById(def.chk) : null;
        var row = document.createElement('div');
        row.className = 'nav-panel-toggle-row';
        row.dataset.chk = def.chk || '';
        row.dataset.secId = def.id;
        row.innerHTML =
            '<label class="ed-toggle-wrap" onclick="event.stopPropagation()" style="flex-shrink:0;">'+
                '<input type="checkbox"'+(chk&&chk.checked?' checked':'')+'>'+
                '<span class="ed-toggle-slider"></span>'+
            '</label>'+
            '<span class="nav-panel-toggle-name">'+def.label+'</span>'+
            '<i class="ti ti-grip-vertical nav-panel-handle"></i>';

        var mirror = row.querySelector('input');
        if (chk) {
            mirror.addEventListener('change', function() {
                chk.checked = this.checked;
                chk.dispatchEvent(new Event('change',{bubbles:true}));
                scheduleLive(200);
            });
            chk.addEventListener('change', function() { mirror.checked = chk.checked; });
        }
        /* 이름 클릭 → 섹션으로 이동 (메뉴 유지) */
        row.querySelector('.nav-panel-toggle-name').addEventListener('click', function() {
            scrollToSection(def.id);
        });

        toggleContainer.appendChild(row);
    });

    initNavPanelDrag();
}

/* 패널 드래그-앤-드롭 순서 변경 */
function initNavPanelDrag() {
    var container = document.getElementById('navPanelToggles');
    var dragEl    = null;
    var blockDrag = false;   /* 토글 클릭 시 드래그 차단 */

    function getRows() {
        return Array.prototype.slice.call(
            container.querySelectorAll('.nav-panel-toggle-row'));
    }

    function clearIndicators() {
        getRows().forEach(function(r) {
            r.classList.remove('drop-above', 'drop-below');
        });
    }

    /* 에디터 폼 섹션 DOM 순서를 패널 순서에 맞게 재정렬 */
    function reorderEditorSections() {
        var rows   = getRows();
        var anchor = document.getElementById('sec-wedding');
        if (!anchor) return;
        rows.forEach(function(row) {
            var sid = row.dataset.secId;
            if (!sid) return;
            var sec = document.getElementById('sec-' + sid);
            if (!sec) return;
            if (anchor.nextElementSibling !== sec) {
                anchor.parentNode.insertBefore(sec, anchor.nextElementSibling);
            }
            anchor = sec;
        });

        /* 순서를 폼에 기록 — 저장/게시 시 sectionOrder로 넘어가 게시본에도 반영된다 */
        var order = rows.map(function(r) { return r.dataset.secId; }).filter(Boolean);
        var orderInput = document.getElementById('sectionOrderInput');
        if (orderInput) orderInput.value = order.join(',');

        /* 라이브 프리뷰(청첩장 iframe)에도 순서 전달 */
        try {
            if (liveFrame && previewReady) {
                liveFrame.contentWindow.postMessage(
                    { type: 'WEDDING_SECTION_ORDER', order: order },
                    window.location.origin
                );
            }
        } catch(e) {}
    }

    /* 각 행에 draggable + 이벤트 등록 */
    getRows().forEach(function(row) {
        row.setAttribute('draggable', 'true');

        /* 토글 영역 mousedown → 드래그 차단 */
        row.addEventListener('mousedown', function(e) {
            blockDrag = !!e.target.closest('.ed-toggle-wrap');
        });

        row.addEventListener('dragstart', function(e) {
            if (blockDrag) { e.preventDefault(); return; }
            dragEl = row;
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', row.dataset.secId || '');
            /* 약간 딜레이 후 반투명 처리 (ghost 이미지 생성 후) */
            setTimeout(function() {
                if (dragEl) dragEl.classList.add('dragging');
            }, 0);
        });

        row.addEventListener('dragover', function(e) {
            if (!dragEl || dragEl === row) return;
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            var rect  = row.getBoundingClientRect();
            var upper = e.clientY < rect.top + rect.height / 2;
            clearIndicators();
            row.classList.add(upper ? 'drop-above' : 'drop-below');
        });

        row.addEventListener('dragleave', function(e) {
            /* 자식 요소로 이동할 때 잘못 발동하지 않도록 */
            if (row.contains(e.relatedTarget)) return;
            row.classList.remove('drop-above', 'drop-below');
        });

        row.addEventListener('drop', function(e) {
            e.preventDefault();
            if (!dragEl || dragEl === row) return;
            var rect  = row.getBoundingClientRect();
            var upper = e.clientY < rect.top + rect.height / 2;
            clearIndicators();
            if (upper) {
                container.insertBefore(dragEl, row);
            } else {
                container.insertBefore(dragEl, row.nextElementSibling);
            }
            reorderEditorSections();
            /* 바뀐 순서를 임시저장에 반영 (hidden input은 프로그램으로 바꿔서 change 이벤트가 안 뜸) */
            scheduleLive(150);
        });

        row.addEventListener('dragend', function() {
            if (dragEl) dragEl.classList.remove('dragging');
            dragEl    = null;
            blockDrag = false;
            clearIndicators();
        });
    });

    /* 편집기 폼의 섹션 순서를 패널(=저장된 순서)에 맞춰 한 번 정렬 —
       편집 순서와 하객이 보는 순서가 어긋나지 않도록 */
    reorderEditorSections();
}

/* 섹션으로 스크롤 */
function scrollToSection(secId) {
    var sec = document.getElementById('sec-'+secId);
    if (!sec) return;
    var bd = document.getElementById('bd-'+secId);
    var chev = sec.querySelector('.ed-chevron');
    if (bd && bd.style.display === 'none') {
        sec.classList.add('open'); sec.classList.remove('collapsed');
        bd.style.display = '';
        if (chev) chev.style.transform = 'rotate(0deg)';
    }
    sec.scrollIntoView({behavior:'smooth', block:'start'});
    scrollPreviewTo(secId);
}

/* 스크롤 스파이 → 패널 내 활성 섹션 표시 */
var mainEl = document.querySelector('.ed-main');
mainEl.addEventListener('scroll', function() {
    var secs = ['main','basic','wedding','greet','hosts','cal','dday','gal','map','acct','rsvp'];
    var active = secs[0];
    secs.forEach(function(id) {
        var el = document.getElementById('sec-'+id);
        if (el && el.getBoundingClientRect().top < 180) active = id;
    });
    document.querySelectorAll('.nav-panel-sec-item').forEach(function(item) {
        item.classList.toggle('active', item.dataset.sec === active);
    });
});

/* 햄버거 버튼 — 토글 방식 */
document.getElementById('floatIconBtn').addEventListener('click', function() {
    var panel = document.getElementById('navPanel');
    if (panel.classList.contains('open')) {
        closeNavPanel();
    } else {
        openNavPanel();
    }
});
document.getElementById('navPanelClose').addEventListener('click', closeNavPanel);
/* 메뉴를 열지 않아도 저장된 섹션 순서가 편집기에 반영되도록 로드 시 한 번 구성 */
buildNavPanel();
/* ESC 키 */
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeNavPanel();
});

/* ──────────────────────────────────────
   지도 (카카오)
────────────────────────────────────── */
var kakaoMap = null;
/* SDK 제거됨 — 정적 이미지 방식 사용 */
function initKakaoMap(lat, lng) {}
function searchAddress() {}

/* ── 지도 줌 선택 ── */
var ZOOM_LEVEL = {'20M':3,'30M':4,'50M':5,'100M':6,'250M':7,'500M':8};
var currentZoomVal = '50M'; /* 현재 줌 값 저장 */

function pickZoom(el, val) {
    document.querySelectorAll('#mapZoomTabs .ed-zoom-btn').forEach(function(b){ b.classList.remove('active'); });
    el.classList.add('active');
    var input = document.getElementById('mapZoomInput');
    if (input) input.value = val;
    currentZoomVal = val;
    var levelMap = {'20M':3,'30M':4,'50M':5,'100M':6,'250M':7,'500M':8};
    /* 이미 지도가 그려져 있으면 레벨만 변경 */
    if (_adminMap) {
        _adminMap.setLevel(levelMap[val] || 5);
    } else {
        var lati = document.getElementById('mapLatInput');
        var lngi = document.getElementById('mapLngInput');
        var ni   = document.getElementById('mapPlaceNameInput');
        var lat  = lati ? parseFloat(lati.value) : 0;
        var lng  = lngi ? parseFloat(lngi.value) : 0;
        var name = ni ? ni.value : '';
        if (lat && lng) showStaticMap(lng, lat, name);
    }
    scheduleLive(150);
}

/* ── 주소 검색 팝업 모달 ── */
function openAddressSearch() { openMapSearchModal(); }
function closeAddressSearch() { closeMapSearchModal(); }
function doAddressSearch() { doMapSearch(); }

/* ── 지도 검색 모달 ── */
var addrModalMap = null;
var addrModalMapInitTried = false;

function openMapSearchModal() {
    var modal = document.getElementById('mapSearchModal');
    if (!modal) return;
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
    setTimeout(function() {
        var inp = document.getElementById('mapModalInput');
        if (inp) inp.focus();
        /* 저장된 좌표 있으면 모달 지도 초기화 */
        var lati = document.getElementById('mapLatInput');
        var lngi = document.getElementById('mapLngInput');
        var ni   = document.getElementById('mapPlaceNameInput');
        var lat  = lati ? parseFloat(lati.value) : 0;
        var lng  = lngi ? parseFloat(lngi.value) : 0;
        var name = ni ? ni.value : '';
        if (lat && lng && !_modalMap) {
            initModalMap(lng, lat);
            if (name) moveModalMap(lng, lat, name);
        } else if (_modalMap) {
            _modalMap.relayout();
        }
    }, 150);
}

function closeMapSearchModal() {
    var modal = document.getElementById('mapSearchModal');
    if (modal) modal.classList.remove('open');
    document.body.style.overflow = '';
}

function doMapSearch() {
    var inp = document.getElementById('mapModalInput');
    if (!inp) return;
    var q = inp.value.trim();
    if (!q) return;

    var btn = document.querySelector('.map-search-btn-submit');
    if (btn) { btn.disabled = true; btn.textContent = '검색 중...'; }

    /* 카카오 REST API 브라우저 직접 호출 */
    fetch('https://dapi.kakao.com/v2/local/search/keyword.json?query=' + encodeURIComponent(q) + '&size=15', {
        method: 'GET',
        headers: {
            'Authorization': 'KakaoAK 03a041000c72178b476cbb6e29431e81'
        }
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (btn) { btn.disabled = false; btn.textContent = '검색'; }
        var docs = data.documents || [];
        if (docs.length === 0) {
            /* 키워드 검색 결과 없으면 주소 검색 시도 */
            return fetch('https://dapi.kakao.com/v2/local/search/address.json?query=' + encodeURIComponent(q), {
                headers: { 'Authorization': 'KakaoAK 03a041000c72178b476cbb6e29431e81' }
            })
            .then(function(r) { return r.json(); })
            .then(function(d) {
                renderMapResults((d.documents || []).map(function(r) {
                    return {
                        place_name: r.address_name,
                        road_address_name: r.road_address ? r.road_address.address_name : '',
                        address_name: r.address_name,
                        x: r.x, y: r.y, phone: ''
                    };
                }));
            });
        }
        renderMapResults(docs);
    })
    .catch(function(err) {
        if (btn) { btn.disabled = false; btn.textContent = '검색'; }
        console.error('검색 오류:', err);
        var list = document.getElementById('mapSearchResultList');
        if (list) list.innerHTML =
            '<div class="map-result-item"><div class="map-result-info">' +
            '<div class="map-result-place" style="color:#c4748a;">⚠ 검색 오류</div>' +
            '<div class="map-result-addr">네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</div>' +
            '</div></div>';
    });
}


/* 모달 지도 — 카카오 JS SDK로 그리기 (마커 이동 가능) */
var _modalMap = null;
var _modalMarkers = [];

function initModalMap(lng, lat) {
    var container = document.getElementById('mapSearchModalMap');
    if (!container) return;

    if (!_modalMap) {
        if (!window._kakaoReady || typeof kakao === 'undefined' || !kakao.maps) {
            /* SDK 미로드 — 나중에 다시 시도 */
            var tries = 0;
            var poll = setInterval(function() {
                tries++;
                if (window._kakaoReady) {
                    clearInterval(poll);
                    initModalMap(lng, lat);
                } else if (tries > 20) {
                    clearInterval(poll);
                    /* 완전 폴백: iframe */
                    container.innerHTML =
                        '<iframe src="https://map.kakao.com/link/map/지도,' + lat + ',' + lng +
                        '" style="width:100%;height:100%;border:none;"></iframe>';
                }
            }, 300);
            return;
        }
        container.innerHTML = '';
        _modalMap = new kakao.maps.Map(container, {
            center: new kakao.maps.LatLng(parseFloat(lat), parseFloat(lng)),
            level: 5
        });
    }

    moveModalMap(lng, lat, null);
}

function moveModalMap(lng, lat, name) {
    if (!_modalMap || !lat || !lng) return;
    var pos = new kakao.maps.LatLng(parseFloat(lat), parseFloat(lng));
    /* 기존 마커 제거 */
    _modalMarkers.forEach(function(m) { m.setMap(null); });
    _modalMarkers = [];
    /* 새 마커 추가 */
    var marker = new kakao.maps.Marker({ map: _modalMap, position: pos });
    _modalMarkers.push(marker);
    _modalMap.setCenter(pos);
    if (name) {
        var info = new kakao.maps.InfoWindow({
            content: '<div style="padding:4px 8px;font-size:12px;white-space:nowrap;">' + name + '</div>'
        });
        info.open(_modalMap, marker);
    }
}

function updateModalMap(lng, lat, name) {
    if (_modalMap) {
        moveModalMap(lng, lat, name);
    } else {
        initModalMap(lng, lat);
    }
}

/* 편집기 인라인 지도 — 카카오맵 JS SDK */
var _adminMap = null;
var _adminMarker = null;

function showStaticMap(lng, lat, name) {
    var mapDiv = document.getElementById('adminMap');
    var ph     = document.getElementById('adminMapPlaceholder');
    if (!lat || !lng) return;
    lat = parseFloat(lat); lng = parseFloat(lng);

    var zoomInput = document.getElementById('mapZoomInput');
    var zoomVal   = (zoomInput && zoomInput.value) ? zoomInput.value : (currentZoomVal || '50M');
    var levelMap  = {'20M':3,'30M':4,'50M':5,'100M':6,'250M':7,'500M':8};
    var level     = levelMap[zoomVal] || 5;

    if (mapDiv) mapDiv.style.display = 'block';
    if (ph)     ph.style.display     = 'none';

    /* SDK 준비 여부 확인 */
    if (window._kakaoReady && typeof kakao !== 'undefined' && kakao.maps) {
        /* display:block 적용 후 크기 확정되도록 rAF 대기 */
        requestAnimationFrame(function() {
            _drawAdminMap(mapDiv, lat, lng, level, name);
        });
    } else {
        if (!window._kakaoCallbacks) window._kakaoCallbacks = [];
        window._kakaoCallbacks.push(function() {
            requestAnimationFrame(function() {
                _drawAdminMap(mapDiv, lat, lng, level, name);
            });
        });
    }
}

function _drawAdminMap(container, lat, lng, level, name) {
    var pos = new kakao.maps.LatLng(lat, lng);
    if (_adminMap) {
        _adminMap.relayout();
        _adminMap.setCenter(pos);
        _adminMap.setLevel(level);
        if (_adminMarker) _adminMarker.setPosition(pos);
        return;
    }
    /* container가 실제 크기를 가질 때까지 확인 */
    if (!container.offsetWidth || !container.offsetHeight) {
        setTimeout(function() { _drawAdminMap(container, lat, lng, level, name); }, 50);
        return;
    }
    _adminMap = new kakao.maps.Map(container, {
        center: pos,
        level: level,
        draggable: false,
        scrollwheel: false,
        disableDoubleClick: true,
        disableDoubleClickZoom: true
    });
    /* 생성 직후 relayout으로 크기 확정 */
    setTimeout(function() {
        if (_adminMap) {
            _adminMap.relayout();
            _adminMap.setCenter(pos);
        }
    }, 100);
    _adminMarker = new kakao.maps.Marker({ map: _adminMap, position: pos });
    if (name) {
        var info = new kakao.maps.InfoWindow({
            content: '<div style="padding:4px 10px;font-size:12px;white-space:nowrap;font-family:\'Noto Sans KR\',sans-serif;">' + name + '</div>',
            removable: false
        });
        info.open(_adminMap, _adminMarker);
    }
}

/* Daum 우편번호 서비스로 폴백 검색 (인증 불필요) */
function searchWithDaumPostcode(q, btn) {
    if (btn) { btn.disabled = false; btn.textContent = '검색'; }
    /* Daum 우편번호 팝업 열기 */
    if (typeof daum === 'undefined' || !daum.Postcode) {
        /* Daum 스크립트 동적 로드 */
        var s = document.createElement('script');
        s.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
        s.onload = function() { openDaumPostcode(); };
        document.head.appendChild(s);
    } else {
        openDaumPostcode();
    }
}

/* 검색 결과 전체 저장 (페이지네이션용) */
var mapSearchAllResults = [];
var mapSearchPage = 1;
var MAP_PAGE_SIZE = 5;

function renderMapResults(data) {
    mapSearchAllResults = data || [];
    mapSearchPage = 1;
    renderMapPage(mapSearchPage);
}

function renderMapPage(page) {
    var list = document.getElementById('mapSearchResultList');
    var pagination = document.getElementById('mapSearchPagination');
    if (!list) return;

    var totalPages = Math.ceil(mapSearchAllResults.length / MAP_PAGE_SIZE);
    var start = (page - 1) * MAP_PAGE_SIZE;
    var pageData = mapSearchAllResults.slice(start, start + MAP_PAGE_SIZE);

    if (!mapSearchAllResults.length) {
        list.innerHTML = '<div class="map-result-item"><div class="map-result-info"><div class="map-result-place">검색 결과가 없습니다</div><div class="map-result-addr">다른 검색어를 입력해 보세요</div></div></div>';
        if (pagination) pagination.innerHTML = '';
        return;
    }

    /* 첫 결과로 모달 지도 이동 + 마커 표시 */
    if (pageData[0] && pageData[0].x && pageData[0].y) {
        updateModalMap(pageData[0].x, pageData[0].y, pageData[0].place_name || '');
        /* 전체 결과 마커 */
        if (_modalMap && window._kakaoReady) {
            _modalMarkers.forEach(function(m){ m.setMap(null); });
            _modalMarkers = [];
            var bounds = new kakao.maps.LatLngBounds();
            pageData.forEach(function(p) {
                if (!p.x || !p.y) return;
                var pos = new kakao.maps.LatLng(parseFloat(p.y), parseFloat(p.x));
                var m = new kakao.maps.Marker({ map: _modalMap, position: pos });
                _modalMarkers.push(m);
                bounds.extend(pos);
            });
            if (_modalMarkers.length > 1) _modalMap.setBounds(bounds);
        }
    }

    list.innerHTML = '';
    pageData.forEach(function(p) {
        var road  = (p.road_address && p.road_address.address_name) || p.road_address_name || '';
        var jibun = p.address_name || '';
        var name  = p.place_name || jibun;
        var phone = p.phone ? ' (' + p.phone + ')' : '';

        var item = document.createElement('div');
        item.className = 'map-result-item';
        item.innerHTML =
            '<div class="map-result-info">' +
              '<div class="map-result-place">' + name + phone + '</div>' +
              '<div class="map-result-addr">' + (road || jibun) + '</div>' +
            '</div>' +
            '<button type="button" class="map-result-select-btn">선택</button>';

        /* 지도 업데이트 (항목 클릭) */
        item.querySelector('.map-result-info').addEventListener('click', function() {
            if (p.x && p.y) updateModalMap(p.x, p.y, p.place_name || '');
        });

        /* 선택 버튼 */
        item.querySelector('.map-result-select-btn').addEventListener('click', function() {
            selectMapPlace({
                place_name:        p.place_name || '',
                road_address_name: road,
                address_name:      jibun,
                x: p.x, y: p.y
            });
        });

        list.appendChild(item);
    });

    /* 페이지네이션 */
    if (pagination) {
        pagination.innerHTML = '';
        if (totalPages > 1) {
            for (var i = 1; i <= totalPages; i++) {
                (function(pg) {
                    var btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'map-page-btn' + (pg === page ? ' active' : '');
                    btn.textContent = pg;
                    btn.addEventListener('click', function() {
                        mapSearchPage = pg;
                        renderMapPage(pg);
                    });
                    pagination.appendChild(btn);
                })(i);
            }
        }
    }
}

function selectMapPlace(p) {
    var addr = p.road_address_name || p.address_name;
    var si = document.getElementById('mapSearchInput');
    var sri = document.getElementById('mapAddressRoadInput');
    var ai = document.getElementById('mapAddressInput');
    var lati = document.getElementById('mapLatInput');
    var lngi = document.getElementById('mapLngInput');
    var ni = document.getElementById('mapPlaceNameInput');
    if (si) si.value = addr;
    if (sri) sri.value = addr;
    if (ai) ai.value = p.address_name;
    if (lati) lati.value = p.y;
    if (lngi) lngi.value = p.x;
    if (ni && p.place_name) ni.value = p.place_name;

    /* 지도 표시 */
    var lat = parseFloat(p.y), lng = parseFloat(p.x);
    if (lat && lng) {
        showStaticMap(lng, lat, p.place_name || addr);
    }
    closeMapSearchModal();
    scheduleLive(200);
}

/* 모달 배경 클릭 시 닫기 */
(function() {
    var modal = document.getElementById('mapSearchModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) closeMapSearchModal();
        });
    }
    /* ESC 닫기 */
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeMapSearchModal();
    });
})();
window.addEventListener('load', function() {
    var addr = document.getElementById('mapSearchInput');
    /* 카카오 SDK가 준비됐을 때 인라인 지도 초기화 */
    function tryInitMap() {
        var lati = document.getElementById('mapLatInput');
        var lngi = document.getElementById('mapLngInput');
        var ni   = document.getElementById('mapPlaceNameInput');
        var lat  = lati ? parseFloat(lati.value) : 0;
        var lng  = lngi ? parseFloat(lngi.value) : 0;
        var name = ni ? ni.value : '';
        if (lat && lng) {
            showStaticMap(lng, lat, name);
        }
    }
    tryInitMap();
    /* 초기 저장된 스타일 복원 */
    if (WEDDING.calStyle) {
        var calEl = document.querySelector('.cal-style-card[data-cal="'+WEDDING.calStyle+'"]');
        if (calEl) pickCalStyle(calEl, WEDDING.calStyle);
    }
    if (WEDDING.ddayStyle) {
        var ddayEl = document.querySelector('.style-type-item[data-dday="'+WEDDING.ddayStyle+'"]');
        if (ddayEl) pickDdayStyle(ddayEl, WEDDING.ddayStyle);
    }
    /* 저장된 표시 순서 복원 */
    if (WEDDING.displayOrder && WEDDING.displayOrder === 'bride') {
        var orderBrideBtn = document.querySelector('#orderTabs [data-val="bride"]');
        if (orderBrideBtn) pickTab(orderBrideBtn, 'orderTabs', 'orderVal');
    }

    /* 저장된 메인 디자인 복원 — 저장된 글자색이 있으면 테마 기본색으로 덮어쓰지 않는다 */
    var savedDesign = WEDDING.mainDesign || 'basic';
    var savedColorEl = document.getElementById('fontColorHex');
    var hasSavedColor = !!(savedColorEl && savedColorEl.value.trim());

    var designEl = document.querySelector('.ed-design-card[data-design="'+savedDesign+'"]');
    if (designEl) pickDesign(designEl, savedDesign, { preserveColor: hasSavedColor });
    /* 아직 색상을 고른 적이 없으면 해당 테마의 기본색으로 시작 */
    if (!hasSavedColor) applyDesignDefaultColor(savedDesign);
    if (WEDDING.hasPhoto) {
        var hint = document.getElementById('mainUploadHint');
        if (hint) hint.style.display = 'none';
        var thumbWrap = document.getElementById('mainPhotoThumb');
        if (thumbWrap) thumbWrap.style.display = 'block';
        /* /api/admin/photo는 청첩장별로 스코프되지 않으므로 이미 폼에 있는 base64 값을 사용 */
        var b64Input = document.getElementById('mainPhotoBase64');
        var photoDataUrl = (b64Input && b64Input.value) ? 'data:image/jpeg;base64,' + b64Input.value : '/api/admin/photo';
        var thumb = document.getElementById('mainThumbImg');
        if (thumb) thumb.src = photoDataUrl;
        var dcPh  = document.getElementById('dcPlaceholder');
        var dcImg = document.getElementById('dcImg');
        if (dcPh)  dcPh.style.display = 'none';
        if (dcImg) { dcImg.style.display = 'block'; dcImg.src = photoDataUrl; }
    }

    /* 토글 상태에 따라 섹션 제목 색상 + 즉시 반영 */
    /* 섹션 헤더의 토글만 대상 — 슬라이드 메뉴 미러 토글은 buildNavPanel()이 따로 배선한다 */
    document.querySelectorAll('.ed-sec-hd .ed-toggle-wrap input').forEach(function(chk) {
        function syncTitle() {
            /* 슬라이드 메뉴의 미러 토글은 .ed-sec-hd 밖에 있으므로 제목 동기화 대상이 아니다 */
            var hd = chk.closest('.ed-sec-hd');
            if (!hd) return;
            var title = hd.querySelector('.ed-sec-title');
            if (title) title.style.color = chk.checked ? '#2c2822' : '#bbb';
        }
        chk.addEventListener('change', function() {
            syncTitle();
            /* 토글 즉시 미리보기 반영 + 즉시 자동저장 */
            sendLive();
            clearTimeout(saveTimer);
            saveTimer = setTimeout(autoSave, 400); /* 토글은 0.4초 후 빠른 저장 */
        });
        syncTitle();
    });

    /* 저장된 갤러리 이미지를 galImages 배열/썸네일 목록에 복원.
       안 하면 화면엔 빈 목록으로 보이다가, 여기서 사진을 한 장이라도 추가·삭제하는 순간
       빈 배열 기준으로 히든 입력을 덮어써서 기존에 저장돼 있던 사진이 전부 사라진다. */
    var savedGalImages = document.getElementById('galleryImagesInput').value;
    if (savedGalImages) {
        galImages = savedGalImages.split('|||').filter(function(s) { return s.trim(); });
        renderGalThumbs();
    }

    /* 탭/카드형 픽커들의 활성 표시를 방금 복원된 히든 입력값에 맞춰 동기화
       (되돌리기 버튼에서만 호출되던 것을 최초 로드 시에도 실행 — 안 하면 저장된 값과 무관하게
       템플릿에 하드코딩된 기본 탭이 항상 활성으로 보임: 지도 잠금/자세히보기가 대표적 사례) */
    resyncPickerVisuals();

    /* 여기까지가 서버 값 복원 — 이제 스냅샷을 떠 두고 이후의 사용자 편집만 되돌리기 대상으로 삼는다 */
    captureSectionSnapshot();
});
