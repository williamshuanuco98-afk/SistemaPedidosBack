export function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Remove accents/diacritics and convert to lowercase for fuzzy matching
export function normalizeText(str) {
  if (!str) return '';
  return String(str)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

// Advanced multi-token search with high-precision relevance ranking (order independent & word-prefix priority)
export function filterAndRankItems(items, queryStr, getSearchableText) {
  if (!items || !Array.isArray(items)) return [];
  const rawQuery = (queryStr || '').trim();
  if (!rawQuery) return items;

  const normalizedQuery = normalizeText(rawQuery);
  const tokens = normalizedQuery.split(/\s+/).filter(t => t.length > 0);
  if (tokens.length === 0) return items;

  const matched = [];

  for (const item of items) {
    const text = normalizeText(getSearchableText(item));
    
    // Every single token typed by user must exist somewhere in the normalized item text
    const isMatch = tokens.every(token => text.includes(token));

    if (isMatch) {
      let score = 0;

      // 1. Exact full query match
      if (text === normalizedQuery) score += 10000;
      if (text.startsWith(normalizedQuery)) score += 5000;

      // 2. Main field prefix bonus
      const mainName = normalizeText(item.nombre_cliente || item.razon_social || item.nombre_producto || '');
      if (mainName && mainName.startsWith(tokens[0])) {
        score += 2500;
      }

      // 3. Document / Code prefix bonus
      const docCode = normalizeText(item.nro_documento || item.codigo_producto || '');
      if (docCode && docCode.startsWith(rawQuery)) {
        score += 3000;
      }

      // 4. Word boundary prefix bonus (e.g. typing "che" matches words starting with "che" like "CHEMIFABRIK")
      const words = text.split(/[\s\-\,\.\/]+/);
      for (const token of tokens) {
        for (const word of words) {
          if (word === token) {
            score += 1500;
          } else if (word.startsWith(token)) {
            score += 1000; // High score for word prefix match
          } else if (word.includes(token)) {
            score += 50;   // Low score for infix match (e.g. "sanchez")
          }
        }
      }

      // 5. Short string length bonus (prefer concise matches over long paragraphs)
      if (text.length > 0) {
        score += Math.max(0, 100 - text.length);
      }

      matched.push({ item, score });
    }
  }

  // Sort descending by highest relevance score
  matched.sort((a, b) => b.score - a.score);
  return matched.map(m => m.item);
}

// Format date strictly to DD/MM/AAAA format
export function formatDate(dateStr) {
  if (!dateStr || dateStr === '-' || dateStr === 'null' || dateStr === 'undefined') return '-';
  const str = String(dateStr).trim();
  if (!str) return '-';
  
  if (str.includes('T')) {
    const cleanStr = str.split('T')[0];
    const parts = cleanStr.split('-');
    if (parts.length === 3 && parts[0].length === 4) {
      return `${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}`;
    }
  }
  if (str.includes('-')) {
    const parts = str.split('-');
    if (parts.length === 3) {
      if (parts[0].length === 4) { // YYYY-MM-DD
        return `${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}`;
      } else if (parts[2].length === 4) { // DD-MM-YYYY
        return `${parts[0].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[2]}`;
      }
    }
  }
  if (str.includes('/')) {
    const parts = str.split('/');
    if (parts.length === 3) {
      if (parts[0].length === 4) { // YYYY/MM/DD
        return `${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}`;
      } else if (parts[2].length === 4) { // DD/MM/YYYY
        return `${parts[0].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[2]}`;
      }
    }
  }
  return str;
}

// Universal Clean Bootstrap Modal Opener & Controller
export function showBootstrapModal(modalElemOrId) {
  const elem = typeof modalElemOrId === 'string' ? document.getElementById(modalElemOrId) : modalElemOrId;
  if (!elem) {
    console.error("showBootstrapModal: Element not found:", modalElemOrId);
    alert("No se encontró la ventana emergente.");
    return;
  }
  if (!elem) return;

  elem.style.removeProperty('pointer-events');
  elem.style.pointerEvents = 'auto';

  try {
    const bsModal = window.bootstrap && window.bootstrap.Modal;
    if (bsModal) {
      let modalInstance = bsModal.getInstance(elem);
      if (modalInstance) {
        modalInstance.dispose();
      }
      modalInstance = new bsModal(elem, { backdrop: true, keyboard: true });
      modalInstance.show();

      setTimeout(() => {
        const backdrop = document.querySelector('.modal-backdrop');
        if (backdrop) {
          backdrop.onclick = () => hideBootstrapModal(elem);
        }
      }, 100);
      return;
    }
  } catch (err) {
    console.warn("Bootstrap JS modal show error:", err);
  }

  // UI Fallback if Bootstrap JS is absent
  document.body.classList.add('modal-open');
  elem.classList.add('show');
  elem.style.display = 'block';
  elem.style.pointerEvents = 'auto';
  elem.removeAttribute('aria-hidden');
  elem.setAttribute('aria-modal', 'true');
}

export function hideBootstrapModal(modalElemOrId) {
  const elem = typeof modalElemOrId === 'string' ? document.getElementById(modalElemOrId) : modalElemOrId;
  if (elem) {
    try {
      const bsModal = window.bootstrap && window.bootstrap.Modal;
      if (bsModal) {
        const modalInstance = bsModal.getInstance(elem);
        if (modalInstance) {
          modalInstance.hide();
        }
      }
    } catch (e) {}

    elem.classList.remove('show');
    elem.style.display = 'none';
    elem.style.removeProperty('pointer-events');
    elem.setAttribute('aria-hidden', 'true');
    elem.removeAttribute('aria-modal');
  }

  document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
  document.body.classList.remove('modal-open');
  document.body.style.removeProperty('overflow');
  document.body.style.removeProperty('pointer-events');
}
window.hideBootstrapModal = hideBootstrapModal;
window.showBootstrapModal = showBootstrapModal;

export function showConfirmModal({
  title = '¿Confirmar Acción?',
  message = '¿Está seguro de continuar?',
  icon = 'bi-exclamation-triangle-fill',
  iconBg = 'rgba(239, 68, 68, 0.12)',
  iconColor = '#ef4444',
  confirmText = 'Confirmar',
  confirmBtnClass = 'btn-danger',
  cancelText = 'Cancelar'
} = {}) {
  return new Promise((resolve) => {
    const modalElem = document.getElementById('globalConfirmModal');
    if (!modalElem || typeof bootstrap === 'undefined' || !bootstrap.Modal) {
      resolve(true);
      return;
    }

    const titleEl = document.getElementById('globalConfirmTitle');
    const msgEl = document.getElementById('globalConfirmMessage');
    const iconEl = document.getElementById('globalConfirmIcon');
    const iconBox = document.getElementById('globalConfirmIconBox');
    const actionBtn = document.getElementById('globalConfirmActionBtn');
    const cancelBtn = document.getElementById('globalConfirmCancelBtn');

    if (titleEl) titleEl.textContent = title;
    if (msgEl) msgEl.textContent = message;
    if (iconEl) iconEl.className = `bi ${icon} fs-2`;
    if (iconBox) {
      iconBox.style.backgroundColor = iconBg;
      iconBox.style.color = iconColor;
    }
    if (actionBtn) {
      actionBtn.textContent = confirmText;
      actionBtn.className = `btn ${confirmBtnClass} flex-fill py-2 rounded-3 fw-bold shadow-sm`;
    }
    if (cancelBtn) cancelBtn.textContent = cancelText;

    const bsModal = (typeof bootstrap !== 'undefined' && bootstrap.Modal) 
      ? (bootstrap.Modal.getOrCreateInstance ? bootstrap.Modal.getOrCreateInstance(modalElem) : new bootstrap.Modal(modalElem))
      : null;
    
    if (!bsModal) {
      resolve(true);
      return;
    }

    let isResolved = false;

    const cleanup = () => {
      document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
      document.body.classList.remove('modal-open');
    };

    const onConfirm = () => {
      if (isResolved) return;
      isResolved = true;
      bsModal.hide();
      setTimeout(cleanup, 250);
      resolve(true);
    };

    const onCancel = () => {
      if (isResolved) return;
      isResolved = true;
      bsModal.hide();
      setTimeout(cleanup, 250);
      resolve(false);
    };

    actionBtn.onclick = onConfirm;
    cancelBtn.onclick = onCancel;

    modalElem.addEventListener('hidden.bs.modal', () => {
      if (!isResolved) {
        isResolved = true;
        resolve(false);
      }
      setTimeout(cleanup, 100);
    }, { once: true });

    bsModal.show();
  });
}
window.showConfirmDialog = showConfirmModal;

export function initFlatpickrOnAllInputs() {
  if (typeof window.flatpickr === 'undefined') return;

  const inputs = document.querySelectorAll('input[type="date"], input.flatpickr-input');
  inputs.forEach(input => {
    if (input.dataset.fpBound === 'true') return;
    input.dataset.fpBound = 'true';

    try {
      const fpConfig = {
        dateFormat: 'Y-m-d',
        altInput: true,
        altFormat: 'd/m/Y',
        allowInput: true,
        clickOpens: true,
        locale: (window.flatpickr.l10ns && window.flatpickr.l10ns.es) ? window.flatpickr.l10ns.es : 'es'
      };

      if (input.max) {
        fpConfig.maxDate = input.max;
      }

      const fp = window.flatpickr(input, fpConfig);

      input.addEventListener('dblclick', () => {
        if (fp) fp.open();
      });
      input.addEventListener('click', () => {
        if (fp) fp.open();
      });
    } catch (e) {
      console.warn("Flatpickr init error on input:", input, e);
    }
  });
}
window.initFlatpickrOnAllInputs = initFlatpickrOnAllInputs;

export function setAppZoom() {
  try {
    localStorage.removeItem('inplabel_user_zoom');
    if (typeof document !== 'undefined' && document.body) {
      document.body.style.removeProperty('zoom');
    }
  } catch (e) {}
}

export function initGlobalZoomHandlers() {
  try {
    localStorage.removeItem('inplabel_user_zoom');
    if (typeof document !== 'undefined' && document.body) {
      document.body.style.removeProperty('zoom');
    }
  } catch (e) {}
}
window.setAppZoom = setAppZoom;
window.initGlobalZoomHandlers = initGlobalZoomHandlers;

// Generic Pagination Helper
export function paginateItems(items, currentPage = 1, pageSize = 20) {
  const totalItems = items ? items.length : 0;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const page = Math.max(1, Math.min(currentPage, totalPages));
  const startIndex = totalItems === 0 ? 0 : (page - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, totalItems);

  const pageItems = items ? items.slice(startIndex, endIndex) : [];

  return {
    items: pageItems,
    currentPage: page,
    pageSize,
    totalPages,
    totalItems,
    startIndex: totalItems === 0 ? 0 : startIndex + 1,
    endIndex
  };
}

export function renderPaginationUI({ containerId, currentPage, totalPages, totalItems, startIndex, endIndex, onPageChangeName }) {
  const container = document.getElementById(containerId);
  if (!container) return;

  if (totalItems === 0) {
    container.innerHTML = '';
    container.classList.add('d-none');
    return;
  }

  container.classList.remove('d-none');
  const isFirst = currentPage <= 1;
  const isLast = currentPage >= totalPages;

  container.innerHTML = `
    <div class="d-flex flex-wrap align-items-center justify-content-between p-3 bg-body-tertiary border-top gap-2">
      <div class="text-muted small">
        Mostrando <span class="fw-bold text-body">${startIndex}-${endIndex}</span> de <span class="fw-bold text-body">${totalItems}</span> registros
      </div>
      <div class="d-flex align-items-center gap-2">
        <button type="button" class="btn btn-sm btn-outline-secondary" ${isFirst ? 'disabled' : ''} onclick="${onPageChangeName}(-1)">
          <i class="bi bi-chevron-left me-1"></i> Anterior
        </button>
        <span class="small fw-semibold px-2 text-secondary">Página ${currentPage} de ${totalPages}</span>
        <button type="button" class="btn btn-sm btn-outline-secondary" ${isLast ? 'disabled' : ''} onclick="${onPageChangeName}(1)">
          Siguiente <i class="bi bi-chevron-right ms-1"></i>
        </button>
      </div>
    </div>
  `;
}
window.paginateItems = paginateItems;
window.renderPaginationUI = renderPaginationUI;


