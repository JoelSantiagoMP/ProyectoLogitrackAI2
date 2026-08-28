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

function sortEntitiesByIdAsc(items) {
  return [...(items || [])].sort((a, b) => Number(a.id) - Number(b.id));
}

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

function getCurrentRole() {
  return currentRole || sessionStorage.getItem('logitrack_rol');
}

function isAdmin() {
  return getCurrentRole() === 'ADMIN';
}

const ADMIN_MODALS = ['modal-movimiento', 'modal-bodega', 'modal-producto', 'modal-proveedor-rapido', 'modal-usuario', 'modal-confirm'];

const PAGE_ROLES = {
  dashboard: ['ADMIN', 'AGENTE'],
  ordenes: ['ADMIN', 'AGENTE'],
  bodegas: ['ADMIN', 'AGENTE', 'EMPLEADO'],
  productos: ['ADMIN', 'AGENTE', 'EMPLEADO'],
  movimientos: ['ADMIN', 'AGENTE', 'EMPLEADO'],
  reportes: ['ADMIN', 'AGENTE', 'EMPLEADO'],
  auditoria: ['ADMIN'],
  usuarios: ['ADMIN'],
};

function canAccessPage(page) {
  const role = getCurrentRole();
  if (!role || !page) return false;
  return (PAGE_ROLES[page] || []).includes(role);
}

function getDefaultPageForRole() {
  const role = getCurrentRole();
  const priority = ['dashboard', 'bodegas', 'productos', 'movimientos', 'reportes', 'ordenes', 'auditoria', 'usuarios'];
  return priority.find((page) => (PAGE_ROLES[page] || []).includes(role)) || 'bodegas';
}

function applySidebarRbac() {
  const role = getCurrentRole();
  if (!role) return;

  document.querySelectorAll('.nav-item[data-page]').forEach((item) => {
    if (!canAccessPage(item.dataset.page)) {
      item.remove();
    }
  });

  document.querySelectorAll('.sidebar-nav .nav-section-label').forEach((label) => {
    let sibling = label.nextElementSibling;
    let hasNavItem = false;
    while (sibling && !sibling.classList.contains('nav-section-label')) {
      if (sibling.matches('.nav-item[data-page]')) {
        hasNavItem = true;
        break;
      }
      sibling = sibling.nextElementSibling;
    }
    if (!hasNavItem) label.remove();
  });
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

function nombreEncargado(bodega) {
  if (!bodega) return 'Sin asignar';
  const enc = bodega.encargado;
  if (enc == null || enc === '') return 'Sin asignar';
  if (typeof enc === 'string') {
    const texto = enc.trim();
    return texto || 'Sin asignar';
  }
  const legacy = enc.username || enc.nombre || enc.name;
  if (legacy && String(legacy).trim()) return String(legacy).trim();
  return 'Sin asignar';
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
  if (ADMIN_MODALS.includes(id) && !isAdmin()) {
    showToast('No tienes permiso para esta acción.', 'error');
    return;
  }
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
  if (!canAccessPage(page)) {
    showToast('No tienes permiso para acceder a esta sección.', 'error');
    page = getDefaultPageForRole();
    if (!canAccessPage(page)) return;
  }
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
    const token = data?.token || data?.accessToken || data?.jwt || (typeof data === 'string' ? data : null);
    if (!token || typeof token !== 'string') throw new Error('Respuesta de login inválida.');
    saveSession(token, data?.username || username, data?.rol);
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
  navigateTo(getDefaultPageForRole());
}

async function resolveCurrentRole() {
  currentRole = sessionStorage.getItem('logitrack_rol');
  try {
    const me = await apiFetch('/api/auth/me');
    if (me?.rol) {
      currentRole = me.rol;
      sessionStorage.setItem('logitrack_rol', me.rol);
    }
    if (me?.username) {
      currentUser = me.username;
      sessionStorage.setItem('logitrack_user', me.username);
    }
  } catch (_) {
    /* Se conserva el rol almacenado en sesión si /me no está disponible */
  }
  applyRoleUi();
}

function applyRoleUi() {
  currentRole = sessionStorage.getItem('logitrack_rol') || currentRole;
  const roleEl = document.getElementById('sidebar-role');
  if (roleEl) roleEl.textContent = currentRole || '—';

  applySidebarRbac();

  if (!isAdmin()) {
    document.querySelectorAll('.admin-only').forEach((el) => el.remove());
    ADMIN_MODALS.forEach((id) => closeModal(id));
  }

  const activePage = pages.find(
    (p) => document.getElementById(`page-${p}`)?.classList.contains('active')
  );
  if (activePage && !canAccessPage(activePage)) {
    pages.forEach((p) => {
      document.getElementById(`page-${p}`)?.classList.remove('active');
      document.getElementById(`nav-${p}`)?.classList.remove('active');
    });
    const fallback = getDefaultPageForRole();
    document.getElementById(`page-${fallback}`)?.classList.add('active');
    document.getElementById(`nav-${fallback}`)?.classList.add('active');
    const [title, breadcrumb] = pageTitles[fallback] || [fallback, fallback];
    document.getElementById('page-title').textContent = title;
    document.getElementById('page-breadcrumb').textContent = breadcrumb;
    loadPage(fallback);
  }
}

document.getElementById('logout-btn')?.addEventListener('click', () => {
  clearSession();
  showToast('Sesión cerrada correctamente.', 'info');
  window.location.reload();
});

/* ─────────────────────────────────────────────────────
   DASHBOARD IQ
   ───────────────────────────────────────────────────── */
async function loadDashboard() {
  try {
    const fetches = [
      apiFetch('/api/kpis'),
      apiFetchOptional('/api/panel/resumen'),
      apiFetch('/api/productos/riesgo'),
      apiFetch('/api/ordenes?estado=BORRADOR'),
      apiFetch('/api/bodegas'),
    ];
    if (isAdmin()) {
      fetches.push(apiFetch('/api/ordenes?estado=APROBADA'));
    }
    const results = await Promise.allSettled(fetches);
    const [kpisR, resumenR, riesgoR, ordenesR, bodegasR, aprobadasR] = results;

    if (bodegasR.status === 'fulfilled') bodegasCache = sortEntitiesByIdAsc(bodegasR.value || []);
    if (kpisR.status === 'fulfilled') renderKpis(kpisR.value);
    else showToast(kpisR.reason?.message || 'No se pudieron cargar los KPIs', 'error');
    renderPanelResumen(resumenR.status === 'fulfilled' ? resumenR.value : null);
    renderRiesgo(riesgoR.status === 'fulfilled' ? riesgoR.value || [] : []);
    const borradores = ordenesR.status === 'fulfilled' ? ordenesR.value || [] : [];
    renderOrdenesTabla('tbody-ordenes-borrador', borradores, true);
    if (isAdmin()) {
      const aprobadas = aprobadasR?.status === 'fulfilled' ? aprobadasR.value || [] : [];
      renderOrdenesAprobadasDashboard(aprobadas);
    }
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
        const unidades = Number(b.unidades) || 0;
        const capacidad = Number(b.capacidad) || 0;
        const crit = pct >= 90;
        const detalleStock = capacidad > 0
          ? `${unidades.toLocaleString('es-CO')} / ${capacidad.toLocaleString('es-CO')} uds`
          : `${unidades.toLocaleString('es-CO')} uds`;
        return `<div class="ocupacion-row">
          <div class="ocupacion-head"><strong>${escapeHtml(b.nombre)}</strong><span>${pct.toFixed(1)}%</span></div>
          <div class="ocupacion-sub">${detalleStock}</div>
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

function renderOrdenesAprobadasDashboard(ordenes) {
  const tbody = document.getElementById('tbody-ordenes-aprobada');
  const card = document.getElementById('card-ordenes-aprobada');
  if (!tbody) return;
  if (!ordenes.length) {
    tbody.innerHTML = '<tr><td colspan="7"><div class="table-loading">No hay órdenes pendientes de recepción</div></td></tr>';
    if (card) card.classList.toggle('hidden', true);
    return;
  }
  if (card) card.classList.remove('hidden');
  tbody.innerHTML = ordenes.map((raw) => {
    const o = etiquetaOrden(raw);
    return `<tr>
      <td>#${o.id}</td>
      <td>${escapeHtml(o.producto)}</td>
      <td>${escapeHtml(o.proveedor)}</td>
      <td>${o.cantidad}</td>
      <td>${formatCurrency(o.total)}</td>
      <td>${escapeHtml(o.bodega)}</td>
      <td>${botonesEstado(raw)}</td>
    </tr>`;
  }).join('');
}

async function loadOrdenes() {
  try {
    bodegasCache = sortEntitiesByIdAsc(await apiFetch('/api/bodegas') || bodegasCache);
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
    if (estado === 'RECIBIDA' && document.getElementById('page-productos')?.classList.contains('active')) {
      await loadProductos();
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
    bodegasData = sortEntitiesByIdAsc(await apiFetch('/api/bodegas') || []);
    renderBodegasTable(bodegasData);
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-bodegas', isAdmin() ? 6 : 5, e.message);
    setApiStatus(false);
  }
}

function renderBodegasTable(data) {
  const tbody = document.getElementById('tbody-bodegas');
  if (!tbody) return;
  const cols = isAdmin() ? 6 : 5;
  const rows = sortEntitiesByIdAsc(data);
  if (!rows.length) { tbody.innerHTML = `<tr><td colspan="${cols}"><div class="table-loading">No hay bodegas registradas</div></td></tr>`; return; }
  tbody.innerHTML = rows.map(b => `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${b.id}</span></td>
      <td style="font-weight:500">${escapeHtml(b.nombre)}</td>
      <td>${escapeHtml(b.ubicacion)}</td>
      <td>${Number(b.capacidad).toLocaleString('es-CO')} uds</td>
      <td>${escapeHtml(nombreEncargado(b))}</td>
      ${isAdmin() ? `<td>
        <div class="actions-cell">
          <button class="btn-icon" onclick="editBodega(${b.id})" title="Editar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="btn-icon danger" onclick="confirmDelete('bodega', ${b.id}, '${escapeHtml(b.nombre)}')" title="Eliminar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>
          </button>
        </div>
      </td>` : ''}
    </tr>
  `).join('');
}

document.getElementById('search-bodegas')?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  renderBodegasTable(bodegasData.filter(b =>
    b.nombre.toLowerCase().includes(q) ||
    b.ubicacion.toLowerCase().includes(q) ||
    nombreEncargado(b).toLowerCase().includes(q)
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
  if (!isAdmin()) return;
  const b = bodegasData.find(x => x.id === id);
  if (!b) return;
  document.getElementById('bodega-id').value = b.id;
  document.getElementById('bodega-nombre').value = b.nombre;
  document.getElementById('bodega-ubicacion').value = b.ubicacion;
  document.getElementById('bodega-capacidad').value = b.capacidad;
  const encargadoLabel = nombreEncargado(b);
  document.getElementById('bodega-encargado').value = encargadoLabel === 'Sin asignar' ? '' : encargadoLabel;
  document.getElementById('modal-bodega-title').textContent = 'Editar Bodega';
  document.getElementById('bodega-error').classList.add('hidden');
  openModal('modal-bodega');
}

document.getElementById('form-bodega')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!isAdmin()) return;
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
let proveedoresData = [];
let showingLowStock = false;

function getProductosParaPicker() {
  return sortEntitiesByIdAsc(productosData);
}

function populateFiltroCategoriasProducto() {
  const select = document.getElementById('filter-categoria-producto');
  if (!select) return;
  const current = select.value;
  const categorias = [...new Set(productosData.map((p) => p.categoria).filter(Boolean))]
    .sort((a, b) => a.localeCompare(b, 'es'));
  fillSelectOptions(
    select,
    categorias,
    'Todas las categorías',
    (c) => `<option value="${escapeAttr(c)}">${escapeHtml(c)}</option>`
  );
  if (current && [...select.options].some((o) => o.value === current)) {
    select.value = current;
  }
}

function aplicarFiltrosProductos() {
  const q = (document.getElementById('search-productos')?.value || '').toLowerCase().trim();
  const categoria = document.getElementById('filter-categoria-producto')?.value || '';
  let data = showingLowStock
    ? productosData.filter((p) => (p.stock ?? 0) < 10)
    : productosData.slice();
  if (categoria) {
    data = data.filter((p) => (p.categoria || '') === categoria);
  }
  if (q) {
    data = data.filter((p) =>
      (p.nombre || '').toLowerCase().includes(q) ||
      (p.categoria || '').toLowerCase().includes(q) ||
      (p.bodegaNombre || '').toLowerCase().includes(q) ||
      (p.proveedorPrincipal?.nombre || '').toLowerCase().includes(q)
    );
  }
  renderProductosTable(sortEntitiesByIdAsc(data), Boolean(q || categoria || showingLowStock));
}

async function loadProductos() {
  try {
    productosData = sortEntitiesByIdAsc(await apiFetch('/api/productos') || []);
    populateFiltroCategoriasProducto();
    aplicarFiltrosProductos();
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-productos', isAdmin() ? 8 : 7, e.message);
    setApiStatus(false);
  }
}

function renderProductosTable(data, filtrosActivos = false) {
  const tbody = document.getElementById('tbody-productos');
  if (!tbody) return;
  const cols = isAdmin() ? 8 : 7;
  if (!data.length) {
    const msg = filtrosActivos ? 'No se encontraron productos' : 'No hay productos registrados';
    tbody.innerHTML = `<tr><td colspan="${cols}"><div class="table-loading">${msg}</div></td></tr>`;
    return;
  }
  tbody.innerHTML = data.map(p => `
    <tr>
      <td><span style="font-weight:600;color:var(--clr-txt-muted)">#${p.id}</span></td>
      <td style="font-weight:500">${escapeHtml(p.nombre)}</td>
      <td><span class="badge badge-empleado">${escapeHtml(p.categoria)}</span></td>
      <td>${p.proveedorPrincipal?.nombre
        ? `<span class="badge badge-proveedor" title="${escapeAttr(p.proveedorPrincipal.email || '')}">${escapeHtml(p.proveedorPrincipal.nombre)}</span>`
        : '<span style="color:var(--clr-txt-muted)">—</span>'}
      </td>
      <td>${p.bodegaNombre
        ? p.bodegaNombre.split(', ').map((nombre) => `<span class="badge badge-bodega">${escapeHtml(nombre)}</span>`).join(' ')
        : '<span style="color:var(--clr-txt-muted)">—</span>'}
      </td>
      <td class="${p.stock < 10 ? 'stock-low' : 'stock-ok'}">${p.stock} uds ${p.stock < 10 ? '⚠' : ''}</td>
      <td>${formatCurrency(p.precio)}</td>
      ${isAdmin() ? `<td>
        <div class="actions-cell">
          <button class="btn-icon" onclick="editProducto(${p.id})" title="Editar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="btn-icon danger" onclick="confirmDelete('producto', ${p.id}, '${escapeHtml(p.nombre)}')" title="Eliminar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>
          </button>
        </div>
      </td>` : ''}
    </tr>
  `).join('');
}

document.getElementById('search-productos')?.addEventListener('input', aplicarFiltrosProductos);
document.getElementById('filter-categoria-producto')?.addEventListener('change', aplicarFiltrosProductos);

document.getElementById('btn-stock-bajo')?.addEventListener('click', async () => {
  showingLowStock = !showingLowStock;
  const btn = document.getElementById('btn-stock-bajo');
  if (showingLowStock) {
    btn.classList.add('btn-primary');
    btn.classList.remove('btn-outline');
    try {
      const data = await apiFetch('/api/productos/stock-bajo');
      const byId = new Map((data || []).map((p) => [p.id, p]));
      productosData = sortEntitiesByIdAsc(productosData.map((p) => byId.has(p.id) ? { ...p, ...byId.get(p.id) } : p));
    } catch (e) { showToast(e.message, 'error'); }
  } else {
    btn.classList.remove('btn-primary');
    btn.classList.add('btn-outline');
  }
  aplicarFiltrosProductos();
});

document.getElementById('btn-nuevo-producto')?.addEventListener('click', async () => {
  document.getElementById('form-producto').reset();
  document.getElementById('producto-id').value = '';
  document.getElementById('modal-producto-title').textContent = 'Nuevo Producto';
  document.getElementById('producto-error').classList.add('hidden');
  document.getElementById('producto-stock').disabled = false;
  document.getElementById('label-producto-stock').textContent = 'Stock inicial *';
  document.getElementById('label-producto-bodega').textContent = 'Bodega *';
  document.getElementById('hint-producto-bodega').textContent = 'Indica la bodega donde se registrará el inventario de este producto.';
  document.getElementById('grupo-producto-bodega').classList.remove('hidden');
  await ensureProductoModalData();
  resetEntityPicker('producto-bodega');
  resetEntityPicker('producto-proveedor');
  openModal('modal-producto');
});

async function loadProveedoresParaPicker() {
  try {
    const data = await apiFetch('/api/proveedores');
    proveedoresData = sortEntitiesByIdAsc(data || []);
  } catch {
    proveedoresData = [];
  }
  return proveedoresData;
}

async function ensureProductoModalData() {
  const tasks = [];
  if (!bodegasCache.length && !bodegasData.length) {
    tasks.push(apiFetch('/api/bodegas').then((data) => {
      bodegasCache = sortEntitiesByIdAsc(data || []);
    }).catch(() => {
      bodegasCache = [];
    }));
  }
  tasks.push(loadProveedoresParaPicker());
  if (tasks.length) await Promise.all(tasks);
}

async function ensureProductoBodegasData() {
  await ensureProductoModalData();
}

async function setProductoProveedorPicker(selectedProveedor) {
  await ensureProductoModalData();
  resetEntityPicker('producto-proveedor');
  if (!selectedProveedor) return;
  const proveedor = getProveedoresParaPicker().find((pr) => String(pr.id) === String(selectedProveedor.id ?? selectedProveedor));
  if (proveedor) setEntityPickerSelection('producto-proveedor', proveedor);
}

async function setProductoBodegaPicker(selectedId) {
  await ensureProductoBodegasData();
  resetEntityPicker('producto-bodega');
  if (!selectedId) return;
  const bodega = getBodegasParaPicker().find((b) => String(b.id) === String(selectedId));
  if (bodega) setEntityPickerSelection('producto-bodega', bodega);
}

async function cargarStockBodegaProducto() {
  const productoId = document.getElementById('producto-id').value;
  const bodegaId = getEntityPickerValue('producto-bodega');
  const stockInput = document.getElementById('producto-stock');
  if (!productoId || !bodegaId) return;
  try {
    const data = await apiFetch(`/api/productos/${productoId}/inventario/${bodegaId}`);
    stockInput.value = data?.cantidad ?? 0;
  } catch {
    stockInput.value = 0;
  }
}

async function editProducto(id) {
  if (!isAdmin()) return;
  const p = productosData.find(x => x.id === id);
  if (!p) return;
  document.getElementById('producto-id').value = p.id;
  document.getElementById('producto-nombre').value = p.nombre;
  document.getElementById('producto-categoria').value = p.categoria;
  document.getElementById('producto-precio').value = p.precio;
  document.getElementById('producto-stock').disabled = false;
  document.getElementById('label-producto-stock').textContent = 'Stock en la bodega *';
  document.getElementById('label-producto-bodega').textContent = 'Bodega *';
  document.getElementById('hint-producto-bodega').textContent = 'Selecciona la bodega del producto. Si cambias el stock, se genera un movimiento de entrada o salida para conservar la trazabilidad.';
  document.getElementById('grupo-producto-bodega').classList.remove('hidden');
  document.getElementById('modal-producto-title').textContent = 'Editar Producto';
  document.getElementById('producto-error').classList.add('hidden');
  await setProductoBodegaPicker(p.bodegaId);
  await setProductoProveedorPicker(p.proveedorPrincipal);
  if (p.bodegaId) {
    await cargarStockBodegaProducto();
  } else {
    document.getElementById('producto-stock').value = p.stock ?? 0;
  }
  openModal('modal-producto');
}

document.getElementById('form-producto')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!isAdmin()) return;
  const id = document.getElementById('producto-id').value;
  const stockVal = parseInt(document.getElementById('producto-stock').value);
  const bodegaIdVal = getEntityPickerValue('producto-bodega');
  const proveedorIdVal = getEntityPickerValue('producto-proveedor');
  const body = {
    nombre: document.getElementById('producto-nombre').value.trim(),
    categoria: document.getElementById('producto-categoria').value.trim(),
    stock: stockVal,
    precio: parseFloat(document.getElementById('producto-precio').value),
    bodegaId: bodegaIdVal,
    proveedorId: proveedorIdVal,
  };
  if (!body.nombre || !body.categoria || isNaN(body.stock) || isNaN(body.precio)) {
    showModalError('producto-error', 'Completa todos los campos obligatorios.');
    return;
  }
  if (!body.bodegaId) {
    showModalError('producto-error', 'Selecciona la bodega del producto.');
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
   ENTITY PICKER (combobox predictivo reutilizable)
   ───────────────────────────────────────────────────── */
const entityPickerRegistry = {};

function registerEntityPicker(prefix, config) {
  entityPickerRegistry[prefix] = config;
}

function formatEntityChip(item, label) {
  return `[ID: #${item.id}] ${label}`;
}

function filterEntitiesPredictive(items, query, getSearchText, getLabel) {
  const q = (query || '').trim().toLowerCase();
  if (!q) return [];

  const matches = (items || []).filter((item) => {
    const label = (getLabel ? getLabel(item) : '').toLowerCase();
    const searchText = getSearchText(item).toLowerCase();
    const idText = String(item.id ?? '').toLowerCase();
    return label.includes(q) || searchText.includes(q) || idText.includes(q);
  });

  matches.sort((a, b) => {
    const aLabel = (getLabel ? getLabel(a) : getSearchText(a)).toLowerCase();
    const bLabel = (getLabel ? getLabel(b) : getSearchText(b)).toLowerCase();
    const aIdx = aLabel.indexOf(q);
    const bIdx = bLabel.indexOf(q);
    if (aIdx !== bIdx) return aIdx - bIdx;
    return aLabel.localeCompare(bLabel, 'es', { sensitivity: 'base' });
  });

  return matches.slice(0, 12);
}

function setEntityPickerOpen(prefix, open) {
  const root = document.getElementById(`${prefix}-picker`);
  if (!root) return;
  const modal = root.closest('.modal-backdrop');
  if (open) {
    const scope = modal
      ? `#${modal.id} .entity-picker[data-entity-picker]`
      : '.entity-picker[data-entity-picker]';
    document.querySelectorAll(scope).forEach((el) => {
      const otherPrefix = el.getAttribute('data-entity-picker');
      if (!otherPrefix || otherPrefix === prefix) return;
      const otherList = document.getElementById(`${otherPrefix}-list`);
      const otherInput = document.getElementById(`${otherPrefix}-input`);
      otherList?.classList.add('hidden');
      otherInput?.setAttribute('aria-expanded', 'false');
      el.classList.remove('is-open');
    });
    root.classList.add('is-open');
  } else {
    root.classList.remove('is-open');
  }
}

function hideEntityPickerList(prefix) {
  const list = document.getElementById(`${prefix}-list`);
  const input = document.getElementById(`${prefix}-input`);
  list?.classList.add('hidden');
  input?.setAttribute('aria-expanded', 'false');
  setEntityPickerOpen(prefix, false);
}

function entityPickerHasExactMatch(items, query, getLabel) {
  const q = (query || '').trim().toLowerCase();
  if (!q) return false;
  return (items || []).some((item) => (getLabel ? getLabel(item) : '').trim().toLowerCase() === q);
}

function renderEntityPickerOptions(prefix, matches, activeIndex = -1, query = '') {
  const list = document.getElementById(`${prefix}-list`);
  const input = document.getElementById(`${prefix}-input`);
  const config = entityPickerRegistry[prefix];
  if (!list || !input || !config) return;

  const q = (query || '').trim();
  const items = config.getItems?.() || [];
  const offerCreate = q
    && config.onCreateRequest
    && !entityPickerHasExactMatch(items, q, config.getLabel);

  if (!matches.length && !offerCreate) {
    list.innerHTML = '<li class="autocomplete-empty">Sin coincidencias</li>';
    list.classList.remove('hidden');
    input.setAttribute('aria-expanded', 'true');
    setEntityPickerOpen(prefix, true);
    return;
  }

  let html = matches.map((item, idx) => {
    const label = config.getOptionLabel ? config.getOptionLabel(item) : config.getLabel(item);
    const subtext = config.getOptionSubtext?.(item);
    const optionClass = config.getOptionClass?.(item) || '';
    const hideId = config.hideOptionId;
    return `<li class="autocomplete-option${idx === activeIndex ? ' is-active' : ''}${optionClass ? ` ${optionClass}` : ''}" role="option" data-picker-prefix="${escapeAttr(prefix)}" data-entity-id="${item.id}">
      <div class="entity-option-content">
        <span class="entity-option-label">${escapeHtml(label)}</span>
        ${subtext ? `<span class="entity-option-subtext">${escapeHtml(subtext)}</span>` : ''}
      </div>
      ${hideId ? '' : `<span class="entity-option-id">#${item.id}</span>`}
    </li>`;
  }).join('');

  if (offerCreate) {
    const createIdx = matches.length;
    const createLabel = config.getCreateOptionLabel
      ? config.getCreateOptionLabel(q)
      : `+ Agregar proveedor "${q}"`;
    html += `<li class="autocomplete-option autocomplete-option--create${createIdx === activeIndex ? ' is-active' : ''}" role="option" data-picker-prefix="${escapeAttr(prefix)}" data-create-query="${escapeAttr(q)}">
      <span class="entity-option-label">${escapeHtml(createLabel)}</span>
    </li>`;
  }

  list.innerHTML = html;
  list.classList.remove('hidden');
  input.setAttribute('aria-expanded', 'true');
  setEntityPickerOpen(prefix, true);
}

function getEntityPickerSelection(prefix) {
  const hidden = document.getElementById(`${prefix}-id`);
  const id = hidden?.value ? parseInt(hidden.value, 10) : null;
  if (!id) return null;
  const config = entityPickerRegistry[prefix];
  const items = config?.getItems?.() || [];
  return items.find((item) => item.id === id) || { id };
}

function setEntityPickerSelection(prefix, item) {
  const hidden = document.getElementById(`${prefix}-id`);
  const chip = document.getElementById(`${prefix}-chip`);
  const chipText = document.getElementById(`${prefix}-chip-text`);
  const root = document.getElementById(`${prefix}-picker`);
  const input = document.getElementById(`${prefix}-input`);
  const config = entityPickerRegistry[prefix];
  if (!hidden || !chip || !chipText || !root || !input || !config) return;

  if (item) {
    hidden.value = item.id;
    chipText.textContent = formatEntityChip(item, config.getLabel(item));
    chip.classList.remove('hidden');
    root.classList.add('is-selected');
    input.value = '';
    config.onSelect?.(item);
  } else {
    hidden.value = '';
    chipText.textContent = '';
    chip.classList.add('hidden');
    root.classList.remove('is-selected');
    config.onClear?.();
  }
  hideEntityPickerList(prefix);
}

function getEntityPickerValue(prefix) {
  const raw = document.getElementById(`${prefix}-id`)?.value;
  if (!raw) return null;
  const id = parseInt(raw, 10);
  return Number.isInteger(id) && id > 0 ? id : null;
}

function resetEntityPicker(prefix) {
  setEntityPickerSelection(prefix, null);
  const input = document.getElementById(`${prefix}-input`);
  if (input) input.value = '';
}

function setEntityPickerDisabled(prefix, disabled) {
  const input = document.getElementById(`${prefix}-input`);
  const root = document.getElementById(`${prefix}-picker`);
  if (input) input.disabled = disabled;
  if (root) root.classList.toggle('is-disabled', disabled);
  if (disabled) hideEntityPickerList(prefix);
}

function initEntityPicker(prefix) {
  const config = entityPickerRegistry[prefix];
  const input = document.getElementById(`${prefix}-input`);
  const list = document.getElementById(`${prefix}-list`);
  if (!config || !input || !list || input.dataset.pickerBound === '1') return;
  input.dataset.pickerBound = '1';

  let activeIndex = -1;
  let currentMatches = [];
  let currentQuery = '';

  input.addEventListener('input', () => {
    if (input.disabled) return;
    if (getEntityPickerValue(prefix)) return;
    activeIndex = -1;
    const q = (input.value || '').trim();
    currentQuery = q;
    if (!q && config.getItemsOnEmptyFocus) {
      currentMatches = config.getItemsOnEmptyFocus();
      if (currentMatches.length) {
        renderEntityPickerOptions(prefix, currentMatches, activeIndex, q);
      } else {
        hideEntityPickerList(prefix);
      }
      return;
    }
    if (!q) {
      hideEntityPickerList(prefix);
      return;
    }
    currentMatches = filterEntitiesPredictive(config.getItems(), q, config.getSearchText, config.getLabel);
    renderEntityPickerOptions(prefix, currentMatches, activeIndex, q);
  });

  input.addEventListener('keydown', (e) => {
    if (list.classList.contains('hidden')) return;
    const offerCreate = currentQuery
      && config.onCreateRequest
      && !entityPickerHasExactMatch(config.getItems?.() || [], currentQuery, config.getLabel);
    const totalOptions = currentMatches.length + (offerCreate ? 1 : 0);
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (!totalOptions) return;
      activeIndex = (activeIndex + 1) % totalOptions;
      renderEntityPickerOptions(prefix, currentMatches, activeIndex, currentQuery);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (!totalOptions) return;
      activeIndex = (activeIndex - 1 + totalOptions) % totalOptions;
      renderEntityPickerOptions(prefix, currentMatches, activeIndex, currentQuery);
    } else if (e.key === 'Enter') {
      if (offerCreate && activeIndex === currentMatches.length) {
        e.preventDefault();
        config.onCreateRequest(currentQuery);
        hideEntityPickerList(prefix);
        return;
      }
      if (activeIndex >= 0 && currentMatches[activeIndex]) {
        e.preventDefault();
        setEntityPickerSelection(prefix, currentMatches[activeIndex]);
      }
    } else if (e.key === 'Escape') {
      hideEntityPickerList(prefix);
    }
  });

  list.addEventListener('mousedown', (e) => {
    const createOption = e.target.closest('[data-create-query]');
    if (createOption && createOption.getAttribute('data-picker-prefix') === prefix) {
      e.preventDefault();
      const query = createOption.getAttribute('data-create-query') || '';
      config.onCreateRequest?.(query);
      hideEntityPickerList(prefix);
      return;
    }
    const option = e.target.closest('[data-entity-id]');
    if (!option || option.getAttribute('data-picker-prefix') !== prefix) return;
    e.preventDefault();
    const id = parseInt(option.getAttribute('data-entity-id'), 10);
    const item = (config.getItems() || []).find((x) => x.id === id);
    if (item) setEntityPickerSelection(prefix, item);
  });

  input.addEventListener('focus', () => {
    if (input.disabled) return;
    if (getEntityPickerValue(prefix)) return;
    const q = (input.value || '').trim();
    currentQuery = q;
    activeIndex = -1;
    if (!q && config.getItemsOnEmptyFocus) {
      currentMatches = config.getItemsOnEmptyFocus();
      if (currentMatches.length) {
        renderEntityPickerOptions(prefix, currentMatches, activeIndex, q);
      }
      return;
    }
    if (!q) return;
    currentMatches = filterEntitiesPredictive(config.getItems(), q, config.getSearchText, config.getLabel);
    renderEntityPickerOptions(prefix, currentMatches, activeIndex, q);
  });

  input.addEventListener('blur', () => {
    setTimeout(() => hideEntityPickerList(prefix), 120);
  });
}

function bindEntityPickerClearButtons() {
  if (document.body.dataset.pickerClearBound === '1') return;
  document.body.dataset.pickerClearBound = '1';
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-picker-clear]');
    if (!btn) return;
    e.preventDefault();
    const prefix = btn.getAttribute('data-picker-clear');
    setEntityPickerSelection(prefix, null);
    document.getElementById(`${prefix}-input`)?.focus();
  });
}

function bindMonetaryInput(input) {
  if (!input || input.dataset.moneyBound === '1') return;
  input.dataset.moneyBound = '1';
  input.addEventListener('wheel', (e) => {
    if (document.activeElement === input) e.preventDefault();
  }, { passive: false });
  input.addEventListener('keydown', (e) => {
    if (e.key === 'e' || e.key === 'E') e.preventDefault();
  });
}

function createEntityPickerHtml(prefix, placeholder) {
  return `
    <div class="entity-picker autocomplete" id="${prefix}-picker" data-entity-picker="${prefix}">
      <input type="hidden" id="${prefix}-id" />
      <div class="autocomplete-chip hidden" id="${prefix}-chip">
        <span class="autocomplete-chip-text" id="${prefix}-chip-text"></span>
        <button type="button" class="autocomplete-chip-clear" data-picker-clear="${prefix}" aria-label="Quitar selección" title="Quitar">×</button>
      </div>
      <div class="autocomplete-input-wrap">
        <input type="text" id="${prefix}-input" placeholder="${escapeHtml(placeholder)}" autocomplete="off" spellcheck="false" role="combobox" aria-expanded="false" aria-controls="${prefix}-list" />
        <ul class="autocomplete-list hidden" id="${prefix}-list" role="listbox"></ul>
      </div>
    </div>`;
}

function bindCantidadInput(input) {
  if (!input || input.dataset.qtyBound === '1') return;
  input.dataset.qtyBound = '1';
  input.setAttribute('min', '1');
  input.setAttribute('step', '1');
  input.setAttribute('inputmode', 'numeric');

  input.addEventListener('keydown', (e) => {
    if (e.key === '-' || e.key === 'e' || e.key === 'E' || e.key === '+' || e.key === '.' || e.key === ',') {
      e.preventDefault();
    }
  });

  input.addEventListener('input', () => {
    const raw = input.value.trim();
    if (raw === '') {
      input.classList.remove('detalle-cantidad-invalid');
      input.setCustomValidity('');
      return;
    }
    const n = parseInt(raw, 10);
    if (!Number.isInteger(n) || n < 1) {
      input.classList.add('detalle-cantidad-invalid');
      input.setCustomValidity('La cantidad debe ser un entero mayor o igual a 1.');
    } else {
      input.classList.remove('detalle-cantidad-invalid');
      input.setCustomValidity('');
      if (String(n) !== raw) input.value = String(n);
    }
  });

  input.addEventListener('wheel', (e) => {
    if (document.activeElement === input) e.preventDefault();
  }, { passive: false });
}

function parseCantidadValid(raw) {
  if (raw === '' || raw == null) {
    return { valid: false, value: null, error: 'Indica una cantidad válida (mínimo 1).' };
  }
  const n = parseInt(String(raw).trim(), 10);
  if (!Number.isInteger(n) || n < 1) {
    return { valid: false, value: null, error: 'La cantidad debe ser un entero mayor o igual a 1.' };
  }
  return { valid: true, value: n, error: null };
}

function getUsuariosParaPicker() {
  const merged = [...(usuariosFiltroCache || []), ...(usuariosData || [])];
  const byId = new Map();
  for (const u of merged) {
    if (u?.id != null) byId.set(u.id, u);
  }
  return [...byId.values()].sort((a, b) => (a.username || '').localeCompare(b.username || '', 'es', { sensitivity: 'base' }));
}

function getBodegasParaPicker() {
  const merged = [...(bodegasCache || []), ...(bodegasData || [])];
  const byId = new Map();
  for (const b of merged) {
    if (b?.id != null) byId.set(b.id, b);
  }
  return sortEntitiesByIdAsc([...byId.values()]);
}

let inventarioMovimientoCache = new Map();

async function loadInventarioMovimientoCache() {
  try {
    const data = await apiFetch('/api/reportes/inventario') || [];
    inventarioMovimientoCache = new Map();
    for (const row of data) {
      const productoId = row.productoId;
      if (productoId == null) continue;
      if (!inventarioMovimientoCache.has(productoId)) {
        inventarioMovimientoCache.set(productoId, []);
      }
      inventarioMovimientoCache.get(productoId).push({
        bodegaId: row.bodegaId,
        bodegaNombre: row.bodegaNombre,
        cantidad: row.cantidad ?? 0,
      });
    }
    for (const items of inventarioMovimientoCache.values()) {
      items.sort((a, b) => (a.bodegaNombre || '').localeCompare(b.bodegaNombre || '', 'es', { sensitivity: 'base' }));
    }
  } catch {
    inventarioMovimientoCache = new Map();
  }
}

function getInventarioProducto(productoId) {
  return inventarioMovimientoCache.get(productoId) || [];
}

function obtenerStockEnBodega(productoId, bodegaId) {
  const inv = getInventarioProducto(productoId).find((i) => String(i.bodegaId) === String(bodegaId));
  return inv?.cantidad ?? 0;
}

function formatProductoInventarioLine(productoId) {
  const items = getInventarioProducto(productoId).filter((i) => i.cantidad > 0);
  if (!items.length) return 'Sin stock en bodegas';
  return items.map((i) => `${i.bodegaNombre} (${i.cantidad} uds)`).join(', ');
}

function getMovimientoProductosSeleccionados() {
  const ids = [];
  document.querySelectorAll('#detalles-container .detalle-row').forEach((row) => {
    const index = row.dataset.index ?? '0';
    const id = getEntityPickerValue(`detalle-producto-${index}`);
    if (id) ids.push(id);
  });
  return [...new Set(ids)];
}

function getBodegasConStockParaProductos(productoIds) {
  const bodegas = getBodegasParaPicker();
  const resultado = [];
  for (const bodega of bodegas) {
    let cantidadTotal = 0;
    for (const productoId of productoIds) {
      cantidadTotal += obtenerStockEnBodega(productoId, bodega.id);
    }
    if (cantidadTotal > 0) {
      resultado.push({ bodegaId: bodega.id, bodegaNombre: bodega.nombre, cantidadTotal });
    }
  }
  return resultado.sort((a, b) => b.cantidadTotal - a.cantidadTotal
    || (a.bodegaNombre || '').localeCompare(b.bodegaNombre || '', 'es', { sensitivity: 'base' }));
}

function getBodegasOrigenPriorizadas() {
  const productoIds = getMovimientoProductosSeleccionados();
  const bodegas = getBodegasParaPicker();
  if (!productoIds.length) return bodegas;

  const conStock = new Set(getBodegasConStockParaProductos(productoIds).map((b) => b.bodegaId));
  return [...bodegas].sort((a, b) => {
    const aTiene = conStock.has(a.id);
    const bTiene = conStock.has(b.id);
    if (aTiene !== bTiene) return aTiene ? -1 : 1;
    const stockA = productoIds.reduce((sum, pid) => sum + obtenerStockEnBodega(pid, a.id), 0);
    const stockB = productoIds.reduce((sum, pid) => sum + obtenerStockEnBodega(pid, b.id), 0);
    if (stockA !== stockB) return stockB - stockA;
    return (a.nombre || '').localeCompare(b.nombre || '', 'es', { sensitivity: 'base' });
  });
}

function formatBodegaStockParaProductos(bodegaId) {
  const productoIds = getMovimientoProductosSeleccionados();
  if (!productoIds.length) return '';
  return productoIds.map((pid) => {
    const producto = productosData.find((p) => p.id === pid);
    const qty = obtenerStockEnBodega(pid, bodegaId);
    return `${producto?.nombre || `Producto #${pid}`}: ${qty} uds`;
  }).join(' · ');
}

function requiereFiltradoPorBodegaOrigen() {
  const tipo = document.getElementById('mov-tipo')?.value || '';
  return tipo === 'SALIDA' || tipo === 'TRANSFERENCIA';
}

function getProductosParaMovimientoPicker() {
  const productos = sortEntitiesByIdAsc(productosData);
  if (!requiereFiltradoPorBodegaOrigen()) {
    return productos;
  }
  const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
  if (!bodegaOrigenId) return [];
  return productos.filter((p) => obtenerStockEnBodega(p.id, bodegaOrigenId) > 0);
}

function formatProductoStockEnBodega(productoId, bodegaId) {
  const qty = obtenerStockEnBodega(productoId, bodegaId);
  if (qty <= 0) return 'Sin stock en bodega origen';
  return `${qty} uds disponibles`;
}

function updateMovimientoDetalleProductosState() {
  const requiereOrigen = requiereFiltradoPorBodegaOrigen();
  const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
  const bloqueado = requiereOrigen && !bodegaOrigenId;

  const hint = document.getElementById('hint-mov-detalle-productos');
  if (hint) {
    if (bloqueado) {
      hint.textContent = 'Selecciona primero la bodega de origen para buscar productos con stock disponible.';
      hint.classList.remove('hidden');
      hint.classList.add('picker-hint-warning');
    } else if (requiereOrigen && bodegaOrigenId) {
      const bodega = getBodegasParaPicker().find((b) => b.id === bodegaOrigenId);
      const count = getProductosParaMovimientoPicker().length;
      hint.textContent = count
        ? `Mostrando ${count} producto(s) con existencia en ${bodega?.nombre || 'la bodega origen'}.`
        : `No hay productos con stock en ${bodega?.nombre || 'la bodega origen'}.`;
      hint.classList.remove('hidden');
      hint.classList.toggle('picker-hint-warning', count === 0);
    } else {
      hint.textContent = '';
      hint.classList.add('hidden');
      hint.classList.remove('picker-hint-warning');
    }
  }

  document.querySelectorAll('#detalles-container .detalle-row').forEach((row) => {
    const index = row.dataset.index ?? '0';
    const prefix = `detalle-producto-${index}`;
    setEntityPickerDisabled(prefix, bloqueado);

    if (requiereOrigen && bodegaOrigenId) {
      const productoId = getEntityPickerValue(prefix);
      if (productoId && obtenerStockEnBodega(productoId, bodegaOrigenId) <= 0) {
        resetEntityPicker(prefix);
      }
    }

    const input = document.getElementById(`${prefix}-input`);
    if (input) {
      input.placeholder = bloqueado
        ? 'Selecciona la bodega de origen…'
        : (requiereOrigen ? 'Buscar productos con stock en origen…' : 'Buscar producto…');
    }
  });

  const btnAdd = document.getElementById('btn-add-detalle');
  if (btnAdd) btnAdd.disabled = bloqueado;

  if (requiereOrigen) {
    updateMovimientoBodegaOrigenContext();
  }
  refreshAllDetalleStockHints();
}

function bodegaTieneStockProductosSeleccionados(bodegaId) {
  return getMovimientoProductosSeleccionados().some((pid) => obtenerStockEnBodega(pid, bodegaId) > 0);
}

function updateMovimientoBodegaFields() {
  const tipo = document.getElementById('mov-tipo')?.value || '';
  const grupoOrigen = document.getElementById('grupo-mov-bodega-origen');
  const grupoDestino = document.getElementById('grupo-mov-bodega-destino');
  const labelOrigen = document.getElementById('label-mov-bodega-origen');
  const labelDestino = document.getElementById('label-mov-bodega-destino');
  const inputOrigen = document.getElementById('mov-bodega-origen-input');
  const inputDestino = document.getElementById('mov-bodega-destino-input');

  if (!grupoOrigen || !grupoDestino) return;

  const showOrigen = tipo === 'SALIDA' || tipo === 'TRANSFERENCIA';
  const showDestino = tipo === 'ENTRADA' || tipo === 'TRANSFERENCIA';

  grupoOrigen.classList.toggle('hidden', !showOrigen);
  grupoDestino.classList.toggle('hidden', !showDestino);

  if (labelOrigen) labelOrigen.textContent = showOrigen ? 'Bodega Origen *' : 'Bodega Origen';
  if (labelDestino) labelDestino.textContent = showDestino ? 'Bodega Destino *' : 'Bodega Destino';

  if (inputOrigen) {
    inputOrigen.disabled = !showOrigen;
    inputOrigen.setAttribute('aria-required', showOrigen ? 'true' : 'false');
  }
  if (inputDestino) {
    inputDestino.disabled = !showDestino;
    inputDestino.setAttribute('aria-required', showDestino ? 'true' : 'false');
  }

  if (!showOrigen) resetEntityPicker('mov-bodega-origen');
  if (!showDestino) resetEntityPicker('mov-bodega-destino');

  if (showOrigen) {
    updateMovimientoBodegaOrigenContext();
  } else {
    const hint = document.getElementById('hint-mov-bodega-origen');
    hint?.classList.add('hidden');
  }
  updateMovimientoDetalleProductosState();
}

function updateMovimientoBodegaOrigenContext() {
  const hint = document.getElementById('hint-mov-bodega-origen');
  const input = document.getElementById('mov-bodega-origen-input');
  const productoIds = getMovimientoProductosSeleccionados();
  if (!hint || !input) return;

  const grupoOrigen = document.getElementById('grupo-mov-bodega-origen');
  if (grupoOrigen?.classList.contains('hidden')) return;

  if (!productoIds.length) {
    hint.textContent = '';
    hint.classList.add('hidden');
    hint.classList.remove('picker-hint-warning');
    input.placeholder = 'Buscar bodega de origen…';
    return;
  }

  const sugerencias = getBodegasConStockParaProductos(productoIds);
  input.placeholder = 'Bodegas con stock del producto…';

  if (!sugerencias.length) {
    hint.textContent = 'Los productos seleccionados no tienen stock en ninguna bodega.';
    hint.classList.remove('hidden');
    hint.classList.add('picker-hint-warning');
    return;
  }

  const texto = sugerencias.slice(0, 3)
    .map((s) => `${s.bodegaNombre} (${s.cantidadTotal} uds)`)
    .join(', ');
  hint.textContent = `Sugerencia: hay stock en ${texto}${sugerencias.length > 3 ? '…' : ''}`;
  hint.classList.remove('hidden', 'picker-hint-warning');
}

function updateDetalleStockHint(row) {
  const hint = row?.querySelector('.detalle-stock-hint');
  if (!hint) return;

  const index = row.dataset.index ?? '0';
  const productoId = getEntityPickerValue(`detalle-producto-${index}`);
  const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
  const cantidadInput = row.querySelector('.detalle-cantidad');
  const tipo = document.getElementById('mov-tipo')?.value || '';

  if (!productoId || !bodegaOrigenId || (tipo && tipo === 'ENTRADA')) {
    hint.textContent = '';
    hint.className = 'detalle-stock-hint hidden';
    return;
  }

  const disponible = obtenerStockEnBodega(productoId, bodegaOrigenId);
  const qty = parseInt(cantidadInput?.value || '', 10);
  hint.classList.remove('hidden');

  if (disponible <= 0) {
    hint.className = 'detalle-stock-hint stock-low';
    hint.textContent = 'Sin stock en la bodega origen seleccionada';
    return;
  }

  if (Number.isInteger(qty) && qty > disponible) {
    hint.className = 'detalle-stock-hint stock-insufficient';
    hint.textContent = `Disponible: ${disponible} uds · solicitas ${qty} (insuficiente)`;
    return;
  }

  hint.className = 'detalle-stock-hint stock-ok';
  hint.textContent = `Disponible en origen: ${disponible} uds`;
}

function refreshAllDetalleStockHints() {
  document.querySelectorAll('#detalles-container .detalle-row').forEach(updateDetalleStockHint);
}

function getProveedoresParaPicker() {
  return sortEntitiesByIdAsc(proveedoresData);
}

function openProveedorRapidoModal(nombrePrefill) {
  const form = document.getElementById('form-proveedor-rapido');
  form?.reset();
  document.getElementById('proveedor-rapido-nombre').value = (nombrePrefill || '').trim();
  document.getElementById('proveedor-rapido-dias').value = '7';
  document.getElementById('proveedor-rapido-error')?.classList.add('hidden');
  openModal('modal-proveedor-rapido');
  setTimeout(() => document.getElementById('proveedor-rapido-nombre')?.focus(), 50);
}

async function crearProveedorRapido(body) {
  const creado = await apiFetch('/api/proveedores', { method: 'POST', body: JSON.stringify(body) });
  proveedoresData = sortEntitiesByIdAsc([...(proveedoresData || []).filter((p) => p.id !== creado.id), creado]);
  return creado;
}

document.getElementById('form-proveedor-rapido')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!isAdmin()) return;

  const nombre = document.getElementById('proveedor-rapido-nombre').value.trim();
  const email = document.getElementById('proveedor-rapido-email').value.trim();
  const diasEntrega = parseInt(document.getElementById('proveedor-rapido-dias').value, 10);

  if (!nombre) {
    showModalError('proveedor-rapido-error', 'El nombre del proveedor es obligatorio.');
    return;
  }
  if (!Number.isInteger(diasEntrega) || diasEntrega < 1 || diasEntrega > 90) {
    showModalError('proveedor-rapido-error', 'Los días de entrega deben estar entre 1 y 90.');
    return;
  }

  setModalLoading('btn-save-proveedor-rapido', true);
  try {
    const creado = await crearProveedorRapido({
      nombre,
      email: email || null,
      diasEntrega,
    });
    setEntityPickerSelection('producto-proveedor', creado);
    closeModal('modal-proveedor-rapido');
    showToast(`Proveedor "${creado.nombre}" creado y seleccionado.`, 'success');
  } catch (err) {
    showModalError('proveedor-rapido-error', err.message || 'No se pudo crear el proveedor.');
  } finally {
    setModalLoading('btn-save-proveedor-rapido', false);
  }
});

function registerProductoModalPickers() {
  registerEntityPicker('producto-bodega', {
    getItems: getBodegasParaPicker,
    getLabel: (b) => b.nombre,
    getSearchText: (b) => `${b.nombre} ${b.id} ${b.ubicacion || ''}`,
    onSelect: () => {
      if (document.getElementById('producto-id')?.value) {
        cargarStockBodegaProducto();
      }
    },
    onClear: () => {
      const stockInput = document.getElementById('producto-stock');
      if (stockInput && document.getElementById('producto-id')?.value) {
        stockInput.value = '';
      }
    },
  });
  registerEntityPicker('producto-proveedor', {
    getItems: getProveedoresParaPicker,
    getLabel: (pr) => pr.nombre,
    getSearchText: (pr) => `${pr.nombre} ${pr.id} ${pr.email || ''} ${pr.diasEntrega || ''}`,
    getOptionSubtext: (pr) => {
      const partes = [];
      if (pr.email) partes.push(pr.email);
      if (pr.diasEntrega != null) partes.push(`${pr.diasEntrega} días de entrega`);
      return partes.join(' · ');
    },
    getCreateOptionLabel: (q) => `+ Agregar proveedor "${q}"`,
    onCreateRequest: (query) => openProveedorRapidoModal(query),
    getItemsOnEmptyFocus: getProveedoresParaPicker,
  });
}

function registerMovimientoModalPickers() {
  registerEntityPicker('mov-usuario', {
    getItems: getUsuariosParaPicker,
    getLabel: (u) => u.username,
    getSearchText: (u) => `${u.username} ${u.id} ${u.rol || ''}`,
  });
  registerEntityPicker('mov-bodega-origen', {
    getItems: getBodegasOrigenPriorizadas,
    getLabel: (b) => b.nombre,
    getSearchText: (b) => `${b.nombre} ${b.id} ${b.ubicacion || ''} ${formatBodegaStockParaProductos(b.id)}`,
    getOptionSubtext: (b) => formatBodegaStockParaProductos(b.id),
    getOptionClass: (b) => (bodegaTieneStockProductosSeleccionados(b.id) ? 'autocomplete-option--suggested' : ''),
    getItemsOnEmptyFocus: () => {
      const productoIds = getMovimientoProductosSeleccionados();
      return productoIds.length ? getBodegasOrigenPriorizadas() : [];
    },
    onSelect: () => {
      updateMovimientoDetalleProductosState();
    },
    onClear: () => {
      updateMovimientoDetalleProductosState();
    },
  });
  registerEntityPicker('mov-bodega-destino', {
    getItems: getBodegasParaPicker,
    getLabel: (b) => b.nombre,
    getSearchText: (b) => `${b.nombre} ${b.id} ${b.ubicacion || ''}`,
  });
}

function registerDetalleProductoPicker(index) {
  const prefix = `detalle-producto-${index}`;
  registerEntityPicker(prefix, {
    getItems: getProductosParaMovimientoPicker,
    getLabel: (p) => p.nombre,
    getSearchText: (p) => {
      const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
      const stockLine = requiereFiltradoPorBodegaOrigen() && bodegaOrigenId
        ? formatProductoStockEnBodega(p.id, bodegaOrigenId)
        : formatProductoInventarioLine(p.id);
      return `${p.nombre} ${p.id} ${p.categoria || ''} ${stockLine}`;
    },
    getOptionLabel: (p) => {
      const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
      if (requiereFiltradoPorBodegaOrigen() && bodegaOrigenId) {
        return `${p.nombre} — ${formatProductoStockEnBodega(p.id, bodegaOrigenId)}`;
      }
      const stockLine = formatProductoInventarioLine(p.id);
      return stockLine === 'Sin stock en bodegas' ? p.nombre : `${p.nombre} — ${stockLine}`;
    },
    getItemsOnEmptyFocus: () => {
      if (!requiereFiltradoPorBodegaOrigen()) return [];
      const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
      return bodegaOrigenId ? getProductosParaMovimientoPicker() : [];
    },
    onSelect: () => {
      updateMovimientoBodegaOrigenContext();
      const row = document.querySelector(`#detalles-container .detalle-row[data-index="${index}"]`);
      if (row) updateDetalleStockHint(row);
    },
    onClear: () => {
      updateMovimientoBodegaOrigenContext();
      const row = document.querySelector(`#detalles-container .detalle-row[data-index="${index}"]`);
      if (row) updateDetalleStockHint(row);
    },
  });
  return prefix;
}

function mountDetalleProductoPicker(wrap, index) {
  const prefix = registerDetalleProductoPicker(index);
  wrap.innerHTML = createEntityPickerHtml(prefix, 'Buscar producto…');
  initEntityPicker(prefix);
}

function initDetalleRow(row, index) {
  const wrap = row.querySelector('[data-detalle-producto-wrap]');
  if (wrap) {
    wrap.setAttribute('data-detalle-producto-wrap', String(index));
    mountDetalleProductoPicker(wrap, index);
  }
  const cantidadInput = row.querySelector('.detalle-cantidad');
  bindCantidadInput(cantidadInput);
  cantidadInput?.addEventListener('input', () => updateDetalleStockHint(row));
}

function buildDetalleRowHtml(index) {
  return `
    <div class="detalle-row" data-index="${index}">
      <div class="detalle-producto-col">
        <div class="detalle-producto-wrap" data-detalle-producto-wrap="${index}"></div>
        <span class="detalle-stock-hint hidden" aria-live="polite"></span>
      </div>
      <input type="number" placeholder="Cantidad" class="detalle-cantidad" min="1" step="1" inputmode="numeric" aria-label="Cantidad" />
      <button type="button" class="btn-remove-detalle" onclick="removeDetalle(this)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
      </button>
    </div>`;
}

function resetDetallesContainer() {
  const container = document.getElementById('detalles-container');
  if (!container) return;
  container.innerHTML = buildDetalleRowHtml(0);
  initDetalleRow(container.firstElementChild, 0);
}

async function ensureMovimientoModalData() {
  const tasks = [];
  if (!usuariosFiltroCache.length && !usuariosData.length) {
    tasks.push(populateUsuariosAutocomplete());
  }
  if (!bodegasCache.length && !bodegasData.length) {
    tasks.push(apiFetch('/api/bodegas').then((data) => { bodegasCache = sortEntitiesByIdAsc(data || []); }).catch(() => {}));
  }
  if (!productosData.length) {
    tasks.push(apiFetch('/api/productos').then((data) => { productosData = sortEntitiesByIdAsc(data || []); }).catch(() => {}));
  }
  tasks.push(loadInventarioMovimientoCache());
  if (tasks.length) await Promise.all(tasks);
}

function initMovimientoModalPickers() {
  registerMovimientoModalPickers();
  registerProductoModalPickers();
  ['mov-usuario', 'mov-bodega-origen', 'mov-bodega-destino', 'producto-bodega', 'producto-proveedor'].forEach((prefix) => {
    initEntityPicker(prefix);
  });
  bindEntityPickerClearButtons();
  bindMonetaryInput(document.getElementById('producto-precio'));
  const firstRow = document.querySelector('#detalles-container .detalle-row');
  if (firstRow) initDetalleRow(firstRow, 0);
}

initMovimientoModalPickers();

/* ─────────────────────────────────────────────────────
   MOVIMIENTOS
   ───────────────────────────────────────────────────── */
let movimientosData = [];
let usuariosFiltroCache = [];
let movimientoUsuarioSeleccionado = null;

function setMovimientosFiltroError(message) {
  const el = document.getElementById('movimientos-filtro-error');
  if (!el) return;
  if (!message) {
    el.textContent = '';
    el.classList.add('hidden');
    return;
  }
  el.textContent = message;
  el.classList.remove('hidden');
}

function syncClearableInput(input) {
  if (!input) return;
  const wrap = input.closest('.clearable-input');
  if (!wrap) return;
  wrap.classList.toggle('has-value', Boolean(input.value));
}

function bindClearableInputs(onCleared) {
  document.querySelectorAll('[data-clear-input]').forEach((btn) => {
    if (btn.dataset.boundClear === '1') return;
    btn.dataset.boundClear = '1';
    const inputId = btn.getAttribute('data-clear-input');
    const input = document.getElementById(inputId);
    if (!input) return;
    syncClearableInput(input);
    input.addEventListener('input', () => syncClearableInput(input));
    input.addEventListener('change', () => syncClearableInput(input));
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      input.value = '';
      syncClearableInput(input);
      input.dispatchEvent(new Event('change', { bubbles: true }));
      input.dispatchEvent(new Event('input', { bubbles: true }));
      if (typeof onCleared === 'function') onCleared(inputId, input);
    });
  });
}

function collectUsernamesForAutocomplete() {
  const fromMovimientos = movimientosData
    .map((m) => m.usuario?.username || (typeof m.usuario === 'string' ? m.usuario : null))
    .filter(Boolean);
  const fromUsuarios = usuariosFiltroCache.map((u) => u.username).filter(Boolean);
  return [...new Set([...fromUsuarios, ...fromMovimientos])]
    .sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }));
}

function filterUsernamesPredictive(query) {
  const q = (query || '').trim().toLowerCase();
  if (!q) return [];
  const all = collectUsernamesForAutocomplete();
  const starts = [];
  const contains = [];
  for (const name of all) {
    const lower = name.toLowerCase();
    if (lower.startsWith(q)) starts.push(name);
    else if (lower.includes(q)) contains.push(name);
  }
  return [...starts, ...contains].slice(0, 8);
}

function setMovimientoUsuarioSeleccionado(username) {
  movimientoUsuarioSeleccionado = username || null;
  const root = document.getElementById('filter-mov-usuario-ac');
  const chip = document.getElementById('filter-mov-usuario-chip');
  const chipText = document.getElementById('filter-mov-usuario-chip-text');
  const input = document.getElementById('filter-mov-usuario');
  const list = document.getElementById('filter-mov-usuario-list');
  if (!root || !chip || !chipText || !input) return;

  if (movimientoUsuarioSeleccionado) {
    chipText.textContent = movimientoUsuarioSeleccionado;
    chip.classList.remove('hidden');
    root.classList.add('is-selected');
    input.value = '';
    input.setAttribute('aria-expanded', 'false');
    list?.classList.add('hidden');
  } else {
    chipText.textContent = '';
    chip.classList.add('hidden');
    root.classList.remove('is-selected');
    input.setAttribute('aria-expanded', 'false');
    list?.classList.add('hidden');
  }
}

function escapeAttr(str) {
  return escapeHtml(str).replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function renderUsuarioAutocompleteOptions(matches, activeIndex = -1) {
  const list = document.getElementById('filter-mov-usuario-list');
  const input = document.getElementById('filter-mov-usuario');
  if (!list || !input) return;
  if (!matches.length) {
    list.innerHTML = '<li class="autocomplete-empty">Sin coincidencias</li>';
    list.classList.remove('hidden');
    input.setAttribute('aria-expanded', 'true');
    return;
  }
  list.innerHTML = matches.map((name, idx) =>
    `<li class="autocomplete-option${idx === activeIndex ? ' is-active' : ''}" role="option" data-username="${escapeAttr(name)}">${escapeHtml(name)}</li>`
  ).join('');
  list.classList.remove('hidden');
  input.setAttribute('aria-expanded', 'true');
}

function hideUsuarioAutocomplete() {
  const list = document.getElementById('filter-mov-usuario-list');
  const input = document.getElementById('filter-mov-usuario');
  list?.classList.add('hidden');
  if (list) {
    list.style.left = '';
    list.style.top = '';
    list.style.width = '';
  }
  input?.setAttribute('aria-expanded', 'false');
}

function initUsuarioAutocompleteMovimientos() {
  const input = document.getElementById('filter-mov-usuario');
  const list = document.getElementById('filter-mov-usuario-list');
  const clearBtn = document.getElementById('filter-mov-usuario-clear');
  if (!input || !list || input.dataset.acBound === '1') return;
  input.dataset.acBound = '1';
  let activeIndex = -1;
  let currentMatches = [];

  input.addEventListener('input', () => {
    if (movimientoUsuarioSeleccionado) return;
    activeIndex = -1;
    currentMatches = filterUsernamesPredictive(input.value);
    if (!(input.value || '').trim()) {
      hideUsuarioAutocomplete();
      return;
    }
    renderUsuarioAutocompleteOptions(currentMatches, activeIndex);
  });

  input.addEventListener('keydown', (e) => {
    if (list.classList.contains('hidden')) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (!currentMatches.length) return;
      activeIndex = (activeIndex + 1) % currentMatches.length;
      renderUsuarioAutocompleteOptions(currentMatches, activeIndex);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (!currentMatches.length) return;
      activeIndex = (activeIndex - 1 + currentMatches.length) % currentMatches.length;
      renderUsuarioAutocompleteOptions(currentMatches, activeIndex);
    } else if (e.key === 'Enter') {
      if (activeIndex >= 0 && currentMatches[activeIndex]) {
        e.preventDefault();
        setMovimientoUsuarioSeleccionado(currentMatches[activeIndex]);
        aplicarFiltrosMovimientos();
      }
    } else if (e.key === 'Escape') {
      hideUsuarioAutocomplete();
    }
  });

  list.addEventListener('mousedown', (e) => {
    const option = e.target.closest('[data-username]');
    if (!option) return;
    e.preventDefault();
    setMovimientoUsuarioSeleccionado(option.getAttribute('data-username'));
    aplicarFiltrosMovimientos();
  });

  input.addEventListener('blur', () => {
    setTimeout(() => hideUsuarioAutocomplete(), 120);
  });

  clearBtn?.addEventListener('click', (e) => {
    e.preventDefault();
    setMovimientoUsuarioSeleccionado(null);
    document.getElementById('filter-mov-usuario')?.focus();
    aplicarFiltrosMovimientos();
  });
}

async function populateFiltroBodegasMovimiento() {
  const select = document.getElementById('filter-bodega-movimiento');
  if (!select) return;
  try {
    const bodegas = bodegasCache.length ? bodegasCache : sortEntitiesByIdAsc(await apiFetch('/api/bodegas') || []);
    if (!bodegasCache.length) bodegasCache = bodegas;
    fillSelectOptions(
      select,
      bodegas,
      'Todas las bodegas',
      (b) => `<option value="${b.id}">${escapeHtml(b.nombre)}</option>`
    );
  } catch {
    /* el listado sigue usable sin el combo de bodegas */
  }
}

async function populateUsuariosAutocomplete() {
  try {
    usuariosFiltroCache = await apiFetch('/api/usuarios') || [];
  } catch {
    usuariosFiltroCache = [];
  }
}

async function loadMovimientos() {
  try {
    await Promise.all([populateFiltroBodegasMovimiento(), populateUsuariosAutocomplete()]);
    movimientosData = await apiFetch('/api/movimientos') || [];
    initUsuarioAutocompleteMovimientos();
    bindClearableInputs();
    aplicarFiltrosMovimientos();
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-movimientos', 7, e.message);
    setApiStatus(false);
  }
}

function renderMovimientosTable(data, filtrosActivos = false) {
  const tbody = document.getElementById('tbody-movimientos');
  if (!tbody) return;
  if (!data.length) {
    const msg = filtrosActivos
      ? 'No se encontraron movimientos'
      : 'No hay movimientos registrados';
    tbody.innerHTML = `<tr><td colspan="7"><div class="table-loading">${msg}</div></td></tr>`;
    return;
  }
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

function leerFiltrosMovimientos() {
  return {
    tipo: document.getElementById('filter-tipo-movimiento')?.value || '',
    bodegaId: document.getElementById('filter-bodega-movimiento')?.value || '',
    fechaInicioRaw: document.getElementById('filter-mov-fecha-inicio')?.value || '',
    fechaFinRaw: document.getElementById('filter-mov-fecha-fin')?.value || '',
    idRaw: document.getElementById('filter-mov-id')?.value?.trim() || '',
    usuario: movimientoUsuarioSeleccionado || '',
  };
}

function hayFiltrosMovimientosActivos(f) {
  return Boolean(f.tipo || f.bodegaId || f.fechaInicioRaw || f.fechaFinRaw || f.idRaw || f.usuario);
}

function validarRangoFechasMovimientos(fechaInicioRaw, fechaFinRaw) {
  if (!fechaInicioRaw && !fechaFinRaw) return true;
  if (fechaInicioRaw && fechaFinRaw) {
    const inicio = new Date(fechaInicioRaw);
    const fin = new Date(fechaFinRaw);
    if (Number.isNaN(inicio.getTime()) || Number.isNaN(fin.getTime())) {
      setMovimientosFiltroError('Las fechas del rango no son válidas. Revisa el formato e inténtalo de nuevo.');
      return false;
    }
    if (inicio > fin) {
      setMovimientosFiltroError('La fecha de inicio no puede ser posterior a la fecha de fin. Ajusta el rango e inténtalo de nuevo.');
      return false;
    }
  }
  setMovimientosFiltroError('');
  return true;
}

function aplicarFiltrosMovimientos() {
  const f = leerFiltrosMovimientos();
  if (!validarRangoFechasMovimientos(f.fechaInicioRaw, f.fechaFinRaw)) {
    renderMovimientosTable([], true);
    return;
  }

  let data = movimientosData;
  if (f.tipo) {
    data = data.filter((m) => m.tipoMovimiento === f.tipo);
  }
  if (f.bodegaId) {
    const bodegaId = Number(f.bodegaId);
    data = data.filter((m) =>
      m.bodegaOrigen?.id === bodegaId || m.bodegaDestino?.id === bodegaId
    );
  }
  if (f.fechaInicioRaw) {
    const inicio = new Date(f.fechaInicioRaw).getTime();
    data = data.filter((m) => {
      const t = new Date(m.fecha).getTime();
      return !Number.isNaN(t) && t >= inicio;
    });
  }
  if (f.fechaFinRaw) {
    const fin = new Date(f.fechaFinRaw).getTime();
    data = data.filter((m) => {
      const t = new Date(m.fecha).getTime();
      return !Number.isNaN(t) && t <= fin;
    });
  }
  if (f.idRaw) {
    const id = Number(f.idRaw);
    data = data.filter((m) => m.id === id);
  }
  if (f.usuario) {
    const selected = f.usuario.toLowerCase();
    data = data.filter((m) =>
      (m.usuario?.username || String(m.usuario || '')).toLowerCase() === selected
    );
  }

  setMovimientosFiltroError('');
  renderMovimientosTable(data, hayFiltrosMovimientosActivos(f));
}

['filter-tipo-movimiento', 'filter-bodega-movimiento', 'filter-mov-fecha-inicio', 'filter-mov-fecha-fin']
  .forEach((id) => {
    document.getElementById(id)?.addEventListener('change', aplicarFiltrosMovimientos);
  });
document.getElementById('filter-mov-id')?.addEventListener('input', aplicarFiltrosMovimientos);

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

document.getElementById('btn-nuevo-movimiento')?.addEventListener('click', async () => {
  await ensureMovimientoModalData();
  document.getElementById('form-movimiento').reset();
  resetEntityPicker('mov-usuario');
  resetEntityPicker('mov-bodega-origen');
  resetEntityPicker('mov-bodega-destino');
  resetDetallesContainer();
  updateMovimientoBodegaFields();
  document.getElementById('movimiento-error').classList.add('hidden');
  openModal('modal-movimiento');
});

document.getElementById('mov-tipo')?.addEventListener('change', updateMovimientoBodegaFields);

document.getElementById('btn-add-detalle')?.addEventListener('click', () => {
  const container = document.getElementById('detalles-container');
  const idx = container.children.length;
  const div = document.createElement('div');
  div.innerHTML = buildDetalleRowHtml(idx).trim();
  const row = div.firstElementChild;
  container.appendChild(row);
  initDetalleRow(row, idx);
  updateMovimientoDetalleProductosState();
});

function removeDetalle(btn) {
  const row = btn.closest('.detalle-row');
  const container = document.getElementById('detalles-container');
  if (container.children.length > 1) {
    row.remove();
    updateMovimientoBodegaOrigenContext();
    refreshAllDetalleStockHints();
  }
}

document.getElementById('form-movimiento')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!isAdmin()) {
    showToast('Solo un ADMIN puede registrar movimientos.', 'error');
    return;
  }
  const tipo = document.getElementById('mov-tipo').value;
  const usuarioId = getEntityPickerValue('mov-usuario');
  const bodegaOrigenId = getEntityPickerValue('mov-bodega-origen');
  const bodegaDestinoId = getEntityPickerValue('mov-bodega-destino');

  if (!tipo) {
    showModalError('movimiento-error', 'Selecciona el tipo de movimiento.');
    return;
  }
  if (!usuarioId) {
    showModalError('movimiento-error', 'Selecciona un usuario válido desde el buscador.');
    return;
  }

  if (tipo === 'ENTRADA' && !bodegaDestinoId) {
    showModalError('movimiento-error', 'Selecciona la bodega destino.');
    return;
  }
  if (tipo === 'SALIDA' && !bodegaOrigenId) {
    showModalError('movimiento-error', 'Selecciona la bodega origen.');
    return;
  }
  if (tipo === 'TRANSFERENCIA') {
    if (!bodegaOrigenId) {
      showModalError('movimiento-error', 'Selecciona la bodega origen.');
      return;
    }
    if (!bodegaDestinoId) {
      showModalError('movimiento-error', 'Selecciona la bodega destino.');
      return;
    }
    if (bodegaOrigenId === bodegaDestinoId) {
      showModalError('movimiento-error', 'La bodega de origen y destino no pueden ser la misma.');
      return;
    }
  }

  const detalleRows = document.querySelectorAll('#detalles-container .detalle-row');
  const detalles = [];
  for (const row of detalleRows) {
    const index = row.dataset.index ?? '0';
    const productoId = getEntityPickerValue(`detalle-producto-${index}`);
    const cantidadInput = row.querySelector('.detalle-cantidad');
    const qtyRaw = cantidadInput?.value ?? '';
    const qtyCheck = parseCantidadValid(qtyRaw);

    if (productoId && !qtyCheck.valid) {
      cantidadInput?.classList.add('detalle-cantidad-invalid');
      showModalError('movimiento-error', qtyCheck.error);
      return;
    }
    if (!productoId && qtyRaw.trim()) {
      showModalError('movimiento-error', 'Selecciona el producto correspondiente a la cantidad indicada.');
      return;
    }
    if (productoId && qtyCheck.valid) {
      detalles.push({ producto: { id: productoId }, cantidad: qtyCheck.value });
    }
  }

  if (!detalles.length) {
    showModalError('movimiento-error', 'Agrega al menos un producto con cantidad válida (mínimo 1).');
    return;
  }

  if ((tipo === 'SALIDA' || tipo === 'TRANSFERENCIA') && bodegaOrigenId) {
    for (const row of detalleRows) {
      const index = row.dataset.index ?? '0';
      const productoId = getEntityPickerValue(`detalle-producto-${index}`);
      const qtyRaw = row.querySelector('.detalle-cantidad')?.value ?? '';
      const qtyCheck = parseCantidadValid(qtyRaw);
      if (!productoId || !qtyCheck.valid) continue;
      const disponible = obtenerStockEnBodega(productoId, bodegaOrigenId);
      if (qtyCheck.value > disponible) {
        const producto = productosData.find((p) => p.id === productoId);
        updateDetalleStockHint(row);
        showModalError(
          'movimiento-error',
          `Stock insuficiente para "${producto?.nombre || `producto #${productoId}`}": disponible ${disponible} uds, solicitado ${qtyCheck.value}.`
        );
        return;
      }
    }
  }

  const body = {
    tipoMovimiento: tipo,
    usuario: { id: usuarioId },
    detalles,
  };
  if (tipo === 'ENTRADA') {
    body.bodegaDestino = { id: bodegaDestinoId };
  } else if (tipo === 'SALIDA') {
    body.bodegaOrigen = { id: bodegaOrigenId };
  } else if (tipo === 'TRANSFERENCIA') {
    body.bodegaOrigen = { id: bodegaOrigenId };
    body.bodegaDestino = { id: bodegaDestinoId };
  }

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
const REPORTE_FILTROS_POR_TIPO = {
  inventario: ['bodega', 'producto', 'categoria'],
  movimientos: ['bodega', 'producto', 'tipo-movimiento', 'fechas'],
  auditoria: ['entidad', 'fechas'],
};

function toIsoLocal(value) {
  if (!value) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return d.toISOString();
}

function setReporteFiltroError(message) {
  const el = document.getElementById('reporte-filtro-error');
  if (!el) return;
  if (!message) {
    el.textContent = '';
    el.classList.add('hidden');
    return;
  }
  el.textContent = message;
  el.classList.remove('hidden');
}

function actualizarVisibilidadFiltrosReporte() {
  const tipo = document.getElementById('reporte-tipo')?.value || 'inventario';
  const visibles = new Set(REPORTE_FILTROS_POR_TIPO[tipo] || []);
  document.querySelectorAll('#page-reportes [data-filter]').forEach((field) => {
    const key = field.getAttribute('data-filter');
    field.classList.toggle('hidden', !visibles.has(key));
  });
  setReporteFiltroError('');
}

function fillSelectOptions(select, items, allLabel, mapFn) {
  if (!select) return;
  const current = select.value;
  select.innerHTML = `<option value="">${escapeHtml(allLabel)}</option>` + items.map(mapFn).join('');
  if (current && [...select.options].some((o) => o.value === current)) {
    select.value = current;
  }
}

async function initReportes() {
  actualizarVisibilidadFiltrosReporte();
  bindClearableInputs();
  try {
    const [bodegas, productos] = await Promise.all([
      apiFetch('/api/bodegas'),
      apiFetch('/api/productos'),
    ]);
    bodegasCache = sortEntitiesByIdAsc(bodegas || bodegasCache);

    fillSelectOptions(
      document.getElementById('reporte-bodega'),
      bodegas || [],
      'Todas las bodegas',
      (b) => `<option value="${b.id}">${escapeHtml(b.nombre)}</option>`
    );

    const listaProductos = productos || [];
    fillSelectOptions(
      document.getElementById('reporte-producto'),
      listaProductos,
      'Todos los productos',
      (p) => `<option value="${p.id}">${escapeHtml(p.nombre)}</option>`
    );

    const categorias = [...new Set(listaProductos.map((p) => p.categoria).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b, 'es'));
    fillSelectOptions(
      document.getElementById('reporte-categoria'),
      categorias,
      'Todas las categorías',
      (c) => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`
    );
  } catch (e) {
    showToast(e.message || 'No se pudieron cargar los filtros de reportes', 'error');
  }
}

document.getElementById('reporte-tipo')?.addEventListener('change', actualizarVisibilidadFiltrosReporte);
document.getElementById('btn-generar-reporte')?.addEventListener('click', generarReporte);

function validarRangoFechasReporte(fechaInicioRaw, fechaFinRaw) {
  if (!fechaInicioRaw && !fechaFinRaw) return true;
  if (fechaInicioRaw && fechaFinRaw) {
    const inicio = new Date(fechaInicioRaw);
    const fin = new Date(fechaFinRaw);
    if (Number.isNaN(inicio.getTime()) || Number.isNaN(fin.getTime())) {
      setReporteFiltroError('Las fechas del rango no son válidas. Revisa el formato e inténtalo de nuevo.');
      return false;
    }
    if (inicio > fin) {
      setReporteFiltroError('La fecha de inicio no puede ser posterior a la fecha de fin. Ajusta el rango e inténtalo de nuevo.');
      return false;
    }
  }
  setReporteFiltroError('');
  return true;
}

async function generarReporte() {
  const tipo = document.getElementById('reporte-tipo').value;
  const bodegaId = document.getElementById('reporte-bodega').value;
  const productoId = document.getElementById('reporte-producto').value;
  const categoria = document.getElementById('reporte-categoria').value;
  const tipoMov = document.getElementById('reporte-tipo-movimiento').value;
  const entidad = document.getElementById('reporte-entidad').value;
  const fechaInicioRaw = document.getElementById('reporte-fecha-inicio').value;
  const fechaFinRaw = document.getElementById('reporte-fecha-fin').value;
  const tbody = document.getElementById('tbody-reportes');
  const thead = document.getElementById('thead-reportes');

  if ((tipo === 'movimientos' || tipo === 'auditoria')
      && !validarRangoFechasReporte(fechaInicioRaw, fechaFinRaw)) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="table-loading">Corrige el rango de fechas para generar el reporte</div></td></tr>`;
    return;
  }

  const fechaInicio = toIsoLocal(fechaInicioRaw);
  const fechaFin = toIsoLocal(fechaFinRaw);
  setReporteFiltroError('');
  tbody.innerHTML = `<tr class="loading-row"><td colspan="8"><div class="table-loading">Generando reporte...</div></td></tr>`;

  try {
    if (tipo === 'inventario') {
      const params = new URLSearchParams();
      if (bodegaId) params.set('bodegaId', bodegaId);
      if (productoId) params.set('productoId', productoId);
      if (categoria) params.set('categoria', categoria);
      const q = params.toString();
      const data = await apiFetch(`/api/reportes/inventario${q ? '?' + q : ''}`) || [];
      thead.innerHTML = `<tr><th>Bodega</th><th>Producto</th><th>Categoría</th><th>Cantidad</th></tr>`;
      if (!data.length) {
        tbody.innerHTML = `<tr><td colspan="4"><div class="table-loading">Sin registros de inventario para los filtros</div></td></tr>`;
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
      if (bodegaId) params.set('bodegaId', bodegaId);
      if (productoId) params.set('productoId', productoId);
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
    setReporteFiltroError(err.message || 'No se pudo generar el reporte');
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

function aplicarFiltrosUsuarios() {
  const q = (document.getElementById('search-usuarios')?.value || '').toLowerCase().trim();
  const rol = document.getElementById('filter-rol-usuario')?.value || '';
  let data = usuariosData.slice().sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
  if (rol) {
    data = data.filter((u) => u.rol === rol);
  }
  if (q) {
    data = data.filter((u) => (u.username || '').toLowerCase().includes(q));
  }
  renderUsuariosTable(data, Boolean(q || rol));
}

async function loadUsuarios() {
  try {
    usuariosData = await apiFetch('/api/usuarios') || [];
    aplicarFiltrosUsuarios();
    setApiStatus(true);
  } catch (e) {
    showTableError('tbody-usuarios', 4, e.message);
    setApiStatus(false);
  }
}

function renderUsuariosTable(data, filtrosActivos = false) {
  const tbody = document.getElementById('tbody-usuarios');
  if (!tbody) return;
  if (!data.length) {
    const msg = filtrosActivos ? 'No se encontraron usuarios' : 'No hay usuarios registrados';
    tbody.innerHTML = `<tr><td colspan="4"><div class="table-loading">${msg}</div></td></tr>`;
    return;
  }
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
      <td><span class="badge ${u.rol === 'ADMIN' ? 'badge-admin' : u.rol === 'AGENTE' ? 'badge-agente' : 'badge-empleado'}">${u.rol === 'ADMIN' ? 'Admin' : u.rol === 'AGENTE' ? 'Agente' : 'Empleado'}</span></td>
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

document.getElementById('search-usuarios')?.addEventListener('input', aplicarFiltrosUsuarios);
document.getElementById('filter-rol-usuario')?.addEventListener('change', aplicarFiltrosUsuarios);

document.getElementById('btn-nuevo-usuario')?.addEventListener('click', () => {
  document.getElementById('form-usuario').reset();
  document.getElementById('usuario-id').value = '';
  document.getElementById('modal-usuario-title').textContent = 'Nuevo Usuario';
  document.getElementById('usuario-error').classList.add('hidden');
  document.getElementById('group-password').style.display = '';
  openModal('modal-usuario');
});

function editUsuario(id) {
  if (!isAdmin()) return;
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
  if (!isAdmin()) return;
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
  if (!isAdmin()) return;
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
    resolveCurrentRole().then(() => enterApp());
  }
})();