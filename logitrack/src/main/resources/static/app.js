/* ======================================================
   LogiTrack — app.js
   SPA Frontend: Auth + Dashboard + CRUD modules
   ====================================================== */

const API_BASE = 'http://localhost:8080';
let jwtToken = null;
let currentUser = null;
let currentRole = null;
let deleteCallback = null;
let bodegasCache = [];
let ordenesCache = [];

/* ─────────────────────────────────────────────────────
   AUTH HELPERS — Token JWT solo en sessionStorage
   ───────────────────────────────────────────────────── */

function saveSession(token, user, rol) {
  jwtToken = token;
  currentUser = user;
  if (rol) currentRole = rol;
  sessionStorage.setItem('logitrack_token', token);
  sessionStorage.setItem('logitrack_user', user);
  if (rol) sessionStorage.setItem('logitrack_rol', rol);
  try {
    localStorage.removeItem('logitrack_token');
    localStorage.removeItem('token');
    localStorage.removeItem('jwt');
  } catch (_) { /* ignore */ }
}

function clearSession() {
  jwtToken = null;
  currentUser = null;
  currentRole = null;
  sessionStorage.removeItem('logitrack_token');
  sessionStorage.removeItem('logitrack_user');
  sessionStorage.removeItem('logitrack_rol');
}

function isAdmin() {
  return currentRole === 'ADMIN';
}

function forceLogout() {
  clearSession();
  document.getElementById('app').classList.add('hidden');
  document.getElementById('login-screen').classList.remove('hidden');
  document.getElementById('login-form').reset();
  const loginError = document.getElementById('login-error');
  if (loginError) loginError.classList.add('hidden');
  showToast('Sesión expirada. Por favor inicia sesión nuevamente.', 'error');
}

/* ─────────────────────────────────────────────────────
   UTILS
   ───────────────────────────────────────────────────── */

function getHeaders() {
  const h = { 'Content-Type': 'application/json' };
  if (jwtToken) h['Authorization'] = `Bearer ${jwtToken}`;
  return h;
}

async function apiFetch(path, options = {}) {
  const url = API_BASE + path;
  const config = {
    headers: getHeaders(),
    ...options,
  };
  try {
    const res = await fetch(url, config);
    if (res.status === 204) return null;

    if (res.status === 401 && jwtToken) {
      forceLogout();
      throw new Error('Sesión expirada.');
    }

    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = { message: text || `Error ${res.status}` };
    }
    if (!res.ok) {
      const msg = data?.message || data?.error || `Error ${res.status}`;
      throw new Error(msg);
    }
    return data;
  } catch (e) {
    if (e instanceof TypeError) {
      setApiStatus(false);
      throw new Error('No se pudo conectar con el servidor. Verifica que el backend esté corriendo.');
    }
    throw e;
  }
}

async function apiFetchOptional(path) {
  const res = await fetch(API_BASE + path, { headers: getHeaders() });
  if (res.status === 401 && jwtToken) {
    forceLogout();
    throw new Error('Sesión expirada.');
  }
  if (res.status === 404) return null;
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(data?.message || data?.error || `Error ${res.status}`);
  return data;
}

async function apiFetchBlob(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    ...options,
    headers: { ...(options.headers || {}), Authorization: jwtToken ? `Bearer ${jwtToken}` : '' },
  });
  if (res.status === 401 && jwtToken) {
    forceLogout();
    throw new Error('Sesión expirada.');
  }
  if (!res.ok) {
    let msg = `Error ${res.status}`;
    try {
      const data = JSON.parse(await res.text());
      msg = data?.message || data?.error || msg;
    } catch (_) { /* ignore */ }
    throw new Error(msg);
  }
  return res.blob();
}

function setApiStatus(ok) {
  const el = document.getElementById('api-status');
  if (!el) return;
  const dot = el.querySelector('.status-dot');
  const txt = el.querySelector('.status-text');
  if (ok) {
    el.classList.remove('error');
    txt.textContent = 'Conectado';
  } else {
    el.classList.add('error');
    txt.textContent = 'Sin conexión';
  }
}

function formatDate(isoStr) {
  if (!isoStr) return '—';
  try {
    const d = new Date(isoStr);
    return d.toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch { return isoStr; }
}

function formatCurrency(n) {
  if (n == null) return '—';
  return '$' + Number(n).toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function escapeHtml(str) {
  if (str == null) return '—';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/* ─────────────────────────────────────────────────────
   TOAST
   ───────────────────────────────────────────────────── */
function showToast(message, type = 'default') {
  const container = document.getElementById('toast-container');
  const icons = {
    success: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>`,
    error: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`,
    info: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`,
    default: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/></svg>`,
  };
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span class="toast-icon">${icons[type] || icons.default}</span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.animation = 'toastOut 0.3s ease forwards';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

/* ─────────────────────────────────────────────────────
   MODAL HELPERS
   ───────────────────────────────────────────────────── */
function openModal(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}
function closeModal(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.add('hidden');
  document.body.style.overflow = '';
}

document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.classList.add('hidden');
    document.body.style.overflow = '';
  }
});

document.querySelectorAll('[data-modal]').forEach(btn => {
  btn.addEventListener('click', () => closeModal(btn.dataset.modal));
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-backdrop:not(.hidden)').forEach(m => {
      m.classList.add('hidden');
      document.body.style.overflow = '';
    });
  }
});

/* ─────────────────────────────────────────────────────
   SIDEBAR / NAVIGATION
   ───────────────────────────────────────────────────── */
const pages = ['dashboard', 'ordenes', 'bodegas', 'productos', 'movimientos', 'reportes', 'auditoria', 'usuarios'];
const pageTitles = {
  dashboard: ['Dashboard IQ', 'Inicio / Indicadores'],
  ordenes: ['Órdenes de compra', 'Operación / Órdenes'],
  bodegas: ['Bodegas', 'Inventario / Bodegas'],
  productos: ['Productos', 'Inventario / Productos'],
  movimientos: ['Movimientos', 'Inventario / Movimientos'],
  reportes: ['Reportes', 'Inventario / Reportes'],
  auditoria: ['Auditoría', 'Sistema / Auditoría'],
  usuarios: ['Usuarios', 'Sistema / Usuarios'],
};

function navigateTo(page) {
  pages.forEach(p => {
    document.getElementById(`page-${p}`)?.classList.remove('active');
    document.getElementById(`nav-${p}`)?.classList.remove('active');
  });
  const el = document.getElementById(`page-${page}`);
  const nav = document.getElementById(`nav-${page}`);
  if (el) el.classList.add('active');
  if (nav) nav.classList.add('active');
  const [title, breadcrumb] = pageTitles[page] || [page, page];
  document.getElementById('page-title').textContent = title;
  document.getElementById('page-breadcrumb').textContent = breadcrumb;
  closeSidebar();
  loadPage(page);
}

function loadPage(page) {
  switch (page) {
    case 'dashboard': loadDashboard(); break;
    case 'ordenes': loadOrdenes(); break;
    case 'bodegas': loadBodegas(); break;
    case 'productos': loadProductos(); break;
    case 'movimientos': loadMovimientos(); break;
    case 'reportes': initReportes(); break;
    case 'auditoria': loadAuditoria(); break;
    case 'usuarios': loadUsuarios(); break;
  }
}

document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', (e) => {
    e.preventDefault();
    navigateTo(item.dataset.page);
  });
});

const sidebar = document.getElementById('sidebar');
const overlay = document.getElementById('sidebar-overlay');
document.getElementById('menu-toggle')?.addEventListener('click', () => {
  sidebar.classList.add('open');
  overlay.classList.add('active');
});
function closeSidebar() {
  sidebar?.classList.remove('open');
  overlay?.classList.remove('active');
}
document.getElementById('sidebar-close')?.addEventListener('click', closeSidebar);
overlay?.addEventListener('click', closeSidebar);

/* ─────────────────────────────────────────────────────
   LOGIN
   ───────────────────────────────────────────────────── */
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');

loginForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;
  if (!username || !password) {
    showLoginError('Completa todos los campos.');
    return;
  }
  setLoginLoading(true);
  loginError.classList.add('hidden');
  try {
    const data = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    const token = data?.token || data?.accessToken || data?.jwt || data;
    if (!token || typeof token !== 'string') throw new Error('Respuesta de login inválida.');
    saveSession(token, username);
    await resolveCurrentRole();
    setApiStatus(true);
    enterApp();
  } catch (err) {
    showLoginError(err.message || 'Credenciales incorrectas.');
  } finally {
    setLoginLoading(false);
  }
});

function showLoginError(msg) {
  if (!loginError) return;
  loginError.textContent = msg;
  loginError.classList.remove('hidden');
}
function setLoginLoading(loading) {
  const btn = document.getElementById('login-btn');
  if (!btn) return;
  btn.querySelector('.btn-text')?.classList.toggle('hidden', loading);
  btn.querySelector('.btn-spinner')?.classList.toggle('hidden', !loading);
  btn.disabled = loading;
}

document.getElementById('toggle-pwd')?.addEventListener('click', () => {
  const inp = document.getElementById('login-password');
  if (inp) inp.type = inp.type === 'password' ? 'text' : 'password';
});

function enterApp() {
  document.getElementById('login-screen')?.classList.add('hidden');
  document.getElementById('app')?.classList.remove('hidden');
  const initials = (currentUser || 'A').charAt(0).toUpperCase();
  const avatar = document.getElementById('user-avatar');
  if (avatar) avatar.textContent = initials;
  const userLabel = document.getElementById('sidebar-username');
  if (userLabel) userLabel.textContent = currentUser || 'Usuario';
  applyRoleUi();
  navigateTo('dashboard');
}

async function resolveCurrentRole() {
  currentRole = sessionStorage.getItem('logitrack_rol');
  try {
    const usuarios = await apiFetch('/api/usuarios');
    const me = (usuarios || []).find((u) => u.username === currentUser);
    if (me?.rol) {
      currentRole = me.rol;
      sessionStorage.setItem('logitrack_rol', me.rol);
    }
  } catch (_) {
    /* AGENTE/ADMIN: si no hay listado, se conserva el rol ya guardado */
  }
  applyRoleUi();
}

function applyRoleUi() {
  const roleEl = document.getElementById('sidebar-role');
  if (roleEl) roleEl.textContent = currentRole || '—';
  document.querySelectorAll('.admin-only').forEach((el) => {
    el.classList.toggle('hidden', !isAdmin());
  });
}

document.getElementById('logout-btn')?.addEventListener('click', () => {
  clearSession();
  document.getElementById('app')?.classList.add('hidden');
  document.getElementById('login-screen')?.classList.remove('hidden');
  document.getElementById('login-form')?.reset();
  if (loginError) loginError.classList.add('hidden');
  showToast('Sesión cerrada correctamente.', 'info');
});

/* ─────────────────────────────────────────────────────
   DASHBOARD IQ
   ───────────────────────────────────────────────────── */
async function loadDashboard() {
  try {
    const [kpisR, resumenR, riesgoR, ordenesR, bodegasR] = await Promise.allSettled([
      apiFetch('/api/kpis'),
      apiFetchOptional('/api/panel/resumen'),
      apiFetch('/api/productos/riesgo'),
      apiFetch('/api/ordenes?estado=BORRADOR'),
      apiFetch('/api/bodegas'),
    ]);
    if (bodegasR.status === 'fulfilled') bodegasCache = bodegasR.value || [];
    if (kpisR.status === 'fulfilled') renderKpis(kpisR.value);
    else showToast(kpisR.reason?.message || 'No se pudieron cargar los KPIs', 'error');
    renderPanelResumen(resumenR.status === 'fulfilled' ? resumenR.value : null);
    renderRiesgo(riesgoR.status === 'fulfilled' ? riesgoR.value || [] : []);
    const borradores = ordenesR.status === 'fulfilled' ? ordenesR.value || [] : [];
    renderOrdenesTabla('tbody-ordenes-borrador', borradores, true);
    setApiStatus(true);
  } catch (e) {
    setApiStatus(false);
    showToast(e.message, 'error');
  }
}

function renderKpis(kpis) {
  if (!kpis) return;
  const ocupacion = kpis.ocupacionPorBodega || [];
  const maxPct = ocupacion.reduce((m, b) => Math.max(m, Number(b.porcentaje) || 0), 0);
  const ocupacionEl = document.getElementById('kpi-ocupacion');
  if (ocupacionEl) ocupacionEl.textContent = ocupacion.length ? `${maxPct.toFixed(1)}%` : '—';
  animateCount('kpi-quiebre', kpis.productosEnQuiebre ?? '—');
  animateCount('kpi-riesgo', kpis.productosEnRiesgo ?? '—');
  const cant = kpis.ordenesPorAprobar?.cantidad ?? 0;
  animateCount('kpi-ordenes-cant', cant);
  const montoEl = document.getElementById('kpi-ordenes-monto');
  if (montoEl) montoEl.textContent = 'Monto total ' + formatCurrency(kpis.ordenesPorAprobar?.montoTotal);
  const calc = document.getElementById('kpi-calculado');
  if (calc) calc.textContent = 'Calculado en ' + formatDate(kpis.calculadoEn) + ' · America/Bogota';

  const list = document.getElementById('kpi-ocupacion-list');
  if (list) {
    if (!ocupacion.length) {
      list.innerHTML = '<p class="empty-hint">Sin bodegas</p>';
    } else {
      list.innerHTML = ocupacion.map((b) => {
        const pct = Number(b.porcentaje) || 0;
        const crit = pct >= 90;
        return `<div class="ocupacion-row">
          <div class="ocupacion-head"><strong>${escapeHtml(b.nombre)}</strong><span>${pct.toFixed(1)}%</span></div>
          <div class="ocupacion-bar"><span class="${crit ? 'critica' : ''}" style="width:${Math.min(pct, 100)}%"></span></div>
        </div>`;
      }).join('');
    }
  }
  const ayer = kpis.movimientosAyer || {};
  setText('ayer-entrada', ayer.entrada ?? 0);
  setText('ayer-salida', ayer.salida ?? 0);
  setText('ayer-transferencia', ayer.transferencia ?? 0);
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function renderPanelResumen(resumen) {
  const body = document.getElementById('panel-resumen-body');
  const fechaEl = document.getElementById('panel-fecha');
  if (!body) return;
  if (!resumen) {
    if (fechaEl) fechaEl.textContent = 'Sin publicar';
    body.innerHTML = '<p class="empty-hint">Aún no hay un resumen válido del panel.</p>';
    return;
  }
  if (fechaEl) fechaEl.textContent = resumen.fecha || '—';
  const alertas = resumen.alertas || [];
  const acciones = resumen.accionesSugeridas || [];
  body.innerHTML = `
    <p class="narrativa">${escapeHtml(resumen.narrativa)}</p>
    <h4 class="iq-subtitle">Alertas</h4>
    ${alertas.length ? alertas.map((a) => `
      <article class="alerta-card sev-${escapeHtml(a.severidad)}">
        <span class="badge badge-sev">${escapeHtml(a.severidad)}</span>
        <strong>${escapeHtml(a.titulo)}</strong>
        <p>${escapeHtml(a.detalle)}</p>
        <small>producto ${a.productoId ?? '—'} · orden ${a.ordenId ?? '—'} · bodega ${a.bodegaId ?? '—'}</small>
      </article>`).join('') : '<p class="empty-hint">Sin alertas</p>'}
    <h4 class="iq-subtitle">Acciones sugeridas</h4>
    ${acciones.length ? `<ul class="acciones-list">${acciones.map((ac) => `
      <li><span class="badge">${escapeHtml(ac.tipo)}</span> ${escapeHtml(ac.descripcion)}</li>`).join('')}</ul>` : '<p class="empty-hint">Sin acciones</p>'}
  `;
}

function nombreBodega(id) {
  const b = bodegasCache.find((x) => x.id === id);
  return b ? b.nombre : (id != null ? `#${id}` : '—');
}

function renderRiesgo(filas) {
  const tbody = document.getElementById('tbody-riesgo');
  if (!tbody) return;
  if (!filas.length) {
    tbody.innerHTML = '<tr><td colspan="7"><div class="table-loading">No hay productos en riesgo</div></td></tr>';
    return;
  }
  tbody.innerHTML = filas.map((f) => `
    <tr>
      <td>${escapeHtml(f.nombreProducto)} <small class="muted">#${f.productoId}</small></td>
      <td>${f.stockTotal}</td>
      <td>${Number(f.consumoDiarioPromedio).toFixed(2)}</td>
      <td>${Number(f.puntoReorden).toFixed(2)}</td>
      <td>${f.diasCobertura == null ? '—' : Number(f.diasCobertura).toFixed(1)}</td>
      <td><span class="badge">${escapeHtml(f.estadoCobertura)}</span></td>
      <td>${escapeHtml(nombreBodega(f.bodegaDestinoId))}</td>
    </tr>`).join('');
}

function etiquetaOrden(o) {
  return {
    id: o.id,
    estado: o.estado,
    producto: o.producto?.nombre || o.productoId || '—',
    proveedor: o.proveedor?.nombre || '—',
    cantidad: o.cantidad,
    total: o.total,
    bodega: o.bodegaDestino?.nombre || '—',
    fecha: o.fechaCreacion,
    tienePdf: Boolean(o.fechaGeneracionPdf),
  };
}

function botonesPdf(o) {
  return `<div class="actions-cell">
    <button class="btn btn-outline btn-sm" onclick="generarPdfOrden(${o.id})">Generar PDF</button>
    <button class="btn btn-ghost btn-sm" onclick="verPdfOrden(${o.id})">Ver</button>
  </div>`;
}

function botonesEstado(o) {
  if (!isAdmin()) return '<span class="muted">—</span>';
  const parts = [];
  if (o.estado === 'BORRADOR') {
    parts.push(`<button class="btn btn-primary btn-sm btn-aprobar" onclick="cambiarEstadoOrden(${o.id}, 'APROBADA')">Aprobar</button>`);
  }
  if (o.estado === 'APROBADA') {
    parts.push(`<button class="btn btn-outline btn-sm" onclick="cambiarEstadoOrden(${o.id}, 'RECIBIDA')">Recibir</button>`);
  }
  if (o.estado === 'BORRADOR' || o.estado === 'APROBADA') {
    parts.push(`<button class="btn btn-ghost btn-sm" onclick="cambiarEstadoOrden(${o.id}, 'CANCELADA')">Cancelar</button>`);
  }
  return parts.length ? `<div class="actions-cell">${parts.join('')}</div>` : '<span class="muted">—</span>';
}

function renderOrdenesTabla(tbodyId, ordenes, compacto) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  if (!ordenes.length) {
    const cols = compacto ? 8 : 10;
    tbody.innerHTML = `<tr><td colspan="${cols}"><div class="table-loading">No hay órdenes</div></td></tr>`;
    return;
  }
  tbody.innerHTML = ordenes.map((raw) => {
    const o = etiquetaOrden(raw);
    if (compacto) {
      return `<tr>
        <td>#${o.id}</td>
        <td>${escapeHtml(o.producto)}</td>
        <td>${escapeHtml(o.proveedor)}</td>
        <td>${o.cantidad}</td>
        <td>${formatCurrency(o.total)}</td>
        <td>${escapeHtml(o.bodega)}</td>
        <td>${botonesPdf(o)}</td>
        <td>${botonesEstado(raw)}</td>
      </tr>`;
    }
    return `<tr>
      <td>#${o.id}</td>
      <td><span class="badge badge-estado-${o.estado}">${escapeHtml(o.estado)}</span></td>
      <td>${escapeHtml(o.producto)}</td>
      <td>${escapeHtml(o.proveedor)}</td>
      <td>${o.cantidad}</td>
      <td>${formatCurrency(o.total)}</td>
      <td>${escapeHtml(o.bodega)}</td>
      <td>${formatDate(o.fecha)}</td>
      <td>${botonesPdf(o)}</td>
      <td>${botonesEstado(raw)}</td>
    </tr>`;
  }).join('');
}

async function loadOrdenes() {
  try {
    bodegasCache = await apiFetch('/api/bodegas') || bodegasCache;
    const estado = document.getElementById('filter-estado-orden')?.value;
    const path = estado ? `/api/ordenes?estado=${encodeURIComponent(estado)}` : '/api/ordenes';
    ordenesCache = await apiFetch(path) || [];
    renderOrdenesTabla('tbody-ordenes', ordenesCache, false);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-ordenes', 10, e.message);
    setApiStatus(false);
  }
}

async function cambiarEstadoOrden(id, estado) {
  if (!isAdmin()) {
    showToast('Solo un ADMIN puede cambiar el estado de una orden.', 'error');
    return;
  }
  try {
    await apiFetch(`/api/ordenes/${id}/estado`, {
      method: 'PATCH',
      body: JSON.stringify({ estado }),
    });
    showToast(`Orden #${id} → ${estado}`, 'success');
    await loadDashboard();
    if (document.getElementById('page-ordenes')?.classList.contains('active')) {
      await loadOrdenes();
    }
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function generarPdfOrden(id) {
  try {
    const blob = await apiFetchBlob(`/api/ordenes/${id}/pdf`, { method: 'POST' });
    abrirPdfBlob(blob);
    showToast('PDF generado. Si la orden está en BORRADOR verá la marca de agua.', 'success');
    if (document.getElementById('page-ordenes')?.classList.contains('active')) await loadOrdenes();
    else await loadDashboard();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function verPdfOrden(id) {
  try {
    const blob = await apiFetchBlob(`/api/ordenes/${id}/pdf`);
    abrirPdfBlob(blob);
  } catch (e) {
    showToast(e.message + ' — genera el PDF primero.', 'error');
  }
}

function abrirPdfBlob(blob) {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
}

document.getElementById('filter-estado-orden')?.addEventListener('change', () => loadOrdenes());
document.getElementById('btn-refresh-ordenes')?.addEventListener('click', () => loadOrdenes());

function animateCount(id, target) {
  const el = document.getElementById(id);
  if (!el || typeof target !== 'number') { if (el) el.textContent = target; return; }
  let start = 0;
  const step = target / 30;
  const interval = setInterval(() => {
    start = Math.min(start + Math.ceil(step), target);
    el.textContent = start;
    if (start >= target) clearInterval(interval);
  }, 25);
}

function renderRecentMovimientos(movimientos) {
  const container = document.getElementById('recent-movimientos');
  if (!container) return;
  const recent = movimientos.slice(-5).reverse();
  if (!recent.length) { container.innerHTML = '<p style="color:var(--clr-txt-muted);font-size:.85rem;text-align:center;padding:1rem;">Sin movimientos registrados</p>'; return; }
  const colors = { ENTRADA: '#22c55e', SALIDA: '#ef4444', TRANSFERENCIA: '#3b82f6' };
  const bgs = { ENTRADA: '#f0fdf4', SALIDA: '#fef2f2', TRANSFERENCIA: '#eff6ff' };
  container.innerHTML = recent.map(m => `
    <div class="recent-item">
      <div class="recent-item-icon" style="background:${bgs[m.tipoMovimiento] || '#f5f4f2'};color:${colors[m.tipoMovimiento] || '#78716c'};">
        ${(m.tipoMovimiento || '?').charAt(0)}
      </div>
      <div class="recent-item-info">
        <div class="recent-item-title">${escapeHtml(m.tipoMovimiento)} — ID #${m.id}</div>
        <div class="recent-item-sub">Usuario: ${escapeHtml(m.usuario?.username || m.usuario || '—')}</div>
      </div>
      <span class="recent-item-right">${formatDate(m.fecha)}</span>
    </div>
  `).join('');
}

function renderLowStock(productos) {
  const container = document.getElementById('low-stock-list');
  if (!container) return;
  if (!productos.length) { container.innerHTML = '<p style="color:var(--clr-green);font-size:.85rem;text-align:center;padding:1rem;">✓ Todo el stock es suficiente</p>'; return; }
  container.innerHTML = productos.map(p => `
    <div class="recent-item">
      <div class="recent-item-icon" style="background:#fef2f2;color:#ef4444;">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>
      </div>
      <div class="recent-item-info">
        <div class="recent-item-title">${escapeHtml(p.nombre)}</div>
        <div class="recent-item-sub">${escapeHtml(p.categoria)}</div>
      </div>
      <span class="recent-item-right stock-low">${p.stock} uds</span>
    </div>
  `).join('');
}

/* ─────────────────────────────────────────────────────
   BODEGAS
   ───────────────────────────────────────────────────── */
let bodegasData = [];

async function loadBodegas() {
  try {
    bodegasData = await apiFetch('/api/bodegas') || [];
    renderBodegasTable(bodegasData);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-bodegas', 6, e.message);
    setApiStatus(false);
  }
}

function renderBodegasTable(data) {
  const tbody = document.getElementById('tbody-bodegas');
  if (!tbody) return;
  if (!data.length) { tbody.innerHTML = `<tr><td colspan="6"><div class="table-loading">No hay bodegas registradas</div></td></tr>`; return; }
  tbody.innerHTML = data.map(b => `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${b.id}</span></td>
      <td style="font-weight:500">${escapeHtml(b.nombre)}</td>
      <td>${escapeHtml(b.ubicacion)}</td>
      <td>${Number(b.capacidad).toLocaleString('es-CO')} uds</td>
      <td>${escapeHtml(b.encargado)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" onclick="editBodega(${b.id})" title="Editar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="btn-icon danger" onclick="confirmDelete('bodega', ${b.id}, '${escapeHtml(b.nombre)}')" title="Eliminar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

document.getElementById('search-bodegas')?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  renderBodegasTable(bodegasData.filter(b =>
    b.nombre.toLowerCase().includes(q) ||
    b.ubicacion.toLowerCase().includes(q) ||
    b.encargado.toLowerCase().includes(q)
  ));
});

document.getElementById('btn-nueva-bodega')?.addEventListener('click', () => {
  document.getElementById('form-bodega').reset();
  document.getElementById('bodega-id').value = '';
  document.getElementById('modal-bodega-title').textContent = 'Nueva Bodega';
  document.getElementById('bodega-error').classList.add('hidden');
  openModal('modal-bodega');
});

function editBodega(id) {
  const b = bodegasData.find(x => x.id === id);
  if (!b) return;
  document.getElementById('bodega-id').value = b.id;
  document.getElementById('bodega-nombre').value = b.nombre;
  document.getElementById('bodega-ubicacion').value = b.ubicacion;
  document.getElementById('bodega-capacidad').value = b.capacidad;
  document.getElementById('bodega-encargado').value = b.encargado;
  document.getElementById('modal-bodega-title').textContent = 'Editar Bodega';
  document.getElementById('bodega-error').classList.add('hidden');
  openModal('modal-bodega');
}

document.getElementById('form-bodega')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('bodega-id').value;
  const body = {
    nombre: document.getElementById('bodega-nombre').value.trim(),
    ubicacion: document.getElementById('bodega-ubicacion').value.trim(),
    capacidad: parseInt(document.getElementById('bodega-capacidad').value),
    encargado: document.getElementById('bodega-encargado').value.trim(),
  };
  if (!body.nombre || !body.ubicacion || !body.capacidad || !body.encargado) {
    showModalError('bodega-error', 'Completa todos los campos obligatorios.');
    return;
  }
  setModalLoading('btn-save-bodega', true);
  try {
    if (id) {
      await apiFetch(`/api/bodegas/${id}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Bodega actualizada correctamente', 'success');
    } else {
      await apiFetch('/api/bodegas', { method: 'POST', body: JSON.stringify(body) });
      showToast('Bodega creada correctamente', 'success');
    }
    closeModal('modal-bodega');
    loadBodegas();
  } catch (err) {
    showModalError('bodega-error', err.message);
  } finally {
    setModalLoading('btn-save-bodega', false);
  }
});

/* ─────────────────────────────────────────────────────
   PRODUCTOS
   ───────────────────────────────────────────────────── */
let productosData = [];
let showingLowStock = false;

async function loadProductos() {
  try {
    productosData = await apiFetch('/api/productos') || [];
    renderProductosTable(productosData);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-productos', 6, e.message);
    setApiStatus(false);
  }
}

function renderProductosTable(data) {
  const tbody = document.getElementById('tbody-productos');
  if (!tbody) return;
  if (!data.length) { tbody.innerHTML = `<tr><td colspan="6"><div class="table-loading">No hay productos registrados</div></td></tr>`; return; }
  tbody.innerHTML = data.map(p => `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${p.id}</span></td>
      <td style="font-weight:500">${escapeHtml(p.nombre)}</td>
      <td><span class="badge badge-empleado">${escapeHtml(p.categoria)}</span></td>
      <td class="${p.stock < 10 ? 'stock-low' : 'stock-ok'}">${p.stock} uds ${p.stock < 10 ? '⚠' : ''}</td>
      <td>${formatCurrency(p.precio)}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" onclick="editProducto(${p.id})" title="Editar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="btn-icon danger" onclick="confirmDelete('producto', ${p.id}, '${escapeHtml(p.nombre)}')" title="Eliminar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

document.getElementById('search-productos')?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  const base = showingLowStock ? productosData.filter(p => p.stock < 10) : productosData;
  renderProductosTable(base.filter(p =>
    p.nombre.toLowerCase().includes(q) ||
    p.categoria.toLowerCase().includes(q)
  ));
});

document.getElementById('btn-stock-bajo')?.addEventListener('click', async () => {
  showingLowStock = !showingLowStock;
  const btn = document.getElementById('btn-stock-bajo');
  if (showingLowStock) {
    btn.classList.add('btn-primary');
    btn.classList.remove('btn-outline');
    try {
      const data = await apiFetch('/api/productos/stock-bajo');
      renderProductosTable(data || []);
    } catch (e) { showToast(e.message, 'error'); }
  } else {
    btn.classList.remove('btn-primary');
    btn.classList.add('btn-outline');
    renderProductosTable(productosData);
  }
});

document.getElementById('btn-nuevo-producto')?.addEventListener('click', async () => {
  document.getElementById('form-producto').reset();
  document.getElementById('producto-id').value = '';
  document.getElementById('modal-producto-title').textContent = 'Nuevo Producto';
  document.getElementById('producto-error').classList.add('hidden');
  document.getElementById('producto-stock').disabled = false;
  document.getElementById('label-producto-stock').textContent = 'Stock inicial *';
  document.getElementById('label-producto-bodega').textContent = 'Bodega del inventario *';
  document.getElementById('hint-producto-bodega').textContent = 'Si el stock es mayor a 0, se registra un movimiento de entrada en esa bodega.';
  document.getElementById('grupo-producto-bodega').classList.remove('hidden');
  await fillProductoBodegas();
  openModal('modal-producto');
});

async function fillProductoBodegas(selectedId) {
  const select = document.getElementById('producto-bodega');
  if (!select) return;
  try {
    const bodegas = await apiFetch('/api/bodegas') || [];
    select.innerHTML = '<option value="">Selecciona una bodega</option>' + bodegas.map(b =>
      `<option value="${b.id}" ${String(b.id) === String(selectedId) ? 'selected' : ''}>${escapeHtml(b.nombre)}</option>`
    ).join('');
  } catch {
    select.innerHTML = '<option value="">No se pudieron cargar las bodegas</option>';
  }
}

async function cargarStockBodegaProducto() {
  const productoId = document.getElementById('producto-id').value;
  const bodegaId = document.getElementById('producto-bodega').value;
  const stockInput = document.getElementById('producto-stock');
  if (!productoId || !bodegaId) return;
  try {
    const data = await apiFetch(`/api/productos/${productoId}/inventario/${bodegaId}`);
    stockInput.value = data?.cantidad ?? 0;
  } catch {
    stockInput.value = 0;
  }
}

document.getElementById('producto-bodega')?.addEventListener('change', () => {
  if (document.getElementById('producto-id').value) {
    cargarStockBodegaProducto();
  }
});

async function editProducto(id) {
  const p = productosData.find(x => x.id === id);
  if (!p) return;
  document.getElementById('producto-id').value = p.id;
  document.getElementById('producto-nombre').value = p.nombre;
  document.getElementById('producto-categoria').value = p.categoria;
  document.getElementById('producto-precio').value = p.precio;
  document.getElementById('producto-stock').disabled = false;
  document.getElementById('label-producto-stock').textContent = 'Stock en la bodega *';
  document.getElementById('label-producto-bodega').textContent = 'Bodega a ajustar *';
  document.getElementById('hint-producto-bodega').textContent = 'El valor es el stock de esa bodega. Si lo cambias, se genera un movimiento de entrada o salida para conservar la trazabilidad.';
  document.getElementById('grupo-producto-bodega').classList.remove('hidden');
  document.getElementById('modal-producto-title').textContent = 'Editar Producto';
  document.getElementById('producto-error').classList.add('hidden');
  await fillProductoBodegas();
  const select = document.getElementById('producto-bodega');
  if (select.options.length > 1) {
    select.selectedIndex = 1;
    await cargarStockBodegaProducto();
  } else {
    document.getElementById('producto-stock').value = p.stock ?? 0;
  }
  openModal('modal-producto');
}

document.getElementById('form-producto')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('producto-id').value;
  const stockVal = parseInt(document.getElementById('producto-stock').value);
  const bodegaIdVal = document.getElementById('producto-bodega')?.value;
  const body = {
    nombre: document.getElementById('producto-nombre').value.trim(),
    categoria: document.getElementById('producto-categoria').value.trim(),
    stock: stockVal,
    precio: parseFloat(document.getElementById('producto-precio').value),
    bodegaId: bodegaIdVal ? parseInt(bodegaIdVal) : null,
  };
  if (!body.nombre || !body.categoria || isNaN(body.stock) || isNaN(body.precio)) {
    showModalError('producto-error', 'Completa todos los campos obligatorios.');
    return;
  }
  if (body.stock > 0 && !body.bodegaId) {
    showModalError('producto-error', 'Selecciona la bodega para el inventario.');
    return;
  }
  if (id && body.stock >= 0 && !body.bodegaId) {
    showModalError('producto-error', 'Selecciona la bodega cuyo stock quieres ajustar.');
    return;
  }
  setModalLoading('btn-save-producto', true);
  try {
    if (id) {
      await apiFetch(`/api/productos/${id}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Producto actualizado. Si cambió el stock, se registró un movimiento.', 'success');
    } else {
      await apiFetch('/api/productos', { method: 'POST', body: JSON.stringify(body) });
      showToast('Producto creado correctamente', 'success');
    }
    closeModal('modal-producto');
    loadProductos();
  } catch (err) {
    showModalError('producto-error', err.message);
  } finally {
    setModalLoading('btn-save-producto', false);
  }
});

/* ─────────────────────────────────────────────────────
   MOVIMIENTOS
   ───────────────────────────────────────────────────── */
let movimientosData = [];

async function loadMovimientos() {
  try {
    movimientosData = await apiFetch('/api/movimientos') || [];
    renderMovimientosTable(movimientosData);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-movimientos', 7, e.message);
    setApiStatus(false);
  }
}

function renderMovimientosTable(data) {
  const tbody = document.getElementById('tbody-movimientos');
  if (!tbody) return;
  if (!data.length) { tbody.innerHTML = `<tr><td colspan="7"><div class="table-loading">No hay movimientos registrados</div></td></tr>`; return; }
  tbody.innerHTML = data.map(m => {
    const tipo = m.tipoMovimiento || '—';
    const badgeClass = `badge badge-${tipo.toLowerCase()}`;
    return `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${m.id}</span></td>
      <td>${formatDate(m.fecha)}</td>
      <td><span class="${badgeClass}">${tipo}</span></td>
      <td>${escapeHtml(m.usuario?.username || m.usuario || '—')}</td>
      <td>${escapeHtml(m.bodegaOrigen?.nombre || '—')}</td>
      <td>${escapeHtml(m.bodegaDestino?.nombre || '—')}</td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" onclick="verDetalleMovimiento(${m.id})" title="Ver detalle">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
          </button>
        </div>
      </td>
    </tr>`;
  }).join('');
}

document.getElementById('search-movimientos')?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  const tipo = document.getElementById('filter-tipo-movimiento').value;
  filterMovimientos(q, tipo);
});

document.getElementById('filter-tipo-movimiento')?.addEventListener('change', (e) => {
  const q = document.getElementById('search-movimientos').value.toLowerCase();
  filterMovimientos(q, e.target.value);
});

function filterMovimientos(q, tipo) {
  let data = movimientosData;
  if (tipo) data = data.filter(m => m.tipoMovimiento === tipo);
  if (q) data = data.filter(m =>
    String(m.id).includes(q) ||
    (m.usuario?.username || '').toLowerCase().includes(q) ||
    (m.bodegaOrigen?.nombre || '').toLowerCase().includes(q) ||
    (m.bodegaDestino?.nombre || '').toLowerCase().includes(q)
  );
  renderMovimientosTable(data);
}

function verDetalleMovimiento(id) {
  const m = movimientosData.find(x => x.id === id);
  if (!m) return;
  const detalles = m.detalles || [];
  const detallesHtml = detalles.length
    ? `<div class="detail-section">Productos</div>
       <table class="data-table" style="margin-top:.25rem">
         <thead><tr><th>Producto ID</th><th>Nombre</th><th>Cantidad</th></tr></thead>
         <tbody>${detalles.map(d => `<tr><td>#${d.producto?.id || d.productoId || '—'}</td><td>${escapeHtml(d.producto?.nombre || '—')}</td><td>${d.cantidad}</td></tr>`).join('')}</tbody>
       </table>`
    : '<p style="color:var(--clr-txt-muted);font-size:.85rem">Sin detalles de productos</p>';
  document.getElementById('modal-detalle-title').textContent = `Movimiento #${m.id}`;
  document.getElementById('modal-detalle-body').innerHTML = `
    <div class="detail-grid">
      <div class="detail-item"><span class="detail-label">ID</span><span class="detail-value">#${m.id}</span></div>
      <div class="detail-item"><span class="detail-label">Tipo</span><span class="detail-value">${m.tipoMovimiento}</span></div>
      <div class="detail-item"><span class="detail-label">Fecha</span><span class="detail-value">${formatDate(m.fecha)}</span></div>
      <div class="detail-item"><span class="detail-label">Usuario</span><span class="detail-value">${escapeHtml(m.usuario?.username || '—')}</span></div>
      <div class="detail-item"><span class="detail-label">Bodega Origen</span><span class="detail-value">${escapeHtml(m.bodegaOrigen?.nombre || '—')}</span></div>
      <div class="detail-item"><span class="detail-label">Bodega Destino</span><span class="detail-value">${escapeHtml(m.bodegaDestino?.nombre || '—')}</span></div>
    </div>
    ${detallesHtml}
  `;
  openModal('modal-detalle');
}

document.getElementById('btn-nuevo-movimiento')?.addEventListener('click', () => {
  document.getElementById('form-movimiento').reset();
  document.getElementById('detalles-container').innerHTML = `
    <div class="detalle-row" data-index="0">
      <input type="number" placeholder="ID Producto" class="detalle-producto" />
      <input type="number" placeholder="Cantidad" class="detalle-cantidad" min="1" />
      <button type="button" class="btn-remove-detalle" onclick="removeDetalle(this)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
      </button>
    </div>`;
  document.getElementById('movimiento-error').classList.add('hidden');
  openModal('modal-movimiento');
});

document.getElementById('btn-add-detalle')?.addEventListener('click', () => {
  const container = document.getElementById('detalles-container');
  const idx = container.children.length;
  const div = document.createElement('div');
  div.className = 'detalle-row';
  div.dataset.index = idx;
  div.innerHTML = `
    <input type="number" placeholder="ID Producto" class="detalle-producto" />
    <input type="number" placeholder="Cantidad" class="detalle-cantidad" min="1" />
    <button type="button" class="btn-remove-detalle" onclick="removeDetalle(this)">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
    </button>`;
  container.appendChild(div);
});

function removeDetalle(btn) {
  const row = btn.closest('.detalle-row');
  const container = document.getElementById('detalles-container');
  if (container.children.length > 1) row.remove();
}

document.getElementById('form-movimiento')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const tipo = document.getElementById('mov-tipo').value;
  const usuarioId = document.getElementById('mov-usuario').value;
  const bodegaOrigenId = document.getElementById('mov-bodega-origen').value;
  const bodegaDestinoId = document.getElementById('mov-bodega-destino').value;
  if (!tipo || !usuarioId) { showModalError('movimiento-error', 'Completa el tipo y usuario.'); return; }
  const detalleRows = document.querySelectorAll('#detalles-container .detalle-row');
  const detalles = [];
  for (const row of detalleRows) {
    const pid = row.querySelector('.detalle-producto').value;
    const qty = row.querySelector('.detalle-cantidad').value;
    if (pid && qty) detalles.push({ producto: { id: parseInt(pid) }, cantidad: parseInt(qty) });
  }
  const body = {
    tipoMovimiento: tipo,
    usuario: { id: parseInt(usuarioId) },
    detalles,
  };
  if (bodegaOrigenId) body.bodegaOrigen = { id: parseInt(bodegaOrigenId) };
  if (bodegaDestinoId) body.bodegaDestino = { id: parseInt(bodegaDestinoId) };
  setModalLoading('btn-save-movimiento', true);
  try {
    await apiFetch('/api/movimientos', { method: 'POST', body: JSON.stringify(body) });
    showToast('Movimiento registrado correctamente', 'success');
    closeModal('modal-movimiento');
    loadMovimientos();
  } catch (err) {
    showModalError('movimiento-error', err.message);
  } finally {
    setModalLoading('btn-save-movimiento', false);
  }
});

/* ─────────────────────────────────────────────────────
   REPORTES
   ───────────────────────────────────────────────────── */
function toIsoLocal(value) {
  if (!value) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return d.toISOString();
}

async function initReportes() {
  const select = document.getElementById('reporte-bodega');
  try {
    const bodegas = await apiFetch('/api/bodegas') || [];
    const current = select.value;
    select.innerHTML = '<option value="">Todas las bodegas</option>' + bodegas.map(b =>
      `<option value="${b.id}" data-nombre="${escapeHtml(b.nombre)}">${escapeHtml(b.nombre)}</option>`
    ).join('');
    if (current) select.value = current;
  } catch { /* se puede generar sin el combo de bodegas */ }
}

document.getElementById('btn-generar-reporte')?.addEventListener('click', generarReporte);

async function generarReporte() {
  const tipo = document.getElementById('reporte-tipo').value;
  const bodegaSelect = document.getElementById('reporte-bodega');
  const bodegaId = bodegaSelect.value;
  const bodegaNombre = bodegaSelect.selectedOptions[0]?.dataset?.nombre || bodegaSelect.selectedOptions[0]?.textContent;
  const tipoMov = document.getElementById('reporte-tipo-movimiento').value;
  const producto = document.getElementById('reporte-producto').value.trim();
  const entidad = document.getElementById('reporte-entidad').value.trim();
  const fechaInicio = toIsoLocal(document.getElementById('reporte-fecha-inicio').value);
  const fechaFin = toIsoLocal(document.getElementById('reporte-fecha-fin').value);
  const tbody = document.getElementById('tbody-reportes');
  const thead = document.getElementById('thead-reportes');
  tbody.innerHTML = `<tr class="loading-row"><td colspan="8"><div class="table-loading">Generando reporte...</div></td></tr>`;
  try {
    if (tipo === 'inventario') {
      const qs = bodegaId ? `?bodegaId=${encodeURIComponent(bodegaId)}` : '';
      const data = await apiFetch(`/api/reportes/inventario${qs}`) || [];
      thead.innerHTML = `<tr><th>Bodega</th><th>Producto</th><th>Categoría</th><th>Cantidad</th></tr>`;
      if (!data.length) {
        tbody.innerHTML = `<tr><td colspan="4"><div class="table-loading">Sin registros de inventario</div></td></tr>`;
        return;
      }
      tbody.innerHTML = data.map(r => `
        <tr>
          <td>${escapeHtml(r.bodegaNombre)}</td>
          <td>${escapeHtml(r.productoNombre)}</td>
          <td>${escapeHtml(r.categoria)}</td>
          <td class="${r.cantidad < 10 ? 'stock-low' : 'stock-ok'}">${r.cantidad}</td>
        </tr>`).join('');
    } else if (tipo === 'movimientos') {
      const params = new URLSearchParams();
      if (bodegaId && bodegaNombre && bodegaNombre !== 'Todas las bodegas') params.set('bodega', bodegaNombre);
      if (producto) params.set('producto', producto);
      if (tipoMov) params.set('tipoMovimiento', tipoMov);
      if (fechaInicio) params.set('fechaInicio', fechaInicio);
      if (fechaFin) params.set('fechaFin', fechaFin);
      const q = params.toString();
      const data = await apiFetch(`/api/reportes/movimientos${q ? '?' + q : ''}`) || [];
      thead.innerHTML = `<tr><th>ID</th><th>Fecha</th><th>Tipo</th><th>Usuario</th><th>Origen</th><th>Destino</th></tr>`;
      if (!data.length) {
        tbody.innerHTML = `<tr><td colspan="6"><div class="table-loading">Sin movimientos para los filtros</div></td></tr>`;
        return;
      }
      tbody.innerHTML = data.map(m => `
        <tr>
          <td>#${m.id}</td>
          <td>${formatDate(m.fecha)}</td>
          <td>${escapeHtml(m.tipoMovimiento)}</td>
          <td>${escapeHtml(m.usuario?.username)}</td>
          <td>${escapeHtml(m.bodegaOrigen?.nombre)}</td>
          <td>${escapeHtml(m.bodegaDestino?.nombre)}</td>
        </tr>`).join('');
    } else {
      const params = new URLSearchParams();
      if (entidad) params.set('entidadAfectada', entidad);
      if (fechaInicio) params.set('fechaInicio', fechaInicio);
      if (fechaFin) params.set('fechaFin', fechaFin);
      const q = params.toString();
      const data = await apiFetch(`/api/reportes/auditoria${q ? '?' + q : ''}`) || [];
      thead.innerHTML = `<tr><th>ID</th><th>Operación</th><th>Fecha</th><th>Usuario</th><th>Entidad</th><th>ID Entidad</th></tr>`;
      if (!data.length) {
        tbody.innerHTML = `<tr><td colspan="6"><div class="table-loading">Sin registros de auditoría para los filtros</div></td></tr>`;
        return;
      }
      tbody.innerHTML = data.map(a => `
        <tr>
          <td>#${a.id}</td>
          <td>${escapeHtml(a.tipoOperacion)}</td>
          <td>${formatDate(a.fechaHora)}</td>
          <td>${escapeHtml(a.usuario)}</td>
          <td>${escapeHtml(a.entidadAfectada)}</td>
          <td>${a.entidadId || '—'}</td>
        </tr>`).join('');
    }
    setApiStatus(true);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="table-loading">${escapeHtml(err.message)}</div></td></tr>`;
    setApiStatus(false);
  }
}

/* ─────────────────────────────────────────────────────
   AUDITORÍA
   ───────────────────────────────────────────────────── */
let auditoriaData = [];

async function loadAuditoria() {
  try {
    auditoriaData = await apiFetch('/api/auditoria') || [];
    renderAuditoriaTable(auditoriaData);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-auditoria', 7, e.message);
    setApiStatus(false);
  }
}

function renderAuditoriaTable(data) {
  const tbody = document.getElementById('tbody-auditoria');
  if (!tbody) return;
  if (!data.length) { tbody.innerHTML = `<tr><td colspan="7"><div class="table-loading">No hay registros de auditoría</div></td></tr>`; return; }
  tbody.innerHTML = data.map(a => {
    const op = (a.tipoOperacion || '').toLowerCase();
    return `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${a.id}</span></td>
      <td><span class="badge badge-${op}">${a.tipoOperacion || '—'}</span></td>
      <td>${formatDate(a.fechaHora)}</td>
      <td style="font-weight:500">${escapeHtml(a.usuario)}</td>
      <td>${escapeHtml(a.entidadAfectada)}</td>
      <td>${a.entidadId || '—'}</td>
      <td>
        <button class="btn-icon" onclick="verDetalleAuditoria(${a.id})" title="Ver cambios">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        </button>
      </td>
    </tr>`;
  }).join('');
}

document.getElementById('search-auditoria')?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  const op = document.getElementById('filter-tipo-operacion').value;
  filterAuditoria(q, op);
});
document.getElementById('filter-tipo-operacion')?.addEventListener('change', (e) => {
  const q = document.getElementById('search-auditoria').value.toLowerCase();
  filterAuditoria(q, e.target.value);
});

function filterAuditoria(q, op) {
  let data = auditoriaData;
  if (op) data = data.filter(a => a.tipoOperacion === op);
  if (q) data = data.filter(a =>
    (a.usuario || '').toLowerCase().includes(q) ||
    (a.entidadAfectada || '').toLowerCase().includes(q)
  );
  renderAuditoriaTable(data);
}

function verDetalleAuditoria(id) {
  const a = auditoriaData.find(x => x.id === id);
  if (!a) return;
  document.getElementById('modal-detalle-title').textContent = `Auditoría #${a.id}`;
  document.getElementById('modal-detalle-body').innerHTML = `
    <div class="detail-grid">
      <div class="detail-item"><span class="detail-label">ID</span><span class="detail-value">#${a.id}</span></div>
      <div class="detail-item"><span class="detail-label">Operación</span><span class="detail-value">${a.tipoOperacion}</span></div>
      <div class="detail-item"><span class="detail-label">Fecha/Hora</span><span class="detail-value">${formatDate(a.fechaHora)}</span></div>
      <div class="detail-item"><span class="detail-label">Usuario</span><span class="detail-value">${escapeHtml(a.usuario)}</span></div>
      <div class="detail-item"><span class="detail-label">Entidad</span><span class="detail-value">${escapeHtml(a.entidadAfectada)}</span></div>
      <div class="detail-item"><span class="detail-label">ID Entidad</span><span class="detail-value">${a.entidadId || '—'}</span></div>
    </div>
    <div class="detail-section">Valor Anterior</div>
    <div class="detail-pre">${a.valorAnterior ? escapeHtml(a.valorAnterior) : 'Sin valor anterior'}</div>
    <div class="detail-section">Valor Nuevo</div>
    <div class="detail-pre">${a.valorNuevo ? escapeHtml(a.valorNuevo) : 'Sin valor nuevo'}</div>
  `;
  openModal('modal-detalle');
}

/* ─────────────────────────────────────────────────────
   USUARIOS
   ───────────────────────────────────────────────────── */
let usuariosData = [];

async function loadUsuarios() {
  try {
    usuariosData = await apiFetch('/api/usuarios') || [];
    renderUsuariosTable(usuariosData);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-usuarios', 4, e.message);
    setApiStatus(false);
  }
}

function renderUsuariosTable(data) {
  const tbody = document.getElementById('tbody-usuarios');
  if (!tbody) return;
  if (!data.length) { tbody.innerHTML = `<tr><td colspan="4"><div class="table-loading">No hay usuarios registrados</div></td></tr>`; return; }
  tbody.innerHTML = data.map(u => `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${u.id}</span></td>
      <td style="font-weight:500">
        <div style="display:flex;align-items:center;gap:.5rem">
          <div style="width:28px;height:28px;background:var(--clr-sidebar-bg);color:#fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:.7rem;font-weight:700;flex-shrink:0">
            ${(u.username || '?').charAt(0).toUpperCase()}
          </div>
          ${escapeHtml(u.username)}
        </div>
      </td>
      <td><span class="badge ${u.rol === 'ADMIN' ? 'badge-admin' : u.rol === 'AGENTE' ? 'badge-agente' : 'badge-empleado'}">${u.rol}</span></td>
      <td>
        <div class="actions-cell">
          <button class="btn-icon" onclick="editUsuario(${u.id})" title="Editar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="btn-icon danger" onclick="confirmDelete('usuario', ${u.id}, '${escapeHtml(u.username)}')" title="Eliminar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

document.getElementById('search-usuarios')?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  renderUsuariosTable(usuariosData.filter(u => u.username.toLowerCase().includes(q)));
});

document.getElementById('btn-nuevo-usuario')?.addEventListener('click', () => {
  document.getElementById('form-usuario').reset();
  document.getElementById('usuario-id').value = '';
  document.getElementById('modal-usuario-title').textContent = 'Nuevo Usuario';
  document.getElementById('usuario-error').classList.add('hidden');
  document.getElementById('group-password').style.display = '';
  openModal('modal-usuario');
});

function editUsuario(id) {
  const u = usuariosData.find(x => x.id === id);
  if (!u) return;
  document.getElementById('usuario-id').value = u.id;
  document.getElementById('usuario-username').value = u.username;
  document.getElementById('usuario-password').value = '';
  document.getElementById('usuario-rol').value = u.rol;
  document.getElementById('modal-usuario-title').textContent = 'Editar Usuario';
  document.getElementById('usuario-error').classList.add('hidden');
  document.getElementById('group-password').style.display = 'none';
  openModal('modal-usuario');
}

document.getElementById('form-usuario')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('usuario-id').value;
  const body = {
    username: document.getElementById('usuario-username').value.trim(),
    rol: document.getElementById('usuario-rol').value,
  };
  if (!id) {
    body.password = document.getElementById('usuario-password').value;
    if (!body.password) { showModalError('usuario-error', 'La contraseña es obligatoria.'); return; }
  }
  if (!body.username) { showModalError('usuario-error', 'El nombre de usuario es obligatorio.'); return; }
  setModalLoading('btn-save-usuario', true);
  try {
    if (id) {
      await apiFetch(`/api/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Usuario actualizado correctamente', 'success');
    } else {
      await apiFetch('/api/usuarios', { method: 'POST', body: JSON.stringify(body) });
      showToast('Usuario creado correctamente', 'success');
    }
    closeModal('modal-usuario');
    loadUsuarios();
  } catch (err) {
    showModalError('usuario-error', err.message);
  } finally {
    setModalLoading('btn-save-usuario', false);
  }
});

/* ─────────────────────────────────────────────────────
   DELETE CONFIRM
   ───────────────────────────────────────────────────── */
function confirmDelete(type, id, name) {
  document.getElementById('confirm-message').textContent =
    `¿Estás seguro de que deseas eliminar "${name}"? Esta acción no se puede deshacer.`;
  deleteCallback = async () => {
    setModalLoading('btn-confirm-delete', true);
    try {
      const endpoints = { bodega: '/api/bodegas', producto: '/api/productos', usuario: '/api/usuarios' };
      await apiFetch(`${endpoints[type]}/${id}`, { method: 'DELETE' });
      showToast(`${type.charAt(0).toUpperCase() + type.slice(1)} eliminado correctamente`, 'success');
      closeModal('modal-confirm');
      const loaders = { bodega: loadBodegas, producto: loadProductos, usuario: loadUsuarios };
      loaders[type]?.();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setModalLoading('btn-confirm-delete', false);
    }
  };
  openModal('modal-confirm');
}

document.getElementById('btn-confirm-delete')?.addEventListener('click', () => {
  if (deleteCallback) deleteCallback();
});

/* ─────────────────────────────────────────────────────
   HELPERS
   ───────────────────────────────────────────────────── */
function showModalError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.classList.remove('hidden');
}

function setModalLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  const txt = btn.querySelector('.btn-text');
  const spinner = btn.querySelector('.btn-spinner');
  if (txt) txt.classList.toggle('hidden', loading);
  if (spinner) spinner.classList.toggle('hidden', !loading);
  btn.disabled = loading;
}

function showTableError(tbodyId, cols, msg) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="${cols}"><div class="table-loading" style="color:var(--clr-red)">⚠ ${escapeHtml(msg)}</div></td></tr>`;
}

/* ─────────────────────────────────────────────────────
   INIT — Restaurar sesión desde sessionStorage
   ───────────────────────────────────────────────────── */
(function init() {
  try {
    localStorage.removeItem('logitrack_token');
    localStorage.removeItem('token');
    localStorage.removeItem('jwt');
  } catch (_) { /* ignore */ }
  const saved = sessionStorage.getItem('logitrack_token');
  const savedUser = sessionStorage.getItem('logitrack_user');
  const savedRol = sessionStorage.getItem('logitrack_rol');
  if (saved) {
    jwtToken = saved;
    currentUser = savedUser || 'Usuario';
    currentRole = savedRol;
    enterApp();
    resolveCurrentRole();
  }
})();