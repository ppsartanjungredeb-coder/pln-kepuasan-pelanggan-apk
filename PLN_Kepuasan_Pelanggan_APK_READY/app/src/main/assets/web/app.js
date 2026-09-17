(() => {
  const cfg = window.PLN_SURVEY_CONFIG || {};
  const buttons = [...document.querySelectorAll('.rating-btn')];
  const overlay = document.getElementById('thanksOverlay');
  const status = document.getElementById('status');
  const statusText = document.getElementById('statusText');
  const QUEUE_KEY = 'pln_survey_pending_v1';
  // Fullscreen browser / kiosk mode. Browser requires a user gesture.
  const fullscreenBtn = document.getElementById('fullscreenBtn');
  async function toggleFullscreen(){
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();
      } else {
        await document.exitFullscreen();
      }
    } catch (e) {
      console.warn('Fullscreen tidak dapat diaktifkan:', e);
    }
  }
  if (fullscreenBtn) {
    fullscreenBtn.addEventListener('click', toggleFullscreen);
    document.addEventListener('fullscreenchange', () => {
      fullscreenBtn.textContent = document.fullscreenElement ? '×' : '⛶';
      fullscreenBtn.title = document.fullscreenElement ? 'Keluar fullscreen' : 'Fullscreen';
    });
  }

  const DEMO_KEY = 'pln_survey_demo_v1';
  let locked = false;

  const uuid = () => {
    if (crypto?.randomUUID) return crypto.randomUUID();
    return 'id-' + Date.now() + '-' + Math.random().toString(16).slice(2);
  };

  const getQueue = () => {
    try { return JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]'); }
    catch { return []; }
  };
  const setQueue = q => localStorage.setItem(QUEUE_KEY, JSON.stringify(q));

  function setStatus() {
    status.classList.remove('offline','demo');
    if (cfg.DEMO_MODE) {
      status.classList.add('demo');
      statusText.textContent = 'Demo • data tersimpan lokal';
    } else if (!navigator.onLine) {
      status.classList.add('offline');
      statusText.textContent = 'Offline • akan disinkronkan otomatis';
    } else {
      statusText.textContent = 'Online';
    }
  }

  function payloadFrom(btn) {
    return {
      rating: btn.dataset.rating,
      category: btn.dataset.category,
      score: Number(btn.dataset.score),
      device: cfg.DEVICE_ID || 'TABLET-01',
      location: cfg.LOCATION || 'Kantor PLN ULP Tanjung Redeb',
      counter: cfg.COUNTER_NAME || 'Ruang Pelayanan',
      syncId: uuid(),
      clientTimestamp: new Date().toISOString()
    };
  }

  async function sendToAppsScript(payload) {
    if (!cfg.GAS_URL || cfg.GAS_URL.includes('PASTE_')) throw new Error('GAS_URL belum diisi');
    await fetch(cfg.GAS_URL, {
      method: 'POST',
      mode: 'no-cors',
      cache: 'no-store',
      headers: {'Content-Type': 'text/plain;charset=utf-8'},
      body: JSON.stringify(payload)
    });
  }

  function storeDemo(payload) {
    const rows = JSON.parse(localStorage.getItem(DEMO_KEY) || '[]');
    rows.push({...payload, savedAt: new Date().toISOString()});
    localStorage.setItem(DEMO_KEY, JSON.stringify(rows));
  }

  async function save(payload) {
    if (cfg.DEMO_MODE) {
      storeDemo(payload);
      return;
    }
    if (!navigator.onLine) {
      const q = getQueue(); q.push(payload); setQueue(q); return;
    }
    try {
      await sendToAppsScript(payload);
    } catch (e) {
      const q = getQueue(); q.push(payload); setQueue(q);
    }
  }

  async function flushQueue() {
    if (cfg.DEMO_MODE || !navigator.onLine) return;
    const q = getQueue();
    if (!q.length) return;
    const remaining = [];
    for (const item of q) {
      try { await sendToAppsScript(item); }
      catch { remaining.push(item); }
    }
    setQueue(remaining);
  }

  function resetUI() {
    overlay.classList.remove('show');
    overlay.setAttribute('aria-hidden','true');
    buttons.forEach(b => { b.disabled = false; b.classList.remove('pulse'); });
    locked = false;
  }

  buttons.forEach(btn => btn.addEventListener('click', async () => {
    if (locked) return;
    locked = true;
    buttons.forEach(b => b.disabled = true);
    btn.classList.add('pulse');
    const payload = payloadFrom(btn);
    await save(payload);
    overlay.classList.add('show');
    overlay.setAttribute('aria-hidden','false');
    setTimeout(resetUI, cfg.RESET_DELAY_MS || 2600);
  }));

  window.addEventListener('online', () => { setStatus(); flushQueue(); });
  window.addEventListener('offline', setStatus);
  setStatus();
  flushQueue();

  // Shortcut admin untuk demo: Ctrl+Shift+D mengekspor data demo sebagai CSV.
  window.addEventListener('keydown', e => {
    if (!(e.ctrlKey && e.shiftKey && e.key.toLowerCase() === 'd')) return;
    const rows = JSON.parse(localStorage.getItem(DEMO_KEY) || '[]');
    if (!rows.length) return alert('Belum ada data demo.');
    const cols = ['savedAt','rating','category','score','device','location','counter','syncId'];
    const csv = [cols.join(','), ...rows.map(r => cols.map(c => JSON.stringify(r[c] ?? '')).join(','))].join('\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], {type:'text/csv'}));
    a.download = 'demo-kepuasan-pln.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  });
})();
