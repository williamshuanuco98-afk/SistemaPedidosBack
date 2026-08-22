export const BASE_URL = (typeof window !== 'undefined' && window.location && window.location.protocol.startsWith('http') && window.location.port === '8080')
  ? '/api'
  : 'http://localhost:8080/api';

async function fetchWithTimeout(resource, options = {}) {
  const { timeout = 3000, headers = {}, ...fetchOptions } = options;
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);

  // Inyectar cabeceras de rol de usuario automáticamente
  let authHeaders = { ...headers };
  try {
    const rawUser = localStorage.getItem('inplabel_user');
    if (rawUser) {
      const u = JSON.parse(rawUser);
      if (u && u.rol) {
        authHeaders['X-User-Role'] = u.rol;
        authHeaders['X-Username'] = u.username;
      }
    }
  } catch (e) {}

  try {
    const response = await fetch(resource, {
      ...fetchOptions,
      headers: authHeaders,
      signal: controller.signal
    });
    clearTimeout(id);
    return response;
  } catch (error) {
    clearTimeout(id);
    throw error;
  }
}

// Empty fallbacks - no dummy data injected
const FALLBACK_CLIENTS = [];
const FALLBACK_PRODUCTS = [];
const FALLBACK_ORDERS = [];
const FALLBACK_SHIPMENTS = [];

function getLocalData(key, fallback = []) {
  try {
    const raw = localStorage.getItem('inplabel_' + key);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        // Clean out old hardcoded sample orders if present
        if (key === 'pedidos') {
          const cleaned = parsed.filter(p => p.nro_orden !== 'OC-2026-089' && p.nro_orden !== 'OC-2026-104');
          return cleaned;
        }
        return parsed;
      }
    }
  } catch (e) {}
  return fallback;
}

function setLocalData(key, data) {
  try {
    localStorage.setItem('inplabel_' + key, JSON.stringify(data));
  } catch (e) {}
}

export const api = {
  getLocalClientes() {
    return getLocalData('clientes', FALLBACK_CLIENTS);
  },
  getLocalProductos() {
    return getLocalData('productos', FALLBACK_PRODUCTS);
  },
  getLocalPedidos() {
    return getLocalData('pedidos', FALLBACK_ORDERS);
  },
  getLocalGuias() {
    return getLocalData('guias', FALLBACK_SHIPMENTS);
  },

  async getStatus() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/status`, { timeout: 1500 });
      if (res && res.ok) return await res.json();
    } catch (e) {}
    return { connected: false, message: 'Spring Boot Backend Desconectado (Modo Local Activo)' };
  },

  async getClientes() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes`, { timeout: 2500 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) {
          setLocalData('clientes', data);
          return data;
        }
      }
    } catch (e) {}
    return getLocalData('clientes', FALLBACK_CLIENTS);
  },

  async addCliente(clienteData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(clienteData),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    const list = getLocalData('clientes', FALLBACK_CLIENTS);
    const newClient = { id: Date.now(), ...clienteData };
    list.unshift(newClient);
    setLocalData('clientes', list);
    return newClient;
  },

  async updateCliente(id, clienteData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(clienteData),
        timeout: 3000
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.error('Error al actualizar cliente en MySQL:', e);
    }
    return { success: false };
  },

  async deleteCliente(id) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes/${id}`, {
        method: 'DELETE',
        timeout: 3000
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.error('Error al eliminar cliente en MySQL:', e);
    }
    return { success: false };
  },

  async consultarSunatRuc(ruc) {
    if (!ruc || (ruc.length !== 11 && ruc.length !== 8)) {
      return { success: false };
    }
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes/sunat/${ruc}`, { timeout: 6000 });
      if (res.ok) {
        const data = await res.json();
        if (data && data.success) return data;
      }
    } catch (e) {
      console.warn("Error consultando backend SUNAT RUC:", e);
    }

    try {
      const res = await fetchWithTimeout(`https://api.apis.net.pe/v1/ruc?numero=${ruc}`, { timeout: 3000 });
      if (res.ok) {
        const data = await res.json();
        if (data && data.nombre) {
          let dir = (data.direccion || '').trim();
          if (!dir) {
            dir = `${data.viaTipo || ''} ${data.viaNombre || ''} ${data.numero ? 'NRO ' + data.numero : ''} ${data.zonaTipo || ''} ${data.zonaCodigo || ''}`.replace(/\s+/g, ' ').trim();
          }
          if (data.distrito && !dir.toUpperCase().includes(data.distrito.toUpperCase())) {
            const loc = [data.departamento, data.provincia, data.distrito].filter(Boolean).join(' - ');
            if (loc) dir = dir ? `${dir} - ${loc}` : loc;
          }

          return {
            success: true,
            nro_documento: ruc,
            nombre_cliente: data.nombre,
            direccion: dir,
            estado: data.estado || 'ACTIVO',
            condicion: data.condicion || 'HABIDO'
          };
        }
      }
    } catch (e) {}

    return { success: false };
  },

  async consultarDni(dni) {
    if (!dni || dni.length !== 8) {
      return { success: false };
    }
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes/dni/${dni}`, { timeout: 6000 });
      if (res.ok) {
        const data = await res.json();
        if (data && data.success) return data;
      }
    } catch (e) {
      console.warn("Error consultando backend DNI:", e);
    }

    try {
      const res = await fetchWithTimeout(`https://api.apis.net.pe/v1/dni?numero=${dni}`, { timeout: 3000 });
      if (res.ok) {
        const data = await res.json();
        if (data && data.nombre) {
          return {
            success: true,
            nro_documento: dni,
            nombre_cliente: data.nombre,
            nombres: data.nombres,
            apellidoPaterno: data.apellidoPaterno,
            apellidoMaterno: data.apellidoMaterno
          };
        }
      }
    } catch (e) {}

    return { success: false };
  },

  async getProductos() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos`, { timeout: 2500 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) {
          setLocalData('productos', data);
          return data;
        }
      }
    } catch (e) {}
    return getLocalData('productos', FALLBACK_PRODUCTS);
  },

  async addProducto(productoData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productoData),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    const list = getLocalData('productos', FALLBACK_PRODUCTS);
    const newProd = { id: Date.now(), codigo_producto: 'PROD-' + Math.floor(100 + Math.random() * 900), ...productoData };
    list.unshift(newProd);
    setLocalData('productos', list);
    return newProd;
  },

  async updateProducto(id, productoData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productoData),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    const list = getLocalData('productos', FALLBACK_PRODUCTS);
    const idx = list.findIndex(p => String(p.id) === String(id));
    if (idx !== -1) {
      list[idx] = { ...list[idx], ...productoData };
      setLocalData('productos', list);
      return list[idx];
    }
    return null;
  },

  async deleteProducto(id) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos/${id}`, {
        method: 'DELETE',
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    let list = getLocalData('productos', FALLBACK_PRODUCTS);
    list = list.filter(p => String(p.id) !== String(id));
    setLocalData('productos', list);
    return { success: true };
  },

  async getPedidos() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/pedidos`, { timeout: 1500 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) {
          setLocalData('pedidos', data);
          return data;
        }
      }
    } catch (e) {}
    return getLocalData('pedidos', FALLBACK_ORDERS);
  },

  async addPedido(pedidoData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/pedidos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(pedidoData),
        timeout: 2500
      });
      if (res.ok) {
        const created = await res.json();
        const list = getLocalData('pedidos', FALLBACK_ORDERS);
        list.unshift(created);
        setLocalData('pedidos', list);
        return created;
      }
    } catch (e) {}
    const list = getLocalData('pedidos', FALLBACK_ORDERS);
    const newOrder = {
      id_pedido: Date.now(),
      nro_pedido: 'PED-' + String(list.length + 1).padStart(4, '0'),
      estado: 'PENDIENTE',
      fecha_pedido: pedidoData.fecha_pedido || new Date().toISOString().split('T')[0],
      fecha_entrega: pedidoData.fecha_entrega || pedidoData.fecha_pedido || new Date().toISOString().split('T')[0],
      nro_orden: pedidoData.nro_orden || pedidoData.nro_orden_compra || '',
      ...pedidoData
    };
    list.unshift(newOrder);
    setLocalData('pedidos', list);
    return newOrder;
  },

  async createPedido(pedidoData) {
    return this.addPedido(pedidoData);
  },

  async updatePedidoStatus(idPedido, payload) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/pedidos/${idPedido}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    const list = getLocalData('pedidos', FALLBACK_ORDERS);
    const order = list.find(o => String(o.id_pedido) === String(idPedido));
    if (order) {
      if (payload.estado) order.estado = payload.estado;
      setLocalData('pedidos', list);
      return order;
    }
    return null;
  },

  async updatePedido(id, fields) {
    return this.updatePedidoStatus(id, fields);
  },

  async getGuias() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias`, { timeout: 1500 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) return data;
      }
    } catch (e) {}
    return getLocalData('guias', FALLBACK_SHIPMENTS);
  },

  async getNextGuiaNumber(serie = 'GR001') {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias/next-number?serie=${encodeURIComponent(serie)}`, { timeout: 1500 });
      if (res.ok) {
        const data = await res.json();
        if (data && data.next_nro_guia) return data.next_nro_guia;
      }
    } catch (e) {}

    const list = getLocalData('guias', FALLBACK_SHIPMENTS);
    const prefix = serie.toUpperCase().startsWith('GR002') ? 'GR002' : 'GR001';
    let maxNum = 0;
    list.forEach(g => {
      if (g.nro_guia && g.nro_guia.startsWith(prefix + '-')) {
        const parts = g.nro_guia.split('-');
        if (parts[1]) {
          const num = parseInt(parts[1], 10);
          if (!isNaN(num) && num > maxNum) maxNum = num;
        }
      }
    });
    return `${prefix}-${String(maxNum + 1).padStart(4, '0')}`;
  },

  async addGuia(guiaData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(guiaData),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    const list = getLocalData('guias', FALLBACK_SHIPMENTS);
    const newGuia = {
      id_guia: Date.now(),
      estado: 'EMITIDA',
      fecha_guia: guiaData.fecha_guia || new Date().toISOString().split('T')[0],
      ...guiaData
    };
    list.unshift(newGuia);
    setLocalData('guias', list);
    return newGuia;
  },

  async createGuia(guiaData) {
    return this.addGuia(guiaData);
  },

  async updateGuia(id, fields) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(fields),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}
    return null;
  },

  async anularGuia(id, motivo) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias/${id}/anular`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ motivo_anulacion: motivo }),
        timeout: 2000
      });
      if (res.ok) return await res.json();
    } catch (e) {}

    const list = getLocalData('guias', FALLBACK_SHIPMENTS);
    const g = list.find(x => String(x.id_guia) === String(id));
    if (g) {
      g.estado = 'ANULADA';
      g.motivo_anulacion = motivo;
      setLocalData('guias', list);
      return g;
    }
    return null;
  },

  // -------------------------------------------------------------
  // LETRAS DE CAMBIO API METHODS
  // -------------------------------------------------------------
  async getLetras(params = {}) {
    const query = new URLSearchParams();
    if (params.search) query.append('search', params.search);
    if (params.dateFrom) query.append('dateFrom', params.dateFrom);
    if (params.dateTo) query.append('dateTo', params.dateTo);
    if (params.estado && params.estado !== 'ALL') query.append('estado', params.estado);

    try {
      const res = await fetchWithTimeout(`${BASE_URL}/letras?${query.toString()}`, { timeout: 2500 });
      if (res.ok) {
        const data = await res.json();
        setLocalData('letras', data);
        return data;
      }
    } catch (e) {
      console.warn("Backend offline or error in getLetras, using localStorage:", e);
    }
    return getLocalData('letras', []);
  },

  async getNextLetraCorrelativo() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/letras/next-correlativo`, { timeout: 2000 });
      if (res.ok) {
        return await res.json();
      }
    } catch (e) {
      console.warn("Backend offline, calculating local correlativo:", e);
    }
    const list = getLocalData('letras', []);
    const anio = new Date().getFullYear();
    const sameYear = list.filter(l => l.anio === anio || (l.nro_letra && l.nro_letra.endsWith(String(anio))));
    const max = sameYear.reduce((acc, curr) => Math.max(acc, Number(curr.numero_correlativo) || 0), 0);
    const next = max + 1;
    return {
      nextCorrelativo: next,
      anio: anio,
      suggestedNroLetra: `${String(next).padStart(3, '0')}-${anio}`
    };
  },

  async createLetrasBatch(letrasArray, storageDir, useSubfolders) {
    const savedPath = storageDir || localStorage.getItem('inplabel_letras_pdf_storage_path') || 'C:\\Inplabel\\Letras';
    const sub = useSubfolders !== undefined ? useSubfolders : (localStorage.getItem('inplabel_pdf_subfolders') !== 'false');

    try {
      const res = await fetchWithTimeout(`${BASE_URL}/letras/batch?storageDir=${encodeURIComponent(savedPath)}&useSubfolders=${sub}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ letras: letrasArray }),
        timeout: 4000
      });
      if (res.ok) {
        const result = await res.json();
        const existing = getLocalData('letras', []);
        const merged = [...(result.letras || letrasArray), ...existing];
        setLocalData('letras', merged);
        return result;
      }
    } catch (e) {
      console.warn("Backend offline, saving letras batch locally:", e);
    }

    const existing = getLocalData('letras', []);
    const idLote = 'LOTE-' + Date.now();
    const created = letrasArray.map((l, idx) => ({
      ...l,
      id_letra: Date.now() + idx,
      id_lote: idLote,
      estado: 'PENDIENTE',
      fecha_creacion: new Date().toISOString()
    }));
    setLocalData('letras', [...created, ...existing]);
    return { success: true, id_lote: idLote, letras: created };
  },

  async anularLetra(idLetra) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/letras/${idLetra}/anular`, {
        method: 'PUT',
        timeout: 2000
      });
      if (res.ok) {
        const data = await res.json();
        const list = getLocalData('letras', []);
        const item = list.find(l => String(l.id_letra) === String(idLetra));
        if (item) {
          item.estado = 'ANULADA';
          setLocalData('letras', list);
        }
        return data;
      }
    } catch (e) {
      console.warn("Backend offline, updating letra locally:", e);
    }

    const list = getLocalData('letras', []);
    const item = list.find(l => String(l.id_letra) === String(idLetra));
    if (item) {
      item.estado = 'ANULADA';
      setLocalData('letras', list);
      return { success: true, message: 'Letra anulada en almacenamiento local' };
    }
    return null;
  },

  async anularLoteLetras(idLote) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/letras/lote/${idLote}/anular`, {
        method: 'PUT',
        timeout: 2500
      });
      if (res.ok) {
        const data = await res.json();
        const list = getLocalData('letras', []);
        list.forEach(l => {
          if (l.id_lote === idLote) l.estado = 'ANULADA';
        });
        setLocalData('letras', list);
        return data;
      }
    } catch (e) {
      console.warn("Backend offline, updating lote locally:", e);
    }

    const list = getLocalData('letras', []);
    list.forEach(l => {
      if (l.id_lote === idLote) l.estado = 'ANULADA';
    });
    setLocalData('letras', list);
    return { success: true, message: 'Lote de letras anulado en almacenamiento local' };
  },

  async login(username, password) {
    const cleanUser = String(username || '').trim().toLowerCase();
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: cleanUser, password }),
        timeout: 6000
      });
      if (res) {
        return await res.json();
      }
    } catch (e) {
      console.warn("Backend login error, attempting offline auth fallback:", e);
    }

    if (cleanUser === 'admin' && password === 'admin123') {
      return {
        success: true,
        message: 'Inicio de sesión (Administrador)',
        user: { idUsuario: 1, username: 'admin', nombreCompleto: 'Administrador Inplabel', rol: 'ADMIN' }
      };
    } else if (cleanUser === 'operaciones' && password === 'operaciones123') {
      return {
        success: true,
        message: 'Inicio de sesión (Operaciones)',
        user: { idUsuario: 2, username: 'operaciones', nombreCompleto: 'Área de Operaciones', rol: 'OPERACIONES' }
      };
    }

    return { success: false, message: 'Usuario o contraseña incorrectos.' };
  }

};

