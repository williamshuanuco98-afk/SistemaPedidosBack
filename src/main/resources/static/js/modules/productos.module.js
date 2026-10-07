import { escapeHtml, filterAndRankItems, showConfirmModal, paginateItems, renderPaginationUI } from '../helpers.js';
import { api } from '../api.js';
import { canDelete } from './auth.module.js';

let currentProducts = [];
let currentPage = 1;
const pageSize = 20;

export function changePage(delta) {
  currentPage += delta;
  filterProductos(document.getElementById('searchProductosInput')?.value || '');
}

export function resetPagination() {
  currentPage = 1;
}

export function renderProductosTable(products = [], searchQuery = '') {
  currentProducts = products || [];
  const searchInput = document.getElementById('searchProductosInput');
  if (searchInput && searchQuery) searchInput.value = searchQuery;
  filterProductos(searchQuery || (searchInput ? searchInput.value : ''));
}

export function filterProductos(queryStr = '') {
  const tbody = document.getElementById('productosTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  const filtered = filterAndRankItems(
    currentProducts, 
    queryStr, 
    p => `#${p.id_producto || ''} ${p.id_producto || ''} ${p.nombre_producto || ''} ${p.tipo_producto || p.categoria || ''} ${p.unidad_medida || ''}`
  );

  const p = paginateItems(filtered, currentPage, pageSize);
  currentPage = p.currentPage;

  renderPaginationUI({
    containerId: 'productosPaginationContainer',
    currentPage: p.currentPage,
    totalPages: p.totalPages,
    totalItems: p.totalItems,
    startIndex: p.startIndex,
    endIndex: p.endIndex,
    onPageChangeName: 'productosModule.changePage'
  });

  if (p.items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-4">No se encontraron productos coincidentes.</td></tr>`;
    return;
  }

  const allowDelete = canDelete();

  p.items.forEach(p => {
    const tipo = p.tipo_producto || p.categoria || 'General';
    const um = p.unidad_medida || 'UNID';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-bold text-muted">#${p.id_producto}</td>
      <td class="fw-semibold">${escapeHtml(p.nombre_producto)}</td>
      <td><span class="badge bg-primary text-white px-2.5 py-1 fs-8 fw-bold">${escapeHtml(tipo)}</span></td>
      <td class="text-center"><span class="badge bg-secondary-subtle text-secondary-emphasis border fw-bold px-2 py-1">${escapeHtml(um)}</span></td>
      <td><span class="status-badge COMPLETADO">ACTIVO</span></td>
      <!-- 1. Columna EDITAR -->
      <td class="text-center">
        <button type="button" class="btn-action-solid btn-edit" title="Editar producto" onclick="productosModule.openEditProductModal('${p.id_producto || p.id}')">
          <i class="bi bi-pencil-fill text-dark"></i>
        </button>
      </td>
      <!-- 2. Columna ELIMINAR -->
      <td class="text-center">
        ${allowDelete ? `
          <button type="button" class="btn-action-solid btn-delete" title="Eliminar producto" onclick="productosModule.deleteProduct('${p.id_producto || p.id}')">
            <i class="bi bi-trash-fill"></i>
          </button>
        ` : `
          <button type="button" class="btn-action-solid btn-delete" style="opacity: 0.25; cursor: not-allowed;" title="Permiso restringido (Solo Administrador)" disabled>
            <i class="bi bi-lock-fill"></i>
          </button>
        `}
      </td>
    `;
    tbody.appendChild(tr);
  });
}

export function openNewProductModal() {
  const modalElem = document.getElementById('modalProducto');
  if (!modalElem) return;

  document.getElementById('modalProductoTitle').innerHTML = '<i class="bi bi-box-seam me-2"></i>Registrar Producto';
  document.getElementById('modalProductoId').value = '';
  document.getElementById('modalProductoNombre').value = '';
  document.getElementById('modalProductoTipo').value = '';
  const umSelect = document.getElementById('modalProductoUnidadMedida');
  if (umSelect) umSelect.value = 'UNID';

  const modal = new bootstrap.Modal(modalElem);
  modal.show();
}

export function openEditProductModal(id) {
  const p = currentProducts.find(item => String(item.id_producto || item.id) === String(id));
  if (!p) return;

  const modalElem = document.getElementById('modalProducto');
  if (!modalElem) return;

  document.getElementById('modalProductoTitle').innerHTML = '<i class="bi bi-pencil-square me-2"></i>Editar Producto';
  document.getElementById('modalProductoId').value = p.id_producto;
  document.getElementById('modalProductoNombre').value = p.nombre_producto;
  
  const currentTipo = (p.tipo_producto || p.categoria || 'FRASCOS').toUpperCase();
  const selectElem = document.getElementById('modalProductoTipo');
  if (selectElem) {
    selectElem.value = currentTipo;
    // If not matched, fallback to FRASCOS
    if (!selectElem.value) {
      selectElem.value = 'FRASCOS';
    }
  }

  const umSelect = document.getElementById('modalProductoUnidadMedida');
  if (umSelect) {
    umSelect.value = (p.unidad_medida || 'UNID').toUpperCase();
  }

  const modal = new bootstrap.Modal(modalElem);
  modal.show();
}

let isSubmittingProducto = false;

export async function saveProductFromModal() {
  if (isSubmittingProducto) return;

  const idStr = document.getElementById('modalProductoId')?.value;
  const nombre = document.getElementById('modalProductoNombre')?.value.trim();
  const tipo = document.getElementById('modalProductoTipo')?.value.trim() || 'General';
  const um = document.getElementById('modalProductoUnidadMedida')?.value.trim() || 'UNID';

  if (!nombre) {
    alert('Por favor ingrese el nombre del producto.');
    return;
  }

  const btnSave = document.querySelector('#formProducto button[type="submit"]');
  const originalBtnHtml = btnSave ? btnSave.innerHTML : '<i class="bi bi-save me-1"></i> Guardar Producto';

  isSubmittingProducto = true;
  if (btnSave) {
    btnSave.disabled = true;
    btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Guardando...';
  }

  const payload = {
    nombre_producto: nombre,
    tipo_producto: tipo,
    categoria: tipo,
    unidad_medida: um
  };

  const modalElem = document.getElementById('modalProducto');
  const modal = modalElem ? bootstrap.Modal.getInstance(modalElem) : null;

  try {
    let res;
    if (idStr) {
      // Edit existing product
      const id = parseInt(idStr, 10);
      res = await api.updateProducto(id, payload);
    } else {
      // Register new product
      res = await api.addProducto(payload);
    }

    if (res && (res.success === false || res.error)) {
      alert(res.error || res.message || 'No se pudo guardar el producto.');
      return;
    }

    // Re-fetch fresh products directly from MySQL database
    currentProducts = await api.getProductos();
    if (window.app) window.app.products = [...currentProducts];

    if (modal) modal.hide();

    // Reset modal fields for next product registration
    const idElem = document.getElementById('modalProductoId');
    const nombreElem = document.getElementById('modalProductoNombre');
    if (idElem) idElem.value = '';
    if (nombreElem) nombreElem.value = '';

    filterProductos();
  } catch (err) {
    console.error('Error al guardar producto:', err);
    alert('Ocurrió un error inesperado al procesar el producto.');
  } finally {
    isSubmittingProducto = false;
    if (btnSave) {
      btnSave.disabled = false;
      btnSave.innerHTML = originalBtnHtml;
    }
  }
}

export async function deleteProduct(id) {
  if (!canDelete()) {
    await showConfirmModal({
      title: 'Acceso Restringido',
      message: 'El perfil de Operaciones no tiene permisos para eliminar productos.',
      icon: 'bi-shield-lock-fill',
      iconBg: 'rgba(234, 179, 8, 0.15)',
      iconColor: '#eab308',
      confirmText: 'Entendido',
      confirmBtnClass: 'btn-warning text-dark',
      cancelText: 'Cerrar'
    });
    return;
  }
  const p = currentProducts.find(item => String(item.id_producto || item.id) === String(id));
  if (!p) return;

  const confirmed = await showConfirmModal({
    title: '¿Eliminar Producto?',
    message: `¿Está seguro de eliminar el producto #${id} "${p.nombre_producto}"?`,
    icon: 'bi-trash3-fill',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
    confirmText: 'Eliminar Producto',
    confirmBtnClass: 'btn-danger',
    cancelText: 'Cancelar'
  });

  if (!confirmed) return;

  await api.deleteProducto(id);
  currentProducts = currentProducts.filter(item => String(item.id_producto || item.id) !== String(id));
  if (window.app) window.app.products = [...currentProducts];
  filterProductos();
}
