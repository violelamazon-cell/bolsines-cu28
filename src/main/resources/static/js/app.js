/**
 * CU 28 - Registrar recepción de bolsín
 * Frontend - Fase 4, Parte 1
 */

(function () {
  'use strict';

  // --- Estado global de la aplicación ---
  const state = {
    currentStep: 1,
    currentEmpleadoId: 1,
    currentComisionMedica: '',
    empleados: [],
    listaBolsinesOriginal: [],
    selectedBolsin: null,
    ultimoReintento: null
  };

  // Exponer para depuración/inspección
  window.__appState = state;

  // --- Elementos del DOM ---
  const el = {
    // Header
    cmBadge: document.getElementById('cm-badge'),
    selectUsuario: document.getElementById('select-usuario'),
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
    paso4: document.getElementById('paso-4')
  };

  // --- Helper de Fetch con cabecera obligatoria X-Empleado-Id ---
  async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }
    if (state.currentEmpleadoId) {
      headers.set('X-Empleado-Id', String(state.currentEmpleadoId));
    }
    return fetch(url, { ...options, headers });
  }

  // --- Helpers de utilidad y extracción de datos ---
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
    if (cm && typeof cm === 'object') {
      return cm.nombreCm ?? cm.nombre ?? cm.descripcion ?? '';
    }
    return '';
  }

  function extraerEmpleadoId(emp) {
    return emp.id ?? emp.empleadoId;
  }

  function extraerEmpleadoNombre(emp) {
    if (emp.nombreCompleto) return emp.nombreCompleto;
    if (emp.apellido && emp.nombre) return `${emp.apellido}, ${emp.nombre}`;
    if (emp.nombre) return emp.nombre;
    return `Empleado ${extraerEmpleadoId(emp)}`;
  }

  function actualizarCmEnUi(nombreCm) {
    if (!nombreCm) return;
    state.currentComisionMedica = nombreCm;
    if (el.cmBadge) el.cmBadge.textContent = nombreCm;
    if (el.cmUsuarioSubtitulo) el.cmUsuarioSubtitulo.textContent = nombreCm;
  }

  // --- Navegación del Stepper y enfoque accesible ---
  const ETAPAS = [
    { num: 1, name: 'Bolsín' },
    { num: 2, name: 'Revisión' },
    { num: 3, name: 'Confirmación' },
    { num: 4, name: 'Resultado' }
  ];

  function goToStep(stepNumber) {
    state.currentStep = stepNumber;

    // Actualizar indicador de pasos
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

    // Gestionar visibilidad de paneles
    [1, 2, 3, 4].forEach(n => {
      const panel = el[`paso${n}`];
      if (panel) {
        if (n === stepNumber) {
          panel.removeAttribute('hidden');
        } else {
          panel.setAttribute('hidden', '');
        }
      }
    });

    // Mover el foco al título único de la página
    if (el.pasoTitulo) {
      el.pasoTitulo.focus();
    }
  }

  // --- Manejo de estados visuales de Paso 1 ---
  function setCargando(cargando) {
    if (el.estadoCargando) {
      el.estadoCargando.hidden = !cargando;
    }
    if (el.btnBuscar) el.btnBuscar.disabled = cargando;
    if (el.btnLimpiar) el.btnLimpiar.disabled = cargando;
  }

  function ocultarBanners() {
    if (el.bannerFiltroError) el.bannerFiltroError.hidden = true;
    if (el.bannerErrorConexion) el.bannerErrorConexion.hidden = true;
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

      // Selección por clic en fila
      tr.addEventListener('click', () => {
        seleccionarFila(bolsin, tr);
      });

      // Selección por teclado (Enter o Espacio)
      tr.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          seleccionarFila(bolsin, tr);
        }
      });

      // Selección explícita por botón
      const btn = tr.querySelector('.btn-seleccionar');
      if (btn) {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          seleccionarFila(bolsin, tr);
        });
      }

      el.tablaBolsinesBody.appendChild(tr);
    });
  }

  function seleccionarFila(bolsin, trElement) {
    state.selectedBolsin = bolsin;
    const allRows = el.tablaBolsinesBody.querySelectorAll('tr');
    allRows.forEach(row => {
      row.classList.remove('selected');
      row.setAttribute('aria-selected', 'false');
    });
    trElement.classList.add('selected');
    trElement.setAttribute('aria-selected', 'true');
  }

  // --- Carga inicial y filtros ---
  async function cargarBolsinesIniciales() {
    setCargando(true);
    ocultarBanners();
    el.estadoVacio.hidden = true;
    el.contenedorTabla.hidden = true;

    try {
      const res = await apiFetch('/api/recepcion-bolsin/iniciar', {
        method: 'POST'
      });

      setCargando(false);

      if (res.ok) {
        let data = null;
        try {
          data = await res.json();
        } catch (_) {}
        const list = Array.isArray(data) ? data : (data?.bolsines || data?.bolsinesEnviados || []);
        state.listaBolsinesOriginal = list;
        renderTablaBolsines(list);
      } else if (res.status === 404) {
        // Estado vacío: sin bolsines enviados para la CM
        state.listaBolsinesOriginal = [];
        el.contenedorTabla.hidden = true;
        el.estadoVacio.hidden = false;
      } else {
        mostrarErrorConexion(() => cargarBolsinesIniciales());
      }
    } catch (err) {
      mostrarErrorConexion(() => cargarBolsinesIniciales());
    }
  }

  async function buscarBolsines(e) {
    if (e) e.preventDefault();

    const precinto = el.filtroPrecinto.value.trim();
    const cmOrigen = el.filtroCmOrigen.value.trim();

    // Si ambos están vacíos, equivale a listar/restaurar todo
    if (!precinto && !cmOrigen) {
      limpiarFiltros();
      return;
    }

    setCargando(true);
    if (el.bannerFiltroError) el.bannerFiltroError.hidden = true;
    if (el.bannerErrorConexion) el.bannerErrorConexion.hidden = true;

    const params = [];
    if (precinto) params.push(`numeroPrecinto=${encodeURIComponent(precinto)}`);
    if (cmOrigen) params.push(`cmOrigen=${encodeURIComponent(cmOrigen)}`);
    const url = `/api/recepcion-bolsin/bolsines?${params.join('&')}`;

    try {
      const res = await apiFetch(url);
      setCargando(false);

      if (res.ok) {
        let data = null;
        try {
          data = await res.json();
        } catch (_) {}
        const bolsines = Array.isArray(data) ? data : (data?.bolsines || data?.bolsinesEnviados || []);
        renderTablaBolsines(bolsines);
        if (el.bannerFiltroError) el.bannerFiltroError.hidden = true;
      } else if (res.status === 404) {
        // A1/A2: Se muestra banner con el mensaje del servidor y SE MANTIENE la tabla anterior
        let mensaje = 'No se encontró ningún bolsín con el criterio ingresado.';
        try {
          const errData = await res.json();
          if (errData && errData.mensaje) {
            mensaje = errData.mensaje;
          }
        } catch (_) {}
        mostrarBannerFiltro(mensaje);
        // La tabla anterior se mantiene visible; si estaba vacía, el estado vacío se mantiene
      } else {
        let mensaje = 'Ocurrió un problema. Intentá de nuevo en unos segundos.';
        try {
          const errData = await res.json();
          if (errData && errData.mensaje) mensaje = errData.mensaje;
        } catch (_) {}
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

  function reiniciarWizard() {
    goToStep(1);
    if (el.filtroPrecinto) el.filtroPrecinto.value = '';
    if (el.filtroCmOrigen) el.filtroCmOrigen.value = '';
    ocultarBanners();
    state.selectedBolsin = null;
    cargarBolsinesIniciales();
  }

  async function cancelarProceso() {
    try {
      await apiFetch('/api/recepcion-bolsin/cancelar', { method: 'POST' });
    } catch (_) {}
    reiniciarWizard();
  }

  // --- Gestión de sesión y empleados ---
  async function cargarSesionYEmpleados() {
    let empleados = [];

    // 1. Cargar lista de empleados
    try {
      const res = await apiFetch('/api/sesion/empleados');
      if (res.ok) {
        const data = await res.json();
        empleados = Array.isArray(data) ? data : (data?.empleados || []);
        poblarSelectorEmpleados(empleados);
      }
    } catch (err) {
      console.warn('No se pudo cargar la lista de empleados:', err);
    }

    // 2. Cargar sesión activa
    try {
      let resSesion = await apiFetch(`/api/sesion?empleadoId=${state.currentEmpleadoId}`);
      if (!resSesion.ok) {
        resSesion = await apiFetch('/api/sesion');
      }
      if (resSesion.ok) {
        const sesionData = await resSesion.json();
        const empId = extraerEmpleadoId(sesionData);
        if (empId) {
          state.currentEmpleadoId = empId;
          if (el.selectUsuario) el.selectUsuario.value = String(empId);
        }
        actualizarCmEnUi(extraerNombreCm(sesionData));
      } else if (empleados.length > 0) {
        // Fallback al primer empleado de la lista
        const primerEmp = empleados[0];
        state.currentEmpleadoId = extraerEmpleadoId(primerEmp);
        if (el.selectUsuario) el.selectUsuario.value = String(state.currentEmpleadoId);
        actualizarCmEnUi(extraerNombreCm(primerEmp));
      }
    } catch (err) {
      console.warn('Error al obtener sesión:', err);
    }

    // 3. Cargar bolsines iniciales
    cargarBolsinesIniciales();
  }

  function poblarSelectorEmpleados(empleados) {
    if (!el.selectUsuario) return;
    el.selectUsuario.innerHTML = '';
    state.empleados = empleados || [];

    state.empleados.forEach(emp => {
      const id = extraerEmpleadoId(emp);
      const nombre = extraerEmpleadoNombre(emp);
      const cm = extraerNombreCm(emp);
      const opt = document.createElement('option');
      opt.value = String(id);
      opt.dataset.cm = cm;
      opt.textContent = cm ? `${nombre} (${cm})` : nombre;
      el.selectUsuario.appendChild(opt);
    });

    if (state.currentEmpleadoId) {
      el.selectUsuario.value = String(state.currentEmpleadoId);
    }
  }

  async function onCambiarUsuario(nuevoEmpleadoId) {
    state.currentEmpleadoId = Number(nuevoEmpleadoId);

    // 1. Obtener CM del empleado elegido desde /api/sesion/empleados
    let nuevaCm = '';
    const empElegido = state.empleados.find(e => String(extraerEmpleadoId(e)) === String(state.currentEmpleadoId));
    if (empElegido) {
      nuevaCm = extraerNombreCm(empElegido);
    }
    if (!nuevaCm && el.selectUsuario) {
      const opt = el.selectUsuario.options[el.selectUsuario.selectedIndex];
      if (opt && opt.dataset.cm) {
        nuevaCm = opt.dataset.cm;
      }
    }

    // 2. Notificar al backend sobre el inicio de sesión y leer CM de la respuesta de /api/sesion/iniciar
    try {
      const res = await apiFetch('/api/sesion/iniciar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ empleadoId: state.currentEmpleadoId })
      });
      if (res.ok) {
        try {
          const data = await res.json();
          const cmFromIniciar = extraerNombreCm(data);
          if (cmFromIniciar) {
            nuevaCm = cmFromIniciar;
          }
        } catch (_) {}
      }
    } catch (_) {}

    // 3. Actualizar CM en chip del header y en el subtítulo del Paso 1
    if (nuevaCm) {
      actualizarCmEnUi(nuevaCm);
    }

    // 4. Mostrar aviso "Cambiaste de usuario. Se reinició el proceso."
    if (el.bannerAvisoUsuario) {
      el.bannerAvisoUsuario.hidden = false;
    }

    // 5. Reiniciar wizard
    reiniciarWizard();
  }

  // --- Registro de Event Listeners ---
  function inicializarEventos() {
    if (el.selectUsuario) {
      el.selectUsuario.addEventListener('change', (e) => {
        onCambiarUsuario(e.target.value);
      });
    }

    if (el.formFiltros) {
      el.formFiltros.addEventListener('submit', buscarBolsines);
    }

    if (el.btnLimpiar) {
      el.btnLimpiar.addEventListener('click', limpiarFiltros);
    }

    if (el.btnReintentar) {
      el.btnReintentar.addEventListener('click', () => {
        if (typeof state.ultimoReintento === 'function') {
          state.ultimoReintento();
        } else {
          cargarBolsinesIniciales();
        }
      });
    }

    if (el.btnCancelarPaso1) {
      el.btnCancelarPaso1.addEventListener('click', cancelarProceso);
    }
  }

  // --- Inicio de la aplicación ---
  document.addEventListener('DOMContentLoaded', () => {
    inicializarEventos();
    goToStep(1);
    cargarSesionYEmpleados();
  });
})();

