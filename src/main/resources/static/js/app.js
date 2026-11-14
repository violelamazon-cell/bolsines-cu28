/**
 * CU 28 - Registrar recepción de bolsín
 * Frontend - Fase 4 (con Login, Homepage y Hash Routing)
 */

(function () {
  'use strict';

  // =========================================================================
  // Estado global
  // =========================================================================
  const state = {
    // Sesión
    currentEmpleadoId: null,
    currentNombreCompleto: '',
    currentPrimerNombre: '',
    currentComisionMedica: '',
    currentCodigoCm: '',
    empleados: [],

    // Asistente CU 28
    currentStep: 1,
    listaBolsinesOriginal: [],
    selectedBolsin: null,
    bolsinDetalle: null,
    opcionElegida: null,
    ultimoReintento: null,
    isConfirmingOrCancelling: false,
    seleccionRecepcionRespuesta: null,
    resultadoConfirmacion: null
  };

  window.__appState = state;

  // =========================================================================
  // Hash routing
  // =========================================================================

  const SCREENS = {
    login: document.getElementById('screen-login'),
    inicio: document.getElementById('screen-inicio'),
    recepcion: document.getElementById('screen-recepcion')
  };

  function getHash() {
    return window.location.hash.replace('#', '') || 'login';
  }

  function navigate(hash, pushState = true) {
    if (pushState) {
      window.location.hash = hash;
    }
    renderScreen(hash);
  }

  function renderScreen(hash) {
    // Ocultar todas las pantallas
    Object.values(SCREENS).forEach(s => {
      if (s) {
        s.classList.remove('screen-active');
        s.setAttribute('hidden', '');
      }
    });

    // Guard: si no hay sesión y se intenta ir a inicio o recepcion → login
    if ((hash === 'inicio' || hash === 'recepcion') && !state.currentEmpleadoId) {
      hash = 'login';
    }

    const target = SCREENS[hash] || SCREENS['login'];
    if (target) {
      target.removeAttribute('hidden');
      target.classList.add('screen-active');
    }

    if (hash === 'inicio') {
      actualizarHomepage();
      cargarContadorPendientes();
    } else if (hash === 'recepcion') {
      actualizarHeaderRecepcion();
    }
  }

  window.addEventListener('hashchange', () => {
    renderScreen(getHash());
  });

  // =========================================================================
  // Elementos del DOM – Login
  // =========================================================================
  const loginEl = {
    cargando: document.getElementById('login-cargando'),
    error: document.getElementById('login-error'),
    errorMsg: document.getElementById('login-error-msg'),
    grid: document.getElementById('login-grid')
  };

  // =========================================================================
  // Elementos del DOM – Homepage
  // =========================================================================
  const homeEl = {
    cmBadge: document.getElementById('home-cm-badge'),
    nombreUsuario: document.getElementById('home-nombre-usuario'),
    saludo: document.getElementById('home-saludo'),
    rolCm: document.getElementById('home-rol-cm'),
    pendientesCount: document.getElementById('home-pendientes-count'),
    btnComenzarCU28: document.getElementById('btn-comenzar-cu28'),
    btnCerrarSesion: document.getElementById('btn-cerrar-sesion-home')
  };

  // =========================================================================
  // Elementos del DOM – Pantalla Recepción (asistente CU 28)
  // =========================================================================
  const el = {
    // Header recepción
    cmBadge: document.getElementById('cm-badge'),
    recNombreUsuario: document.getElementById('rec-nombre-usuario'),
    btnCerrarSesionRec: document.getElementById('btn-cerrar-sesion-rec'),
    btnVolverInicio: document.getElementById('btn-volver-inicio'),

    bannerAvisoUsuario: document.getElementById('banner-aviso-usuario'),

    // Stepper
    stepperItems: document.querySelectorAll('.stepper .step'),
    compactStepNumber: document.getElementById('compact-step-number'),
    compactStepName: document.getElementById('compact-step-name'),

    // Paso 1
    pasoTitulo: document.getElementById('paso-titulo'),
    cmUsuarioSubtitulo: document.getElementById('cm-usuario-subtitulo'),
    formFiltros: document.getElementById('form-filtros'),
    filtroPrecinto: document.getElementById('filtro-precinto'),
    filtroCmOrigen: document.getElementById('filtro-cm-origen'),
    btnBuscar: document.getElementById('btn-buscar'),
    btnLimpiar: document.getElementById('btn-limpiar'),
    bannerFiltroError: document.getElementById('banner-filtro-error'),
    bannerFiltroMensaje: document.getElementById('banner-filtro-mensaje'),
    bannerA6: document.getElementById('banner-a6'),
    bannerA6Mensaje: document.getElementById('banner-a6-mensaje'),
    bannerErrorConexion: document.getElementById('banner-error-conexion'),
    mensajeErrorConexion: document.getElementById('mensaje-error-conexion'),
    btnReintentar: document.getElementById('btn-reintentar'),
    estadoCargando: document.getElementById('estado-cargando'),
    contenedorTabla: document.getElementById('contenedor-tabla'),
    tablaBolsinesBody: document.getElementById('tabla-bolsines-body'),
    estadoVacio: document.getElementById('estado-vacio'),
    btnCancelarPaso1: document.getElementById('btn-cancelar-paso-1'),

    // Paneles de pasos
    paso1: document.getElementById('paso-1'),
    paso2: document.getElementById('paso-2'),
    paso3: document.getElementById('paso-3'),
    paso4: document.getElementById('paso-4'),

    // Paso 2
    p2Resumen: document.getElementById('p2-resumen'),
    p2Cargando: document.getElementById('p2-cargando'),
    p2Error: document.getElementById('p2-error'),
    p2ErrorMensaje: document.getElementById('p2-error-mensaje'),
    p2Remitos: document.getElementById('p2-remitos'),
    p2RemitosLista: document.getElementById('p2-remitos-lista'),
    p2Opciones: document.getElementById('p2-opciones'),
    p2OpcionesLista: document.getElementById('p2-opciones-lista'),
    btnVolverP2: document.getElementById('btn-volver-p2'),
    btnCancelarP2: document.getElementById('btn-cancelar-p2'),
    btnContinuarP2: document.getElementById('btn-continuar-p2'),

    // Paso 3
    p3Titulo: document.getElementById('p3-titulo'),
    dialogConfirmacion: document.getElementById('dialog-confirmacion'),
    dialogNumeroBolsin: document.getElementById('dialog-numero-bolsin'),
    dialogResumenDocs: document.getElementById('dialog-resumen-docs'),
    dialogResumenRemitos: document.getElementById('dialog-resumen-remitos'),
    dialogResumenServidor: document.getElementById('dialog-resumen-servidor'),
    dialogError: document.getElementById('dialog-error'),
    dialogErrorMensaje: document.getElementById('dialog-error-mensaje'),
    btnNoConfirmar: document.getElementById('btn-no-confirmar'),
    btnConfirmarRecepcion: document.getElementById('btn-confirmar-recepcion'),

    // Paso 4
    p4Titulo: document.getElementById('p4-titulo'),
    p4Subtitulo: document.getElementById('p4-subtitulo'),
    p4VistaExito: document.getElementById('p4-vista-exito'),
    p4VistaError: document.getElementById('p4-vista-error'),
    p4TituloError: document.getElementById('p4-titulo-error'),
    p4ErrorMensajeTexto: document.getElementById('p4-error-mensaje-texto'),
    p4ResumenLista: document.getElementById('p4-resumen-lista'),
    p4NotificacionesLista: document.getElementById('p4-notificaciones-lista'),
    btnReiniciarDesdeP4: document.getElementById('btn-reiniciar-desde-p4'),
    btnVolverInicioError: document.getElementById('btn-volver-inicio-error')
  };

  // =========================================================================
  // Helpers
  // =========================================================================

  function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function extraerNombreCm(obj) {
    if (!obj) return '';
    const cm = obj.nombreCm ?? obj.comisionMedica ?? obj.nombreComisionMedica ?? obj.cm ?? obj.comisionMedicaOrigen;
    if (typeof cm === 'string') return cm;
    if (cm && typeof cm === 'object') return cm.nombreCm ?? cm.nombre ?? cm.descripcion ?? '';
    return '';
  }

  function extraerEmpleadoId(emp) {
    return emp.id ?? emp.empleadoId;
  }

  function extraerPrimerNombre(nombreCompleto) {
    if (!nombreCompleto) return '';
    // "Carlos Gómez" → "Carlos"; "Gómez, Carlos" → "Carlos"
    const partes = nombreCompleto.split(',');
    if (partes.length > 1) return partes[1].trim().split(' ')[0];
    return nombreCompleto.trim().split(' ')[0];
  }

  function obtenerIniciales(nombreCompleto) {
    if (!nombreCompleto) return '??';
    const partes = nombreCompleto.trim().split(/[\s,]+/).filter(Boolean);
    if (partes.length === 0) return '??';
    if (partes.length === 1) return partes[0].substring(0, 2).toUpperCase();
    // "Gómez, Carlos" → ["Gómez","Carlos"] → "CG"
    // "Carlos Gómez" → ["Carlos","Gómez"] → "CG"
    return (partes[0][0] + partes[1][0]).toUpperCase();
  }

  function extraerNombreCompleto(emp) {
    if (emp.nombreCompleto) return emp.nombreCompleto;
    if (emp.nombre && emp.apellido) return `${emp.nombre} ${emp.apellido}`;
    if (emp.apellido && !emp.nombre) return emp.apellido;
    if (emp.nombre) return emp.nombre;
    return `Empleado ${extraerEmpleadoId(emp)}`;
  }

  function formatearRemitos(cantidad) {
    return `${cantidad} ${cantidad === 1 ? 'remito' : 'remitos'}`;
  }

  function formatearDocumentaciones(cantidad) {
    return `${cantidad} ${cantidad === 1 ? 'documentación' : 'documentaciones'}`;
  }

  // =========================================================================
  // Fetch helper
  // =========================================================================

  async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');
    if (state.currentEmpleadoId) headers.set('X-Empleado-Id', String(state.currentEmpleadoId));
    return fetch(url, { ...options, headers });
  }

  // =========================================================================
  // Login
  // =========================================================================

  async function cargarLogin() {
    loginEl.cargando.hidden = false;
    loginEl.error.hidden = true;
    loginEl.grid.innerHTML = '';

    try {
      const res = await fetch('/api/sesion/empleados');
      loginEl.cargando.hidden = true;

      if (!res.ok) throw new Error('Error al cargar empleados');
      const data = await res.json();
      const empleados = Array.isArray(data) ? data : (data?.empleados || []);
      state.empleados = empleados;
      renderLoginGrid(empleados);
    } catch (err) {
      loginEl.cargando.hidden = true;
      loginEl.error.hidden = false;
      loginEl.errorMsg.textContent = 'No se pudieron cargar los usuarios. Recargá la página.';
    }
  }

  function renderLoginGrid(empleados) {
    loginEl.grid.innerHTML = '';
    if (!empleados || empleados.length === 0) {
      loginEl.error.hidden = false;
      loginEl.errorMsg.textContent = 'No hay usuarios disponibles.';
      return;
    }

    empleados.forEach(emp => {
      const id = extraerEmpleadoId(emp);
      const nombre = extraerNombreCompleto(emp);
      const cm = extraerNombreCm(emp);
      const iniciales = obtenerIniciales(nombre);

      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'login-user-card';
      btn.setAttribute('role', 'listitem');
      btn.setAttribute('aria-label', `Iniciar sesión como ${nombre}, ${cm}`);
      btn.dataset.empleadoId = String(id);

      btn.innerHTML = `
        <span class="login-user-avatar" aria-hidden="true">${escapeHtml(iniciales)}</span>
        <span class="login-user-info-text">
          <span class="login-user-nombre">${escapeHtml(nombre)}</span>
          <span class="login-user-cm">${escapeHtml(cm)}</span>
        </span>
      `;

      btn.addEventListener('click', () => iniciarSesion(emp, btn));
      btn.addEventListener('keydown', e => {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); iniciarSesion(emp, btn); }
      });

      loginEl.grid.appendChild(btn);
    });
  }

  async function iniciarSesion(emp, btnEl) {
    const id = extraerEmpleadoId(emp);

    // Deshabilitar todas las tarjetas mientras procesa
    loginEl.grid.querySelectorAll('.login-user-card').forEach(c => {
      c.setAttribute('aria-busy', 'true');
      c.disabled = true;
    });
    if (btnEl) btnEl.classList.add('selected');

    try {
      const res = await fetch('/api/sesion/iniciar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ empleadoId: id })
      });

      let sesionData = null;
      try { sesionData = await res.json(); } catch (_) {}

      // Leer CM de la respuesta de /iniciar o del objeto empleado
      const cm = extraerNombreCm(sesionData) || extraerNombreCm(emp);
      const nombre = extraerNombreCompleto(sesionData) || extraerNombreCompleto(emp);

      state.currentEmpleadoId = id;
      state.currentNombreCompleto = nombre;
      state.currentPrimerNombre = extraerPrimerNombre(nombre);
      state.currentComisionMedica = cm;

    } catch (err) {
      // Si falla el POST, igual permitimos entrar con los datos del empleado
      state.currentEmpleadoId = id;
      state.currentNombreCompleto = extraerNombreCompleto(emp);
      state.currentPrimerNombre = extraerPrimerNombre(state.currentNombreCompleto);
      state.currentComisionMedica = extraerNombreCm(emp);
    }

    // Restaurar tarjetas
    loginEl.grid.querySelectorAll('.login-user-card').forEach(c => {
      c.removeAttribute('aria-busy');
      c.disabled = false;
    });

    navigate('inicio');
  }

  function cerrarSesion() {
    state.currentEmpleadoId = null;
    state.currentNombreCompleto = '';
    state.currentPrimerNombre = '';
    state.currentComisionMedica = '';
    state.currentCodigoCm = '';
    // Limpiar estado del asistente
    reiniciarEstadoAsistente();
    navigate('login');
  }

  // =========================================================================
  // Homepage
  // =========================================================================

  function actualizarHomepage() {
    if (homeEl.cmBadge) homeEl.cmBadge.textContent = state.currentComisionMedica || '';
    if (homeEl.nombreUsuario) homeEl.nombreUsuario.textContent = state.currentNombreCompleto || '';
    if (homeEl.saludo) homeEl.saludo.textContent = `Hola, ${state.currentPrimerNombre || state.currentNombreCompleto}`;
    if (homeEl.rolCm) homeEl.rolCm.textContent = `Encargado de Bolsines · ${state.currentComisionMedica}`;

    // Foco accesible en el saludo
    if (homeEl.saludo) {
      homeEl.saludo.setAttribute('tabindex', '-1');
      homeEl.saludo.focus();
    }
  }

  async function cargarContadorPendientes() {
    if (!homeEl.pendientesCount) return;
    homeEl.pendientesCount.textContent = '…';

    try {
      const res = await apiFetch('/api/recepcion-bolsin/iniciar', { method: 'POST' });
      if (res.ok) {
        const data = await res.json();
        const bolsines = Array.isArray(data)
          ? data
          : (data?.bolsines || data?.bolsinesEnviados || []);
        state.listaBolsinesOriginal = bolsines;
        homeEl.pendientesCount.textContent = String(bolsines.length);
      } else if (res.status === 404) {
        state.listaBolsinesOriginal = [];
        homeEl.pendientesCount.textContent = '0';
      } else {
        homeEl.pendientesCount.textContent = '—';
      }
    } catch (_) {
      homeEl.pendientesCount.textContent = '—';
    }
  }

  // =========================================================================
  // Recepción – Header
  // =========================================================================

  function actualizarHeaderRecepcion() {
    if (el.cmBadge) el.cmBadge.textContent = state.currentComisionMedica || '';
    if (el.recNombreUsuario) el.recNombreUsuario.textContent = state.currentNombreCompleto || '';
    if (el.cmUsuarioSubtitulo) el.cmUsuarioSubtitulo.textContent = state.currentComisionMedica || '';
  }

  function actualizarCmEnUi(nombreCm) {
    if (!nombreCm) return;
    state.currentComisionMedica = nombreCm;
    if (el.cmBadge) el.cmBadge.textContent = nombreCm;
    if (el.cmUsuarioSubtitulo) el.cmUsuarioSubtitulo.textContent = nombreCm;
  }

  // =========================================================================
  // ASISTENTE – Stepper y navegación
  // =========================================================================

  const ETAPAS = [
    { num: 1, name: 'Bolsín' },
    { num: 2, name: 'Revisión' },
    { num: 3, name: 'Confirmación' },
    { num: 4, name: 'Resultado' }
  ];

  function goToStep(stepNumber) {
    state.currentStep = stepNumber;

    el.stepperItems.forEach(item => {
      const stepIdx = parseInt(item.dataset.step, 10);
      const badge = item.querySelector('.step-badge');
      item.classList.remove('current', 'completed', 'pending');
      item.removeAttribute('aria-current');

      if (stepIdx < stepNumber) {
        item.classList.add('completed');
        if (badge) badge.textContent = '✔';
      } else if (stepIdx === stepNumber) {
        item.classList.add('current');
        item.setAttribute('aria-current', 'step');
        if (badge) badge.textContent = String(stepIdx);
      } else {
        item.classList.add('pending');
        if (badge) badge.textContent = String(stepIdx);
      }
    });

    if (el.compactStepNumber && el.compactStepName) {
      el.compactStepNumber.textContent = String(stepNumber);
      el.compactStepName.textContent = ETAPAS[stepNumber - 1]?.name || '';
    }

    [1, 2, 3, 4].forEach(n => {
      const panel = el[`paso${n}`];
      if (panel) {
        if (n === stepNumber) panel.removeAttribute('hidden');
        else panel.setAttribute('hidden', '');
      }
    });

    if (stepNumber === 1 && el.pasoTitulo) {
      el.pasoTitulo.focus();
    } else if (stepNumber === 3 && el.p3Titulo) {
      el.p3Titulo.focus();
    } else if (stepNumber === 4) {
      if (el.p4VistaError && !el.p4VistaError.hidden && el.p4TituloError) {
        el.p4TituloError.focus();
      } else if (el.p4Titulo) {
        el.p4Titulo.focus();
      }
    }
  }

  // =========================================================================
  // ASISTENTE – Paso 1: estado visual y tabla
  // =========================================================================

  function setCargando(cargando) {
    if (el.estadoCargando) el.estadoCargando.hidden = !cargando;
    if (el.btnBuscar) el.btnBuscar.disabled = cargando;
    if (el.btnLimpiar) el.btnLimpiar.disabled = cargando;
  }

  function ocultarBanners(mantenerA6 = false) {
    if (el.bannerFiltroError) el.bannerFiltroError.hidden = true;
    if (el.bannerErrorConexion) el.bannerErrorConexion.hidden = true;
    if (!mantenerA6 && el.bannerA6) el.bannerA6.hidden = true;
  }

  function mostrarBannerA6(mensaje) {
    if (el.bannerA6 && el.bannerA6Mensaje) {
      el.bannerA6Mensaje.textContent = mensaje || 'No se registró la recepción. No se modificó ningún dato.';
      el.bannerA6.hidden = false;
    }
  }

  function mostrarErrorConexion(retryFn) {
    state.ultimoReintento = retryFn;
    setCargando(false);
    if (el.contenedorTabla) el.contenedorTabla.hidden = true;
    if (el.estadoVacio) el.estadoVacio.hidden = true;
    if (el.bannerErrorConexion) {
      el.mensajeErrorConexion.textContent = 'Ocurrió un problema. Intentá de nuevo en unos segundos.';
      el.bannerErrorConexion.hidden = false;
    }
  }

  function mostrarBannerFiltro(mensaje) {
    if (el.bannerFiltroError && el.bannerFiltroMensaje) {
      el.bannerFiltroMensaje.textContent = mensaje || 'No se encontraron bolsines con el filtro ingresado.';
      el.bannerFiltroError.hidden = false;
    }
  }

  function renderTablaBolsines(bolsines) {
    el.tablaBolsinesBody.innerHTML = '';

    if (!bolsines || bolsines.length === 0) {
      el.contenedorTabla.hidden = true;
      el.estadoVacio.hidden = false;
      return;
    }

    el.contenedorTabla.hidden = false;
    el.estadoVacio.hidden = true;

    bolsines.forEach(bolsin => {
      const numBolsin = bolsin.numeroBolsin ?? bolsin.numero ?? bolsin.id;
      const precinto = bolsin.numeroPrecinto ?? bolsin.precinto ?? '-';
      const cmOrigen = bolsin.cmOrigen ?? bolsin.comisionMedicaOrigen ?? (bolsin.comisionMedica?.nombre) ?? bolsin.origen ?? '-';

      const tr = document.createElement('tr');
      tr.setAttribute('tabindex', '0');
      tr.setAttribute('aria-selected', 'false');
      tr.setAttribute('aria-label', `Seleccionar bolsín precinto ${precinto} de ${cmOrigen}`);
      tr.dataset.numeroBolsin = String(numBolsin);

      tr.innerHTML = `
        <td>${escapeHtml(cmOrigen)}</td>
        <td class="tabular-nums">${escapeHtml(String(precinto))}</td>
        <td>
          <button type="button" class="btn-seleccionar secondary outline" data-numero-bolsin="${escapeHtml(String(numBolsin))}">
            Seleccionar
          </button>
        </td>
      `;

      tr.addEventListener('click', () => seleccionarFila(bolsin, tr));
      tr.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); seleccionarFila(bolsin, tr); }
      });

      const btn = tr.querySelector('.btn-seleccionar');
      if (btn) btn.addEventListener('click', (e) => { e.stopPropagation(); seleccionarFila(bolsin, tr); });

      el.tablaBolsinesBody.appendChild(tr);
    });
  }

  function seleccionarFila(bolsin, trElement) {
    state.selectedBolsin = bolsin;
    el.tablaBolsinesBody.querySelectorAll('tr').forEach(row => {
      row.classList.remove('selected');
      row.setAttribute('aria-selected', 'false');
    });
    trElement.classList.add('selected');
    trElement.setAttribute('aria-selected', 'true');
    seleccionarBolsin(bolsin.numeroBolsin ?? bolsin.numero ?? bolsin.id);
  }

  async function cargarBolsinesIniciales(mantenerA6 = false) {
    setCargando(true);
    ocultarBanners(mantenerA6);
    el.estadoVacio.hidden = true;
    el.contenedorTabla.hidden = true;

    try {
      // Si ya tenemos la lista cacheada desde la homepage, la usamos
      if (state.listaBolsinesOriginal && state.listaBolsinesOriginal.length > 0 && !mantenerA6) {
        setCargando(false);
        renderTablaBolsines(state.listaBolsinesOriginal);
        return;
      }

      const res = await apiFetch('/api/recepcion-bolsin/iniciar', { method: 'POST' });
      setCargando(false);

      if (res.ok) {
        let data = null;
        try { data = await res.json(); } catch (_) {}
        const list = Array.isArray(data) ? data : (data?.bolsines || data?.bolsinesEnviados || []);
        state.listaBolsinesOriginal = list;
        renderTablaBolsines(list);
      } else if (res.status === 404) {
        state.listaBolsinesOriginal = [];
        el.contenedorTabla.hidden = true;
        el.estadoVacio.hidden = false;
      } else {
        mostrarErrorConexion(() => cargarBolsinesIniciales(mantenerA6));
      }
    } catch (err) {
      mostrarErrorConexion(() => cargarBolsinesIniciales(mantenerA6));
    }
  }

  async function buscarBolsines(e) {
    if (e) e.preventDefault();
    const precinto = el.filtroPrecinto.value.trim();
    const cmOrigen = el.filtroCmOrigen.value.trim();
    if (!precinto && !cmOrigen) { limpiarFiltros(); return; }

    setCargando(true);
    ocultarBanners();

    const params = [];
    if (precinto) params.push(`numeroPrecinto=${encodeURIComponent(precinto)}`);
    if (cmOrigen) params.push(`cmOrigen=${encodeURIComponent(cmOrigen)}`);
    const url = `/api/recepcion-bolsin/bolsines?${params.join('&')}`;

    try {
      const res = await apiFetch(url);
      setCargando(false);
      if (res.ok) {
        let data = null;
        try { data = await res.json(); } catch (_) {}
        const bolsines = Array.isArray(data) ? data : (data?.bolsines || data?.bolsinesEnviados || []);
        renderTablaBolsines(bolsines);
        if (el.bannerFiltroError) el.bannerFiltroError.hidden = true;
      } else if (res.status === 404) {
        let mensaje = 'No se encontró ningún bolsín con el criterio ingresado.';
        try { const e = await res.json(); if (e?.mensaje) mensaje = e.mensaje; } catch (_) {}
        mostrarBannerFiltro(mensaje);
      } else {
        let mensaje = 'Ocurrió un problema. Intentá de nuevo en unos segundos.';
        try { const e = await res.json(); if (e?.mensaje) mensaje = e.mensaje; } catch (_) {}
        mostrarBannerFiltro(mensaje);
      }
    } catch (err) {
      mostrarErrorConexion(() => buscarBolsines());
    }
  }

  function limpiarFiltros() {
    if (el.filtroPrecinto) el.filtroPrecinto.value = '';
    if (el.filtroCmOrigen) el.filtroCmOrigen.value = '';
    ocultarBanners();
    if (state.listaBolsinesOriginal && state.listaBolsinesOriginal.length > 0) {
      renderTablaBolsines(state.listaBolsinesOriginal);
      el.contenedorTabla.hidden = false;
      el.estadoVacio.hidden = true;
    } else {
      cargarBolsinesIniciales();
    }
  }

  function reiniciarEstadoAsistente() {
    state.currentStep = 1;
    state.selectedBolsin = null;
    state.bolsinDetalle = null;
    state.opcionElegida = null;
    state.seleccionRecepcionRespuesta = null;
    state.resultadoConfirmacion = null;
    state.listaBolsinesOriginal = [];
  }

  function reiniciarWizard() {
    goToStep(1);
    if (el.filtroPrecinto) el.filtroPrecinto.value = '';
    if (el.filtroCmOrigen) el.filtroCmOrigen.value = '';
    ocultarBanners();
    state.selectedBolsin = null;
    state.bolsinDetalle = null;
    state.opcionElegida = null;
    state.seleccionRecepcionRespuesta = null;
    state.resultadoConfirmacion = null;
    state.listaBolsinesOriginal = [];
    cargarBolsinesIniciales();
  }

  function volverPaso1ConA6(mensaje) {
    goToStep(1);
    if (el.filtroPrecinto) el.filtroPrecinto.value = '';
    if (el.filtroCmOrigen) el.filtroCmOrigen.value = '';
    ocultarBanners(true);
    mostrarBannerA6(mensaje || 'No se registró la recepción. No se modificó ningún dato.');
    state.selectedBolsin = null;
    state.bolsinDetalle = null;
    state.opcionElegida = null;
    state.seleccionRecepcionRespuesta = null;
    state.resultadoConfirmacion = null;
    state.listaBolsinesOriginal = [];
    cargarBolsinesIniciales(true);
  }

  async function cancelarProceso() {
    try { await apiFetch('/api/recepcion-bolsin/cancelar', { method: 'POST' }); } catch (_) {}
    reiniciarWizard();
  }

  // =========================================================================
  // ASISTENTE – Paso 2: Revisión
  // =========================================================================

  async function seleccionarBolsin(numeroBolsin) {
    goToStep(2);
    state.opcionElegida = null;
    state.bolsinDetalle = null;

    el.p2Resumen.innerHTML = '';
    el.p2Resumen.hidden = true;
    el.p2Remitos.hidden = true;
    el.p2Opciones.hidden = true;
    el.p2Error.hidden = true;
    el.p2Cargando.hidden = false;
    el.btnContinuarP2.disabled = true;

    try {
      const res = await apiFetch('/api/recepcion-bolsin/seleccionar-bolsin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ numeroBolsin: Number(numeroBolsin) })
      });

      el.p2Cargando.hidden = true;

      if (!res.ok) {
        let msg = 'Ocurrió un problema. Intentá de nuevo en unos segundos.';
        try { const e = await res.json(); if (e?.mensaje) msg = e.mensaje; } catch (_) {}
        mostrarErrorP2(msg);
        return;
      }

      const data = await res.json();
      state.bolsinDetalle = data;
      renderPaso2(data);
    } catch (err) {
      el.p2Cargando.hidden = true;
      mostrarErrorP2('Ocurrió un problema. Intentá de nuevo en unos segundos.');
    }
  }

  function mostrarErrorP2(mensaje) {
    el.p2ErrorMensaje.textContent = mensaje;
    el.p2Error.hidden = false;
    el.p2Resumen.hidden = true;
    el.p2Remitos.hidden = true;
    el.p2Opciones.hidden = true;
  }

  function renderPaso2(data) {
    const remitos = data.remitos || [];
    const totalDocs = remitos.reduce((acc, r) => acc + (r.documentaciones || []).length, 0);

    el.p2Resumen.innerHTML = `
      <div class="resumen-campo">
        <span class="resumen-label">Bolsín N°</span>
        <span class="resumen-valor">${escapeHtml(String(data.numeroBolsin))}</span>
      </div>
      <div class="resumen-campo">
        <span class="resumen-label">Precinto</span>
        <span class="resumen-valor">${escapeHtml(String(data.numeroPrecinto ?? '-'))}</span>
      </div>
      <div class="resumen-campo">
        <span class="resumen-label">Origen</span>
        <span class="resumen-valor">${escapeHtml(data.cmOrigen ?? '-')}</span>
      </div>
      <div class="resumen-campo">
        <span class="resumen-label">Contenido</span>
        <span class="resumen-valor">${formatearRemitos(remitos.length)} · ${formatearDocumentaciones(totalDocs)}</span>
      </div>
    `;
    el.p2Resumen.hidden = false;

    el.p2RemitosLista.innerHTML = '';
    remitos.forEach((remito, idx) => {
      const docs = remito.documentaciones || [];
      const details = document.createElement('details');
      details.className = 'remito-details';
      if (idx === 0) details.setAttribute('open', '');
      details.innerHTML = `
        <summary>Remito N° ${escapeHtml(String(remito.numeroRemito))} · ${formatearDocumentaciones(docs.length)}</summary>
        <div class="remito-tabla-wrapper">
          <table aria-label="Documentaciones del Remito N° ${escapeHtml(String(remito.numeroRemito))}">
            <thead>
              <tr>
                <th scope="col">Tipo de documento</th>
                <th scope="col">N°</th>
                <th scope="col">Asunto</th>
              </tr>
            </thead>
            <tbody>
              ${docs.map(doc => `
                <tr>
                  <td>${escapeHtml(doc.tipoDocumento ?? '-')}</td>
                  <td class="tabular-nums">${escapeHtml(String(doc.numero ?? '-'))}</td>
                  <td>${escapeHtml(doc.asunto ?? '-')}</td>
                </tr>
              `).join('')}
              ${docs.length === 0 ? '<tr><td colspan="3">Sin documentaciones</td></tr>' : ''}
            </tbody>
          </table>
        </div>
      `;
      el.p2RemitosLista.appendChild(details);
    });
    el.p2Remitos.hidden = false;

    const opciones = data.opcionesRecepcion || [];
    el.p2OpcionesLista.innerHTML = '';
    state.opcionElegida = null;
    el.btnContinuarP2.disabled = true;

    opciones.forEach(opcion => {
      const wrapper = document.createElement('div');
      wrapper.className = `opcion-card${!opcion.habilitada ? ' disabled' : ''}`;
      const inputId = `opcion-${opcion.codigo}`;
      const input = document.createElement('input');
      input.type = 'radio';
      input.name = 'opcion-recepcion';
      input.id = inputId;
      input.value = String(opcion.codigo);
      input.disabled = !opcion.habilitada;
      input.addEventListener('change', () => {
        if (input.checked) { state.opcionElegida = opcion.codigo; el.btnContinuarP2.disabled = false; }
      });
      const label = document.createElement('label');
      label.htmlFor = inputId;
      label.innerHTML = `
        <span class="opcion-codigo" aria-hidden="true">${escapeHtml(String(opcion.codigo))}</span>
        <span class="opcion-descripcion">${escapeHtml(opcion.descripcion)}</span>
        ${!opcion.habilitada ? '<span class="opcion-proximamente">Próximamente</span>' : ''}
      `;
      wrapper.appendChild(input);
      wrapper.appendChild(label);
      el.p2OpcionesLista.appendChild(wrapper);
    });

    el.p2Opciones.hidden = opciones.length === 0;
  }

  async function continuarDesdeP2() {
    if (!state.bolsinDetalle || state.opcionElegida === null) return;
    el.btnContinuarP2.disabled = true;
    el.btnContinuarP2.setAttribute('aria-busy', 'true');

    try {
      const res = await apiFetch('/api/recepcion-bolsin/seleccionar-recepcion', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ numeroBolsin: Number(state.bolsinDetalle.numeroBolsin), opcion: Number(state.opcionElegida) })
      });

      el.btnContinuarP2.removeAttribute('aria-busy');
      el.btnContinuarP2.disabled = false;

      if (!res.ok) {
        let msg = 'Ocurrió un problema. Intentá de nuevo en unos segundos.';
        try { const e = await res.json(); if (e?.mensaje) msg = e.mensaje; } catch (_) {}
        mostrarErrorP2(msg);
        return;
      }

      const data = await res.json();
      state.seleccionRecepcionRespuesta = data;
      goToStep(3);
      abrirDialogoConfirmacion(data);
    } catch (err) {
      el.btnContinuarP2.removeAttribute('aria-busy');
      el.btnContinuarP2.disabled = false;
      mostrarErrorP2('Ocurrió un problema. Intentá de nuevo en unos segundos.');
    }
  }

  // =========================================================================
  // ASISTENTE – Paso 3: Confirmación
  // =========================================================================

  function abrirDialogoConfirmacion(data) {
    if (!el.dialogConfirmacion || !state.bolsinDetalle) return;
    const remitos = state.bolsinDetalle.remitos || [];
    const totalDocs = remitos.reduce((acc, r) => acc + (r.documentaciones || []).length, 0);

    if (el.dialogNumeroBolsin) el.dialogNumeroBolsin.textContent = String(state.bolsinDetalle.numeroBolsin);
    if (el.dialogResumenDocs) el.dialogResumenDocs.textContent = formatearDocumentaciones(totalDocs);
    if (el.dialogResumenRemitos) el.dialogResumenRemitos.textContent = formatearRemitos(remitos.length);
    if (el.dialogResumenServidor) {
      if (data?.resumen) { el.dialogResumenServidor.textContent = data.resumen; el.dialogResumenServidor.hidden = false; }
      else el.dialogResumenServidor.hidden = true;
    }
    if (el.dialogError) el.dialogError.hidden = true;
    if (el.btnConfirmarRecepcion) {
      el.btnConfirmarRecepcion.disabled = false;
      el.btnConfirmarRecepcion.textContent = 'Confirmar recepción';
      el.btnConfirmarRecepcion.removeAttribute('aria-busy');
    }
    if (el.btnNoConfirmar) {
      el.btnNoConfirmar.disabled = false;
      el.btnNoConfirmar.textContent = 'No confirmar';
      el.btnNoConfirmar.removeAttribute('aria-busy');
    }
    state.isConfirmingOrCancelling = false;
    el.dialogConfirmacion.showModal();
    if (el.btnNoConfirmar) el.btnNoConfirmar.focus();
  }

  async function confirmarRecepcion() {
    if (state.isConfirmingOrCancelling || !state.bolsinDetalle) return;
    state.isConfirmingOrCancelling = true;
    if (el.btnConfirmarRecepcion) {
      el.btnConfirmarRecepcion.disabled = true;
      el.btnConfirmarRecepcion.setAttribute('aria-busy', 'true');
      el.btnConfirmarRecepcion.textContent = 'Registrando…';
    }
    if (el.btnNoConfirmar) el.btnNoConfirmar.disabled = true;
    if (el.dialogError) el.dialogError.hidden = true;

    try {
      const res = await apiFetch('/api/recepcion-bolsin/confirmar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ numeroBolsin: Number(state.bolsinDetalle.numeroBolsin), opcion: Number(state.opcionElegida), confirmado: true })
      });

      if (!res.ok) {
        let msg = 'No se pudo registrar la recepción. No se modificó ningún dato.';
        try { const e = await res.json(); if (e?.mensaje) msg = e.mensaje; } catch (_) {}
        el.dialogConfirmacion.close();
        state.isConfirmingOrCancelling = false;
        renderPaso4Error(msg);
        return;
      }

      const data = await res.json();
      state.resultadoConfirmacion = data;
      // Forzar recarga del contador al volver al inicio
      state.listaBolsinesOriginal = [];
      el.dialogConfirmacion.close();
      state.isConfirmingOrCancelling = false;
      renderPaso4(data);
      goToStep(4);
    } catch (err) {
      el.dialogConfirmacion.close();
      state.isConfirmingOrCancelling = false;
      renderPaso4Error('Ocurrió un problema de conexión al registrar la recepción. No se modificó ningún dato.');
    }
  }

  async function noConfirmarRecepcion() {
    if (state.isConfirmingOrCancelling || !state.bolsinDetalle) return;
    state.isConfirmingOrCancelling = true;
    if (el.btnConfirmarRecepcion) el.btnConfirmarRecepcion.disabled = true;
    if (el.btnNoConfirmar) el.btnNoConfirmar.disabled = true;

    try {
      const res = await apiFetch('/api/recepcion-bolsin/confirmar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ numeroBolsin: Number(state.bolsinDetalle.numeroBolsin), opcion: Number(state.opcionElegida), confirmado: false })
      });
      let mensaje = 'No se registró la recepción. No se modificó ningún dato.';
      if (res.ok) { try { const d = await res.json(); if (d?.mensaje) mensaje = d.mensaje; } catch (_) {} }
      el.dialogConfirmacion.close();
      volverPaso1ConA6(mensaje);
    } catch (err) {
      el.dialogConfirmacion.close();
      volverPaso1ConA6('No se registró la recepción. No se modificó ningún dato.');
    } finally {
      state.isConfirmingOrCancelling = false;
    }
  }

  // =========================================================================
  // ASISTENTE – Paso 4: Resultado
  // =========================================================================

  function renderPaso4(resultado) {
    if (el.p4VistaExito) el.p4VistaExito.hidden = false;
    if (el.p4VistaError) el.p4VistaError.hidden = true;
    if (el.p4Subtitulo && resultado.mensaje) el.p4Subtitulo.textContent = resultado.mensaje;

    const numBolsin = resultado.numeroBolsin ?? state.bolsinDetalle?.numeroBolsin ?? '-';
    const nuevoEstado = resultado.nuevoEstadoBolsin ?? '';
    const remitos = resultado.remitosActualizados ?? 0;
    const docs = resultado.documentacionesActualizadas ?? 0;

    if (el.p4ResumenLista) {
      el.p4ResumenLista.innerHTML = `
        <li>
          <span class="resultado-item-label">Bolsín N°</span>
          <strong class="tabular-nums">${escapeHtml(String(numBolsin))}</strong>
        </li>
        <li>
          <span class="resultado-item-label">Nuevo estado</span>
          <strong class="estado-destacado">${escapeHtml(nuevoEstado)}</strong>
        </li>
        <li>
          <span class="resultado-item-label">Remitos actualizados</span>
          <strong class="tabular-nums">${formatearRemitos(remitos)}</strong>
        </li>
        <li>
          <span class="resultado-item-label">Documentaciones actualizadas</span>
          <strong class="tabular-nums">${formatearDocumentaciones(docs)}</strong>
        </li>
      `;
    }

    if (el.p4NotificacionesLista) {
      const notifs = resultado.notificaciones || [];
      if (notifs.length === 0) {
        el.p4NotificacionesLista.innerHTML = `<p class="notificaciones-vacias text-muted">No se enviaron notificaciones.</p>`;
      } else {
        el.p4NotificacionesLista.innerHTML = notifs.map(n => `
          <div class="notificacion-item">
            <div class="notificacion-destinatario">
              <span class="notificacion-icono" aria-hidden="true">✉</span>
              <strong>${escapeHtml(n.cmOrigen ?? 'Comisión Médica')}</strong>
              <span class="notificacion-email">&lt;${escapeHtml(n.email ?? '')}&gt;</span>
            </div>
            <div class="notificacion-mensaje">
              <span class="notificacion-mensaje-label">Mensaje:</span> ${escapeHtml(n.asunto ?? '')}
            </div>
          </div>
        `).join('');
      }
    }
  }

  function renderPaso4Error(mensaje) {
    if (el.p4VistaExito) el.p4VistaExito.hidden = true;
    if (el.p4VistaError) el.p4VistaError.hidden = false;
    if (el.p4ErrorMensajeTexto) el.p4ErrorMensajeTexto.textContent = mensaje || 'No se pudo registrar la recepción. No se modificó ningún dato.';
    goToStep(4);
  }

  // =========================================================================
  // Inicio del asistente: ir a pantalla recepción desde homepage
  // =========================================================================

  function comenzarCU28() {
    // Limpia el estado del wizard y fuerza recarga de bolsines
    reiniciarEstadoAsistente();
    navigate('recepcion');
    // Después de navegar, inicializa el wizard desde paso 1
    requestAnimationFrame(() => {
      actualizarHeaderRecepcion();
      goToStep(1);
      cargarBolsinesIniciales();
    });
  }

  // =========================================================================
  // Registro de Event Listeners
  // =========================================================================

  function inicializarEventos() {
    // --- Login ---
    // (las tarjetas se crean dinámicamente con addEventListener en renderLoginGrid)

    // --- Homepage ---
    if (homeEl.btnComenzarCU28) homeEl.btnComenzarCU28.addEventListener('click', comenzarCU28);
    if (homeEl.btnCerrarSesion) homeEl.btnCerrarSesion.addEventListener('click', cerrarSesion);

    // --- Header de Recepción ---
    if (el.btnCerrarSesionRec) el.btnCerrarSesionRec.addEventListener('click', () => {
      cerrarSesion();
    });
    if (el.btnVolverInicio) el.btnVolverInicio.addEventListener('click', () => {
      navigate('inicio');
    });

    // --- Paso 1 ---
    if (el.formFiltros) el.formFiltros.addEventListener('submit', buscarBolsines);
    if (el.btnLimpiar) el.btnLimpiar.addEventListener('click', limpiarFiltros);
    if (el.btnReintentar) {
      el.btnReintentar.addEventListener('click', () => {
        if (typeof state.ultimoReintento === 'function') state.ultimoReintento();
        else cargarBolsinesIniciales();
      });
    }
    if (el.btnCancelarPaso1) el.btnCancelarPaso1.addEventListener('click', cancelarProceso);

    // --- Paso 2 ---
    if (el.btnVolverP2) el.btnVolverP2.addEventListener('click', () => goToStep(1));
    if (el.btnCancelarP2) el.btnCancelarP2.addEventListener('click', cancelarProceso);
    if (el.btnContinuarP2) el.btnContinuarP2.addEventListener('click', continuarDesdeP2);

    // --- Paso 3: Diálogo ---
    if (el.dialogConfirmacion) {
      el.dialogConfirmacion.addEventListener('cancel', (e) => { e.preventDefault(); noConfirmarRecepcion(); });
    }
    if (el.btnNoConfirmar) el.btnNoConfirmar.addEventListener('click', (e) => { e.preventDefault(); noConfirmarRecepcion(); });
    if (el.btnConfirmarRecepcion) el.btnConfirmarRecepcion.addEventListener('click', (e) => { e.preventDefault(); confirmarRecepcion(); });

    // --- Paso 4 ---
    if (el.btnReiniciarDesdeP4) {
      el.btnReiniciarDesdeP4.addEventListener('click', () => {
        // Reinicia wizard y vuelve al inicio para mostrar el nuevo contador
        reiniciarEstadoAsistente();
        navigate('inicio');
      });
    }
    if (el.btnVolverInicioError) {
      el.btnVolverInicioError.addEventListener('click', () => {
        reiniciarEstadoAsistente();
        navigate('inicio');
      });
    }
  }

  // =========================================================================
  // Arranque
  // =========================================================================

  async function inicializar() {
    inicializarEventos();

    // Intentar recuperar la sesión activa al cargar
    try {
      const res = await fetch('/api/sesion?empleadoId=1', { headers: { Accept: 'application/json' } });
      if (res.ok) {
        const data = await res.json();
        const id = data.id ?? data.empleadoId;
        if (id) {
          state.currentEmpleadoId = id;
          state.currentNombreCompleto = data.nombreCompleto || `Empleado ${id}`;
          state.currentPrimerNombre = extraerPrimerNombre(state.currentNombreCompleto);
          state.currentComisionMedica = extraerNombreCm(data) || data.cm || '';
        }
      }
    } catch (_) {}

    // Cargar lista de empleados (necesaria para renderizar el login)
    try {
      const res = await fetch('/api/sesion/empleados', { headers: { Accept: 'application/json' } });
      if (res.ok) {
        const data = await res.json();
        state.empleados = Array.isArray(data) ? data : (data?.empleados || []);
      }
    } catch (_) {}

    // Hash routing: determinar pantalla inicial
    const hash = getHash();

    if (hash === 'login' || !state.currentEmpleadoId) {
      await cargarLogin();
      navigate('login', false);
    } else {
      navigate(hash, false);
    }
  }

  document.addEventListener('DOMContentLoaded', inicializar);

})();
