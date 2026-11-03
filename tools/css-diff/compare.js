const { chromium } = require('playwright');
const fs = require('fs');
const [,, tplFile, oldCss, newCss, mode] = process.argv;
const tpl = fs.readFileSync(tplFile, 'utf8');
function page(css) {
  const other = mode === 'inv' ? '__invitation__' : '__editor__';
  return tpl.replace('__tokens__', 'file://' + process.cwd() + '/tokens.css')
            .replace(other, 'file://' + process.cwd() + '/' + css)
            .replace('</head>', '<style>*,*::before,*::after{animation-duration:0s!important;animation-delay:0s!important;transition:none!important}</style></head>');
}
const configs = [];
if (mode === 'inv') {
  for (const d of ['basic','our_story','our_story_pink','married','forever','the_marriage'])
    for (const fsz of ['fs-sm','fs-md','fs-lg'])
      for (const live of [false,true]) configs.push({d,fsz,live});
} else configs.push({});
(async () => {
  const browser = await chromium.launch();
  let diffs = 0; const samples = [];
  for (const vw of [360, 430, 1500]) {
    if (mode === 'inv' && vw === 1500) continue;
    if (mode === 'ed' && vw !== 1500 && vw !== 430) continue;
    const results = {};
    for (const css of [oldCss, newCss]) {
      fs.writeFileSync('/tmp/p.html', page(css));
      const p = await browser.newPage({ viewport: { width: vw, height: 900 } });
      await p.goto('file:///tmp/p.html');
      const all = [];
      for (const c of configs) {
        const snap = await p.evaluate((c) => {
          const hero = document.getElementById('heroSection');
          if (hero && c.d) { hero.className = 'hero design-' + c.d + ' ' + c.fsz; document.querySelector('.wc').className = 'wc' + (c.live ? ' is-live no-date' : ''); }
          // also toggle "on" reveal states
          document.querySelectorAll('.reveal,.reveal-scale,.reveal-left,.reveal-right,.section-divider,.hosts-wrap').forEach(e => e.classList.add('on'));
          const out = [];
          const els = document.querySelectorAll('body *');
          els.forEach((el, i) => {
            for (const pe of [null, '::before', '::after']) {
              const cs = getComputedStyle(el, pe);
              const names = []; for (let k = 0; k < cs.length; k++) names.push(cs[k]); names.sort(); for (let q = names.length - 1; q >= 0; q--) if (names[q].startsWith('--card')) names.splice(q, 1);
              let s = ''; for (const n of names) s += n + ':' + cs.getPropertyValue(n) + '\u0001';
              out.push([i + (pe || ''), el.tagName + '.' + el.className, s]);
            }
          });
          return out;
        }, c);
        all.push(snap);
      }
      results[css] = all;
      await p.close();
    }
    const A = results[oldCss], B = results[newCss];
    for (let ci = 0; ci < A.length; ci++) {
      for (let k = 0; k < A[ci].length; k++) {
        if (A[ci][k][2] !== B[ci][k][2]) {
          diffs++;
          if (samples.length < 15) {
            const a = A[ci][k][2].split('\u0001'), b = B[ci][k][2].split('\u0001');
            const pa = Object.fromEntries(a.map(x => [x.split(':')[0], x.slice(x.indexOf(':')+1)]));
            const pb = Object.fromEntries(b.map(x => [x.split(':')[0], x.slice(x.indexOf(':')+1)]));
            const changed = Object.keys({...pa,...pb}).filter(n => pa[n] !== pb[n]).map(n => `${n}: ${pa[n]} -> ${pb[n]}`);
            samples.push({ vw, cfg: JSON.stringify(configs[ci]||{}), el: A[ci][k][0] + ' ' + A[ci][k][1].slice(0,60), changed: changed.slice(0,4) });
          }
        }
      }
    }
  }
  console.log('DIFFS', diffs);
  console.log(JSON.stringify(samples, null, 1));
  await browser.close();
})();
