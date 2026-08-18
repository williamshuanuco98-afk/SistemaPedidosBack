import { api } from './api.js';
import { themeManager } from './theme.js';
import { Router } from './router.js';
import { escapeHtml } from './helpers.js';

import { renderDashboard } from './modules/dashboard.module.js';
import { 
  renderPedidosTable, 
  populateClientSelect, 
  resetProductRows, 
  addOrderProductRow,
  openFinalizarOrdenModal,
  toggleFinalizarFields,
  saveFinalizarOrden,
  openRegistrarEnvioModal,
  saveRegistrarEnvio,
  viewOrderDetail,
  setupDefaultDateFilters
} from './modules/pedidos.module.js';
import { 
  renderNuevoPedidoPage, 
  hasUnsavedChanges, 
  clearSelectedClient, 
  openAdelantoModal, 
  addAdelantoFromModal, 
  removeAdelanto, 
  onCondicionPagoChange, 
  submitNuevoPedido, 
  updateItemQty, 
  removeOrderItem,
  handleFilesAttached,
  removeAttachedFile
} from './modules/nuevo-pedido.module.js';
import { 
  renderEnviosTable, 
  openPDF as openGuiaPDF, 
  printPDF as printGuiaPDF,
  viewGuiaDetail,
  anularGuia,
  openAnularModal,
  confirmAnularGuia
} from './modules/envios.module.js';
import { 
  initNuevaGuiaView, 
  submitNuevaGuia, 
  onLocalChanged, 
  updateItemQty as updateGuiaItemQty, 
  removeItemRow as removeGuiaItemRow 
} from './modules/nueva-guia.module.js';
import { 
  renderClientesTable, 
  filterClientes, 
  openNewClientModal, 
  openEditClientModal,
  deleteClient,
  onTipoDocChange,
  onNroDocInput, 
  consultarSunatManual, 
  saveClientFromModal 
} from './modules/clientes.module.js';
import { 
  renderProductosTable, 
  filterProductos, 
  openNewProductModal, 
  openEditProductModal, 
  saveProductFromModal, 
  deleteProduct 
} from './modules/productos.module.js';
import { renderConfigView, saveStorageConfig } from './modules/config.module.js';
import { renderProduccionTable, openProductDetailModal, onSearchInput as onProduccionSearch } from './modules/produccion.module.js';
import * as letrasModule from './modules/letras.module.js';
import * as authModule from './modules/auth.module.js';

// Early stub for instant global accessibility
window.app = {
  navigateTo: (route) => { window.location.hash = '#' + route; },
  toggleTheme: () => { themeManager.toggleTheme(); },
  updateUserUI: () => {}
};

window.authModule = authModule;
window.letrasModule = letrasModule;


// Attach modules globally for inline HTML event handlers
window.nuevoPedidoModule = {
  clearSelectedClient,
  openAdelantoModal,
  addAdelantoFromModal,
  removeAdelanto,
  onCondicionPagoChange,
  submitNuevoPedido,
  updateItemQty,
  removeOrderItem,
  handleFilesAttached,
  removeAttachedFile
};

window.nuevaGuiaModule = {
  initNuevaGuiaView,
  submitNuevaGuia,
  onLocalChanged,
  updateItemQty: updateGuiaItemQty,
  removeItemRow: removeGuiaItemRow
};

window.enviosModule = {
  renderEnviosTable,
  openPDF: openGuiaPDF,
  printPDF: printGuiaPDF,
  viewGuiaDetail,
  anularGuia,
  openAnularModal,
  confirmAnularGuia
};

window.clientesModule = {
  filterClientes,
  openNewClientModal,
  openEditClientModal,
  deleteClient,
  onTipoDocChange,
  onNroDocInput,
  consultarSunatManual,
  saveClientFromModal
};

window.pedidosModule = {
  openFinalizarOrdenModal,
  toggleFinalizarFields,
  saveFinalizarOrden,
  openRegistrarEnvioModal,
  saveRegistrarEnvio,
  viewOrderDetail
};

window.productosModule = {
  filterProductos,
  openNewProductModal,
  openEditProductModal,
  saveProductFromModal,
  deleteProduct
};

window.produccionModule = {
  openProductDetailModal,
  onSearchInput: onProduccionSearch
};

window.configModule = {
  saveStorageConfig
};

window.viewOrderDetail = viewOrderDetail;
window.openProductDetailModal = openProductDetailModal;

class ModularSpaApp {
  constructor() {
    window.app = this;
    this.searchQuery = '';

    // Pre-populate initial datasets
    this.clients = api.getLocalClientes() || [];
    this.products = api.getLocalProductos() || [];
    this.orders = api.getLocalPedidos() || [];
    this.shipments = api.getLocalGuias() || [];
    this.statusData = { connected: false };

    this.orderModal = null;
    this.shipmentModal = null;
    this.clientModal = null;
    this.productModal = null;
    this.orderDetailModal = null;

    this.editingOrderId = null;
    this.editingShipmentId = null;

    this.router = new Router((route) => {
      this.bindViewSpecificEvents();
      this.renderCurrentView();
    });

    this.init();
  }

  async init() {
    try {
      this.initBootstrapModals();
    } catch (e) {
      console.warn("Bootstrap modal init error:", e);
    }

    try {
      this.bindGlobalEvents();
    } catch (e) {
      console.warn("Global events bind error:", e);
    }

    try {
      themeManager.initTheme();
    } catch (e) {}

    // Auto-clean old sample/dummy data from localStorage if present
    try {
      const rawP = localStorage.getItem('inplabel_pedidos');
      if (rawP) {
        const arr = JSON.parse(rawP);
        if (Array.isArray(arr)) {
          const filtered = arr.filter(p => p.nro_orden !== 'OC-2026-089' && p.nro_orden !== 'OC-2026-104');
          localStorage.setItem('inplabel_pedidos', JSON.stringify(filtered));
        }
      }
      const rawL = localStorage.getItem('inplabel_letras');
      if (rawL) {
        const arr = JSON.parse(rawL);
        if (Array.isArray(arr)) {
          const filtered = arr.filter(l => l.nro_letra !== '261-2025' && l.nro_letra !== '262-2025');
          localStorage.setItem('inplabel_letras', JSON.stringify(filtered));
        }
      }
      const rawC = localStorage.getItem('inplabel_clientes');
      if (rawC) {
        const arr = JSON.parse(rawC);
        if (Array.isArray(arr)) {
          const filtered = arr.filter(c => c.nombre_cliente !== 'INVERSIONES PLASTICAS S.A.C.' && c.nro_documento !== '20601234567' && c.nro_documento !== '20509876543' && c.nro_documento !== '10458796321');
          localStorage.setItem('inplabel_clientes', JSON.stringify(filtered));
        }
      }
    } catch (e) {}

    this.updateUserUI();
    authModule.initInactivityTracker();

    if (!authModule.isAuthenticated() || authModule.checkSessionTimeout()) {
      await this.router.navigateTo('login');
      return;
    }

    const hash = window.location.hash.replace('#', '');
    const validRoutes = ['dashboard', 'pedidos', 'nuevo-pedido', 'envios', 'nueva-guia', 'letras', 'clientes', 'productos', 'produccion', 'config', 'bd'];
    const initialRoute = (hash && validRoutes.includes(hash)) ? hash : 'dashboard';

    // Navigate to initial route immediately
    await this.router.navigateTo(initialRoute);
    this.renderCurrentView();

    // Refresh backend data asynchronously in background
    this.refreshData();
  }

  updateUserUI() {
    const user = authModule.getCurrentUser();
    const nameEl = document.getElementById('userNameDisplay');
    const roleEl = document.getElementById('userRoleDisplay');
    const avatarEl = document.getElementById('userAvatar');

    if (user) {
      if (nameEl) nameEl.textContent = user.nombreCompleto || user.username;
      if (roleEl) {
        roleEl.textContent = user.rol || 'OPERACIONES';
        roleEl.className = user.rol === 'ADMIN' ? 'badge bg-primary fs-9 py-0 px-1' : 'badge bg-warning text-dark fs-9 py-0 px-1';
      }
      if (avatarEl) {
        const initials = user.rol === 'ADMIN' ? 'AD' : 'OP';
        avatarEl.textContent = initials;
        avatarEl.style.background = user.rol === 'ADMIN' ? '#0d6efd' : '#f59e0b';
      }
    }
  }

  get currentRoute() {
    return this.router?.currentRoute || 'dashboard';
  }

  navigateTo(route) {
    if (this.currentRoute === 'nuevo-pedido' && route !== 'nuevo-pedido' && typeof hasUnsavedChanges === 'function' && hasUnsavedChanges()) {
      if (!confirm('¿Está seguro de salir? Hay datos no guardados en el formulario de pedido.')) {
        return;
      }
    }
    if (this.router) {
      this.router.navigateTo(route);
    } else {
      window.location.hash = '#' + route;
    }
  }

  openNewOrderModal() {
    this.navigateTo('nuevo-pedido');
  }

  confirmLeaveNuevoPedido() {
    if (this.currentRoute === 'nuevo-pedido' && typeof hasUnsavedChanges === 'function' && hasUnsavedChanges()) {
      if (!confirm('¿Está seguro de salir? Hay datos no guardados en el formulario de pedido.')) {
        return;
      }
    }
    this.navigateTo('pedidos');
  }

  triggerPedidosSearch() {
    this.renderCurrentView();
  }

  triggerGuiasSearch() {
    this.renderCurrentView();
  }

  setTheme(theme) {
    themeManager.setTheme(theme);
  }

  toggleTheme() {
    themeManager.toggleTheme();
  }

  initBootstrapModals() {
    document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('overflow');
    document.body.style.removeProperty('pointer-events');

    document.querySelectorAll('.modal').forEach(m => {
      m.classList.remove('show');
      m.style.display = 'none';
      m.style.removeProperty('pointer-events');
    });

    if (typeof bootstrap === 'undefined' || !bootstrap.Modal) return;
    const oElem = document.getElementById('orderModal');
    const sElem = document.getElementById('shipmentModal');
    const cElem = document.getElementById('clientModal');
    const pElem = document.getElementById('productModal');
    const dElem = document.getElementById('orderDetailModal');

    if (oElem) this.orderModal = new bootstrap.Modal(oElem);
    if (sElem) this.shipmentModal = new bootstrap.Modal(sElem);
    if (cElem) this.clientModal = new bootstrap.Modal(cElem);
    if (pElem) this.productModal = new bootstrap.Modal(pElem);
    if (dElem) this.orderDetailModal = new bootstrap.Modal(dElem);
  }

  bindGlobalEvents() {
    // Theme toggle
    const themeBtn = document.getElementById('themeToggleBtn');
    if (themeBtn) {
      themeBtn.addEventListener('click', () => this.toggleTheme());
    }

    // Clean any orphaned modal backdrops automatically
    document.addEventListener('click', (e) => {
      if (e.target && e.target.classList && e.target.classList.contains('modal-backdrop')) {
        document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('overflow');
        document.body.style.removeProperty('padding-right');
        document.body.style.removeProperty('pointer-events');
      }
    });

    // Global double-click handler for date inputs to open native calendar picker
    document.addEventListener('dblclick', (e) => {
      if (e.target && e.target.tagName === 'INPUT' && e.target.type === 'date') {
        if (typeof e.target.showPicker === 'function') {
          try {
            e.target.showPicker();
          } catch (err) {}
        }
      }
    });

    // Sidebar toggle
    const toggleBtn = document.getElementById('btnToggleSidebar') || document.getElementById('toggleSidebarBtn');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        const sidebar = document.querySelector('.sidebar') || document.getElementById('sidebar');
        if (!sidebar) return;
        sidebar.classList.toggle('collapsed');
        const icon = toggleBtn.querySelector('i');
        if (icon) {
          if (sidebar.classList.contains('collapsed')) {
            icon.className = 'bi bi-chevron-right';
          } else {
            icon.className = 'bi bi-chevron-left';
          }
        }
      });
    }

    // Navigation routes
    document.querySelectorAll('.nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const route = btn.getAttribute('data-route');
        if (route) this.navigateTo(route);
      });
    });

    // Hash change event listener
    window.addEventListener('hashchange', () => {
      const route = window.location.hash.replace('#', '');
      if (route && route !== this.currentRoute) {
        this.navigateTo(route);
      }
    });

    // Global search input
    const searchInput = document.getElementById('globalSearchInput');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.searchQuery = e.target.value;
        this.renderCurrentView();
      });
    }
  }

  bindViewSpecificEvents() {
    setupDefaultDateFilters();

    ['filterOrderStatus', 'filterEstablishment', 'filterDateFrom', 'filterDateTo'].forEach(id => {
      const elem = document.getElementById(id);
      if (elem && !elem.dataset.boundChange) {
        elem.dataset.boundChange = 'true';
        elem.addEventListener('change', () => this.renderCurrentView());
      }
    });

    const searchClientName = document.getElementById('searchClientNameInput');
    if (searchClientName && !searchClientName.dataset.boundEnter) {
      searchClientName.dataset.boundEnter = 'true';
      searchClientName.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.renderCurrentView();
        }
      });
    }
  }

  async refreshData() {
    let statusData = { connected: false };
    let clients = [];
    let products = [];
    let orders = [];
    let shipments = [];

    try {
      const results = await Promise.allSettled([
        api.getStatus(),
        api.getClientes(),
        api.getProductos(),
        api.getPedidos(),
        api.getGuias()
      ]);

      if (results[0].status === 'fulfilled' && results[0].value) statusData = results[0].value;
      if (results[1].status === 'fulfilled' && results[1].value) clients = results[1].value;
      if (results[2].status === 'fulfilled' && results[2].value) products = results[2].value;
      if (results[3].status === 'fulfilled' && results[3].value) orders = results[3].value;
      if (results[4].status === 'fulfilled' && results[4].value) shipments = results[4].value;
    } catch (err) {
      console.warn("Backend Spring Boot offline or unreachable:", err);
    }

    this.statusData = statusData;
    if (clients && clients.length > 0) this.clients = clients;
    if (products && products.length > 0) this.products = products;
    if (orders && orders.length > 0) this.orders = orders;
    if (shipments && shipments.length > 0) this.shipments = shipments;

    const indicator = document.getElementById('backendStatusIndicator');
    if (indicator) {
      indicator.removeAttribute('style');
      if (statusData && statusData.connected) {
        indicator.className = 'status-badge ACTIVA py-2 px-3 fs-7';
        indicator.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i> Spring Boot: Conectado a MySQL';
      } else {
        indicator.className = 'status-badge CANCELADO py-2 px-3 fs-7';
        indicator.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i> Spring Boot: Desconectado';
      }
    }

    this.updateBadges();
    this.renderCurrentView();
  }

  updateBadges() {
    const pendingOrders = this.orders.filter(o => o.estado === 'PENDIENTE');
    const activeShipments = this.shipments.filter(s => s.estado === 'EN_TRANSITO' || s.estado === 'ACTIVA');

    const badgeP = document.getElementById('badgePedidos');
    const badgeE = document.getElementById('badgeEnvios');
    if (badgeP) {
      badgeP.textContent = pendingOrders.length;
      badgeP.classList.toggle('d-none', pendingOrders.length === 0);
    }
    if (badgeE) {
      badgeE.textContent = activeShipments.length;
      badgeE.classList.toggle('d-none', activeShipments.length === 0);
    }
  }

  renderCurrentView() {
    this.updateBadges();
    const route = this.currentRoute;

    try {
      if (route === 'dashboard') {
        renderDashboard(this.orders, this.shipments, this.clients, this.products, this.statusData);
      } else if (route === 'pedidos') {
        renderPedidosTable(this.orders, this.searchQuery);
      } else if (route === 'nuevo-pedido') {
        renderNuevoPedidoPage(this.clients, this.products);
      } else if (route === 'envios') {
        renderEnviosTable(this.shipments, this.searchQuery);
      } else if (route === 'letras') {
        letrasModule.initLetrasView();
      } else if (route === 'nueva-guia') {
        initNuevaGuiaView();
      } else if (route === 'clientes') {
        renderClientesTable(this.clients, this.searchQuery);
      } else if (route === 'productos') {
        renderProductosTable(this.products, this.searchQuery);
      } else if (route === 'produccion') {
        renderProduccionTable(this.orders, this.products, this.searchQuery);
      } else if (route === 'config' || route === 'bd') {
        renderConfigView(this.clients.length, this.products.length);
      }
    } catch (err) {
      console.error("Error rendering view:", route, err);
    }
  }

  viewOrderDetail(idPedido) {
    viewOrderDetail(idPedido);
  }
}

export const app = new ModularSpaApp();
window.app = app;
