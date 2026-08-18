import { api } from '../api.js';
import { showConfirmModal } from '../helpers.js';

const STORAGE_KEY = 'inplabel_user';
const LAST_ACTIVITY_KEY = 'inplabel_last_activity';
const INACTIVITY_TIMEOUT_MS = 2 * 60 * 60 * 1000; // 2 horas (7,200,000 ms)

export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

export function isAuthenticated() {
  return getCurrentUser() !== null;
}

export function isAdmin() {
  const user = getCurrentUser();
  return user && (user.rol === 'ADMIN' || user.rol === 'ADMINISTRADOR');
}

export function canDelete() {
  return isAdmin();
}

export function recordActivity() {
  if (isAuthenticated()) {
    localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()));
  }
}

export function checkSessionTimeout() {
  if (!isAuthenticated()) return false;

  const rawLast = localStorage.getItem(LAST_ACTIVITY_KEY);
  if (!rawLast) {
    recordActivity();
    return false;
  }

  const lastTime = parseInt(rawLast, 10);
  const elapsed = Date.now() - lastTime;

  if (elapsed >= INACTIVITY_TIMEOUT_MS) {
    // Sesión expirada automáticamente por inactividad (más de 2 horas)
    console.warn("Sesión expirada automáticamente tras 2 horas de inactividad.");
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(LAST_ACTIVITY_KEY);

    if (window.app && typeof window.app.navigateTo === 'function') {
      window.app.updateUserUI();
      window.app.navigateTo('login');
      setTimeout(() => {
        showLoginAlert('Su sesión ha expirado automáticamente por inactividad (más de 2 horas sin uso). Inicie sesión nuevamente.', 'warning');
      }, 250);
    } else {
      window.location.reload();
    }
    return true;
  }

  return false;
}

export function initInactivityTracker() {
  // Registrar actividad inicial
  recordActivity();

  // Throttling para registrar actividad como máximo 1 vez cada 30 segundos
  let lastThrottled = 0;
  const onUserActivity = () => {
    const now = Date.now();
    if (now - lastThrottled > 30000) {
      lastThrottled = now;
      recordActivity();
    }
  };

  const activityEvents = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
  activityEvents.forEach(evt => {
    window.addEventListener(evt, onUserActivity, { passive: true });
  });

  // Chequeo periódico cada 60 segundos
  setInterval(() => {
    checkSessionTimeout();
  }, 60000);

  // Chequeo al volver a la pestaña
  window.addEventListener('focus', () => {
    checkSessionTimeout();
  });
}

export async function submitLogin(e) {
  if (e) e.preventDefault();

  const userEl = document.getElementById('loginUsername');
  const passEl = document.getElementById('loginPassword');
  const btnSubmit = document.getElementById('btnLoginSubmit');

  const username = userEl ? userEl.value.trim() : '';
  const password = passEl ? passEl.value : '';

  if (!username || !password) {
    showLoginAlert('Por favor ingrese usuario y contraseña.', 'warning');
    return;
  }

  if (btnSubmit) {
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Validando acceso...';
  }

  try {
    const res = await api.login(username, password);

    if (res && res.success && res.user) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(res.user));
      recordActivity();
      showLoginAlert('Acceso correcto. Redirigiendo...', 'success');

      setTimeout(() => {
        if (window.app && typeof window.app.navigateTo === 'function') {
          window.app.updateUserUI();
          window.app.navigateTo('dashboard');
        } else {
          window.location.reload();
        }
      }, 400);
    } else {
      showLoginAlert(res?.message || 'Usuario o contraseña incorrectos.', 'danger');
    }
  } catch (err) {
    showLoginAlert('Error de conexión con el servidor de autenticación.', 'danger');
  } finally {
    if (btnSubmit) {
      btnSubmit.disabled = false;
      btnSubmit.innerHTML = '<i class="bi bi-box-arrow-in-right me-2 fs-5"></i> Iniciar Sesión';
    }
  }
}

export async function confirmLogout() {
  const confirmed = await showConfirmModal({
    title: '¿Cerrar Sesión?',
    message: '¿Está seguro de que desea salir del sistema?',
    icon: 'bi-box-arrow-right',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
    confirmText: 'Cerrar Sesión',
    confirmBtnClass: 'btn-danger',
    cancelText: 'Cancelar'
  });

  if (confirmed) {
    logout();
  }
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY);
  localStorage.removeItem(LAST_ACTIVITY_KEY);
  if (window.app && typeof window.app.navigateTo === 'function') {
    window.app.updateUserUI();
    window.app.navigateTo('login');
  } else {
    window.location.reload();
  }
}

export function showLoginAlert(msg, type = 'danger') {
  const alertEl = document.getElementById('loginAlert');
  if (!alertEl) return;
  alertEl.className = `alert alert-${type} py-2 px-3 small d-flex align-items-center gap-2 mb-3`;
  alertEl.innerHTML = `<i class="bi ${type === 'danger' ? 'bi-exclamation-triangle-fill' : type === 'warning' ? 'bi-exclamation-circle-fill' : 'bi-check-circle-fill'}"></i> <span>${msg}</span>`;
  alertEl.classList.remove('d-none');
}

export function togglePasswordVisibility() {
  const passEl = document.getElementById('loginPassword');
  const iconEl = document.getElementById('togglePassIcon');
  if (!passEl) return;
  if (passEl.type === 'password') {
    passEl.type = 'text';
    if (iconEl) iconEl.className = 'bi bi-eye-slash-fill';
  } else {
    passEl.type = 'password';
    if (iconEl) iconEl.className = 'bi bi-eye-fill';
  }
}
