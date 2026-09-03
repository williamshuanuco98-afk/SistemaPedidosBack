export function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Clean text for search matching: remove accents, normalize punctuation and common abbreviations
export function normalizeText(str) {
  if (!str) return '';
  return String(str)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/c\//g, 'con ')
    .replace(/[\.\,\-\_\/\(\)\[\]]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

// Advanced multi-token search with high-precision relevance ranking (order independent & word-prefix priority)
export function filterAndRankItems(items, queryStr, getSearchableText) {
  if (!items || !Array.isArray(items)) return [];
  const rawQuery = (queryStr || '').trim();
  if (!rawQuery) return items;

  const normalizedQuery = normalizeText(rawQuery);
  const rawTokens = rawQuery.toLowerCase().split(/\s+/).filter(t => t.length > 0);
  const normTokens = normalizedQuery.split(/\s+/).filter(t => t.length > 0);

  // Combine raw tokens and normalized tokens (deduplicated)
  const tokens = Array.from(new Set([...rawTokens, ...normTokens]));
  if (tokens.length === 0) return items;

  const matched = [];

  for (const item of items) {
    const rawText = getSearchableText(item) || '';
    const normText = normalizeText(rawText);
    const words = normText.split(/\s+/).filter(w => w.length > 0);

    let matchedTokenCount = 0;
    let tokenMatchScore = 0;

    // Evaluate token matches
    for (const token of tokens) {
      if (!token) continue;
      let tokenMatched = false;
      let highestTokenScore = 0;

      for (let i = 0; i < words.length; i++) {
        const word = words[i];
        if (word === token) {
          tokenMatched = true;
          highestTokenScore = Math.max(highestTokenScore, 1000 + (token.length * 10)); // Exact word match
        } else if (word.startsWith(token)) {
          tokenMatched = true;
          highestTokenScore = Math.max(highestTokenScore, 500 + (token.length * 5)); // Word prefix match
        } else if (word.includes(token)) {
          tokenMatched = true;
          highestTokenScore = Math.max(highestTokenScore, 100); // Substring match inside word
        }
      }

      // Also check full normText if not matched per word (e.g. compressed number or code)
      if (!tokenMatched && normText.includes(token)) {
        tokenMatched = true;
        highestTokenScore = 80;
      }

      if (tokenMatched) {
        matchedTokenCount++;
        tokenMatchScore += highestTokenScore;
      }
    }

    // Must match at least 1 token if query is 1-2 tokens, or at least 50% of tokens for long queries
    const minTokensRequired = tokens.length <= 2 ? 1 : Math.ceil(tokens.length * 0.5);

    if (matchedTokenCount >= minTokensRequired) {
      let score = 0;

      // Primary Boost: Exponential reward for matching MORE tokens (Rank items with most token matches highest!)
      score += (matchedTokenCount * 50000);

      // Full Match Bonus: If all tokens matched
      if (matchedTokenCount === tokens.length) {
        score += 100000;
      }

      // Token Match Quality Score sum
      score += tokenMatchScore;

      // Exact full string or exact prefix matches
      if (normText === normalizedQuery) score += 80000;
      if (normText.startsWith(normalizedQuery)) score += 40000;

      // Sequential phrase match: If normalizedQuery appears as continuous substring
      if (normText.includes(normalizedQuery)) {
        score += 30000;
      }

      // Main Product / Client Name Prefix Match
      const mainName = normalizeText(item.nombre_producto || item.nombre_cliente || item.razon_social || '');
      if (mainName) {
        if (mainName.startsWith(normalizedQuery)) score += 25000;
        if (mainName.startsWith(tokens[0])) score += 10000;
      }

      // Code / ID exact match (e.g. #140 or PROD-140)
      const docCode = normalizeText(item.codigo_producto || item.id_producto || item.nro_documento || '');
      if (docCode) {
        if (docCode === normalizedQuery || docCode === `#${normalizedQuery}`) score += 90000;
        else if (docCode.startsWith(normalizedQuery)) score += 45000;
      }

      // Length Penalty: Slightly favor concise text when scores are close
      if (normText.length > 0) {
        score += Math.max(0, 50 - Math.floor(normText.length / 5));
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
  if (!elem) return;

  try {
    const bsModal = window.bootstrap && window.bootstrap.Modal;
    if (bsModal) {
      const inst = bsModal.getOrCreateInstance(elem);
      if (inst) {
        inst.show();
        return;
      }
    }
  } catch (err) {
    console.warn("Bootstrap JS modal show error:", err);
  }

  // Fallback if bootstrap JS object is not present
  elem.classList.add('show');
  elem.style.display = 'block';
  elem.style.pointerEvents = 'auto';
  elem.removeAttribute('aria-hidden');
  elem.setAttribute('aria-modal', 'true');
  document.body.classList.add('modal-open');
}

export function hideBootstrapModal(modalElemOrId) {
  const elem = typeof modalElemOrId === 'string' ? document.getElementById(modalElemOrId) : modalElemOrId;
  if (elem) {
    try {
      const bsModal = window.bootstrap && window.bootstrap.Modal;
      if (bsModal) {
        const inst = bsModal.getInstance(elem) || bsModal.getOrCreateInstance(elem);
        if (inst) {
          inst.hide();
        }
      }
    } catch (e) {}

    elem.classList.remove('show');
    elem.style.removeProperty('display');
    elem.style.removeProperty('pointer-events');
    elem.setAttribute('aria-hidden', 'true');
    elem.removeAttribute('aria-modal');
  }

  // Only remove backdrop and body lock if NO open modals remain
  const remainingOpen = document.querySelectorAll('.modal.show');
  if (remainingOpen.length === 0) {
    document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('overflow');
    document.body.style.removeProperty('padding-right');
    document.body.style.removeProperty('pointer-events');
  }
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


