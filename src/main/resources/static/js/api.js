export const BASE_URL = '/api';
export class ApiError extends Error {
  constructor(message, status = 0) { super(message); this.name = 'ApiError'; this.status = status; }
}
async function fetchWithTimeout(resource, options = {}) {
  const { timeout = 10000, headers = {}, ...fetchOptions } = options;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  const internal = String(resource).startsWith(BASE_URL + '/');
  const write = !['GET', 'HEAD'].includes((fetchOptions.method || 'GET').toUpperCase());
  try {
    const response = await fetch(resource, {
      ...fetchOptions, credentials: internal ? 'same-origin' : 'omit',
      headers: internal ? { ...headers, 'X-Requested-With': 'XMLHttpRequest' } : headers,
      signal: controller.signal
    });
    if (internal && !response.ok) {
      const body = await response.json().catch(() => ({}));
      const login = String(resource).endsWith('/auth/login');
      if (login && response.status === 404) throw new ApiError('El servidor abierto no contiene la API de acceso. Abra http://localhost:8080.', 404);
      if (response.status === 401 && !String(resource).endsWith('/auth/login')) {
        window.dispatchEvent(new Event('session-expired'));
      }
      throw new ApiError(body.message || body.error || 'No se pudo completar la operación.', response.status);
    }
    return response;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (internal && String(resource).endsWith('/auth/login')) throw new ApiError('No se pudo conectar con el servidor de acceso. Inicie Sistema Inplabel y abra http://localhost:8080.');
    if (internal && write) throw new ApiError('No se confirmó el guardado en el servidor. Compruebe los datos antes de reintentar.');
    throw error;
  } finally { clearTimeout(timer); }
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
  } catch (e) { if (e instanceof ApiError) throw e;}
  return fallback;
}

function setLocalData(key, data) {
  try {
    localStorage.setItem('inplabel_' + key, JSON.stringify(data));
  } catch (e) { if (e instanceof ApiError) throw e;}
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
      const res = await fetchWithTimeout(`${BASE_URL}/status`, { timeout: 10000 });
      if (res && res.ok) return await res.json();
    } catch (e) { if (e instanceof ApiError) throw e;}
    return { connected: false, message: 'Spring Boot Backend Desconectado (Modo Local Activo)' };
  },

  async getClientes() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes`, { timeout: 10000 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) {
          setLocalData('clientes', data);
          return data;
        }
      }
    } catch (e) { if (e instanceof ApiError) throw e;}
    return getLocalData('clientes', FALLBACK_CLIENTS);
  },

  async addCliente(clienteData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(clienteData),
        timeout: 10000
      });
      const data = await res.json();
      if (res.ok) {
        const current = getLocalData('clientes', []);
        const newClient = (data && (data.id_cliente || data.id)) ? data : { ...clienteData, id_cliente: Date.now() };
        setLocalData('clientes', [newClient, ...current.filter(c => String(c.nro_documento).trim() !== String(clienteData.nro_documento).trim())]);
        return data || { success: true, ...newClient };
      }
      if (data && (data.error || data.message)) {
        return { success: false, error: data.error || data.message };
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.warn("Error de conexión al agregar cliente:", e);
    }
    // Fallback local storage
    const current = getLocalData('clientes', []);
    const newClient = { ...clienteData, id_cliente: Date.now() };
    setLocalData('clientes', [newClient, ...current.filter(c => String(c.nro_documento).trim() !== String(clienteData.nro_documento).trim())]);
    return { success: true, ...newClient };
  },

  async updateCliente(id, clienteData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(clienteData),
        timeout: 10000
      });
      const data = await res.json();
      if (res.ok) {
        const current = getLocalData('clientes', []);
        const updated = current.map(c => String(c.id_cliente || c.id) === String(id) ? { ...c, ...clienteData } : c);
        setLocalData('clientes', updated);
        return data || { success: true };
      }
      if (data && (data.error || data.message)) {
        return { success: false, error: data.error || data.message };
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.error('Error al actualizar cliente en MySQL:', e);
    }
    const current = getLocalData('clientes', []);
    const updated = current.map(c => String(c.id_cliente || c.id) === String(id) ? { ...c, ...clienteData } : c);
    setLocalData('clientes', updated);
    return { success: true };
  },

  async deleteCliente(id) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/clientes/${id}`, {
        method: 'DELETE',
        timeout: 3000
      });
      const current = getLocalData('clientes', []);
      setLocalData('clientes', current.filter(c => String(c.id_cliente || c.id) !== String(id)));
      if (res.ok) return await res.json();
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.error('Error al eliminar cliente en MySQL:', e);
      const current = getLocalData('clientes', []);
      setLocalData('clientes', current.filter(c => String(c.id_cliente || c.id) !== String(id)));
    }
    return { success: true };
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
    } catch (e) { if (e instanceof ApiError) throw e;
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
    } catch (e) { if (e instanceof ApiError) throw e;}

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
    } catch (e) { if (e instanceof ApiError) throw e;
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
    } catch (e) { if (e instanceof ApiError) throw e;}

    return { success: false };
  },

  async getProductos() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos`, { timeout: 10000 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          setLocalData('productos', data);
          return data;
        }
      }
    } catch (e) { if (e instanceof ApiError) throw e;}
    return getLocalData('productos', FALLBACK_PRODUCTS);
  },

  async addProducto(productoData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productoData),
        timeout: 10000
      });
      const data = await res.json();
      if (res.ok) return data;
      if (data && (data.error || data.message)) {
        return { success: false, error: data.error || data.message };
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.warn("Error al registrar producto:", e);
    }
    return { success: false, error: 'No se pudo conectar con el servidor para registrar el producto.' };
  },

  async updateProducto(id, productoData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productoData),
        timeout: 10000
      });
      const data = await res.json();
      if (res.ok) return data;
      if (data && (data.error || data.message)) {
        return { success: false, error: data.error || data.message };
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.warn("Error al actualizar producto:", e);
    }
    return { success: false, error: 'No se pudo actualizar el producto.' };
  },

  async deleteProducto(id) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/productos/${id}`, {
        method: 'DELETE',
        timeout: 10000
      });
      if (res.ok) return await res.json();
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');}
    let list = getLocalData('productos', FALLBACK_PRODUCTS);
    list = list.filter(p => String(p.id) !== String(id));
    setLocalData('productos', list);
    return { success: true };
  },

  async getPedidos() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/pedidos`, { timeout: 10000 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          setLocalData('pedidos', data);
          return data;
        }
      }
    } catch (e) { if (e instanceof ApiError) throw e;}
    return getLocalData('pedidos', FALLBACK_ORDERS);
  },

  async addPedido(pedidoData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/pedidos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(pedidoData),
        timeout: 20000
      });
      if (res.ok) {
        const created = await res.json();
        const list = getLocalData('pedidos', FALLBACK_ORDERS);
        list.unshift(created);
        setLocalData('pedidos', list);
        return created;
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');}
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
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');}
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
      const res = await fetchWithTimeout(`${BASE_URL}/guias`, { timeout: 10000 });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) return data;
      }
    } catch (e) { if (e instanceof ApiError) throw e;}
    return getLocalData('guias', FALLBACK_SHIPMENTS);
  },

  async getNextGuiaNumber(serie = 'GR001') {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias/next-number?serie=${encodeURIComponent(serie)}`, { timeout: 1500 });
      if (res.ok) {
        const data = await res.json();
        if (data && data.next_nro_guia) return data.next_nro_guia;
      }
    } catch (e) { if (e instanceof ApiError) throw e;}

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
        timeout: 25000
      });
      if (res.ok) {
        const saved = await res.json();
        const list = getLocalData('guias', FALLBACK_SHIPMENTS);
        const idx = list.findIndex(g => String(g.id_guia) === String(saved.id_guia));
        if (idx >= 0) {
          list[idx] = saved;
        } else {
          list.unshift(saved);
        }
        setLocalData('guias', list);
        return saved;
      } else {
        const errText = await res.text();
        throw new Error(errText || `Error del servidor HTTP ${res.status}`);
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.error("Error al emitir guía en addGuia:", e);
      throw e;
    }
  },

  async createGuia(guiaData) {
    return this.addGuia(guiaData);
  },

  async addEnvioPedido(envioData) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/envios-pedido`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(envioData),
        timeout: 3000
      });
      if (res.ok) return await res.json();
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');}
    return null;
  },

  async getShipments() {
    return this.getGuias();
  },

  async updateGuia(id, fields) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/guias/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(fields),
        timeout: 10000
      });
      if (res.ok) {
        const data = await res.json();
        const list = getLocalData('guias', FALLBACK_SHIPMENTS);
        const idx = list.findIndex(g => String(g.id_guia) === String(id));
        if (idx !== -1) {
          list[idx] = { ...list[idx], ...fields, ...data };
          setLocalData('guias', list);
        }
        return data;
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.warn("Error actualizando guía en backend:", e);
    }

    const list = getLocalData('guias', FALLBACK_SHIPMENTS);
    const idx = list.findIndex(g => String(g.id_guia) === String(id));
    if (idx !== -1) {
      list[idx] = { ...list[idx], ...fields };
      setLocalData('guias', list);
      return list[idx];
    }
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
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');}

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
    } catch (e) { if (e instanceof ApiError) throw e;
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
    } catch (e) { if (e instanceof ApiError) throw e;
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
        timeout: 25000
      });
      if (res.ok) {
        const result = await res.json();
        const existing = getLocalData('letras', []);
        const merged = [...(result.letras || letrasArray), ...existing];
        setLocalData('letras', merged);
        return result;
      }
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
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
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
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
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
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
    const res = await fetchWithTimeout(BASE_URL + '/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: String(username || '').trim().toLowerCase(), password }), timeout: 10000
    });
    return res.json();
  },
  async session() {
    const res = await fetchWithTimeout(BASE_URL + '/auth/me');
    return res.json();
  },
  async logout() {
    await fetchWithTimeout(BASE_URL + '/auth/logout', { method: 'POST' });
  },
  async getUsers() {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/usuarios`, { timeout: 8000 });
      if (res.ok) return await res.json();
    } catch (e) { if (e instanceof ApiError) throw e;
      console.warn("Error fetching users from API:", e);
    }
    return [];
  },

  async createUser(payload) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/usuarios`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        timeout: 8000
      });
      return await res.json();
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.error("Error creating user:", e);
      return { message: "Error al conectar con el servidor." };
    }
  },

  async updateUser(id, payload) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/usuarios/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        timeout: 8000
      });
      return await res.json();
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.error("Error updating user:", e);
      return { message: "Error al conectar con el servidor." };
    }
  },

  async toggleUserActive(id) {
    try {
      const res = await fetchWithTimeout(`${BASE_URL}/usuarios/${id}/toggle-active`, {
        method: 'PUT',
        timeout: 8000
      });
      return await res.json();
    } catch (e) { throw e instanceof ApiError ? e : new ApiError('No se pudo confirmar la operación en el servidor.');
      console.error("Error toggling user active state:", e);
      return { message: "Error al conectar con el servidor." };
    }
  }
};

