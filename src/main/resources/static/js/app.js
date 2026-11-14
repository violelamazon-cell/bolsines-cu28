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
    bolsinDetalle: null,       // respuesta de /seleccionar-bolsin
    opcionElegida: null,       // número de opción seleccionada en Paso 2
    ultimoReintento: null,
    isConfirmingOrCancelling: false,
    seleccionRecepcionRespuesta: null,
    resultadoConfirmacion: null
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

    // Paso 2 – Revisión
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

    // Paso 3 – Confirmación (Diálogo modal)
    dialogConfirmacion: document.getElementById('dialog-confirmacion'),
    dialogNumeroBolsin: document.getElementById('dialog-numero-bolsin'),
    dialogResumenDocs: document.getElementById('dialog-resumen-docs'),
    dialogResumenRemitos: document.getElementById('dialog-resumen-remitos'),
    dialogResumenServidor: document.getElementById('dialog-resumen-servidor'),
    dialogError: document.getElementById('dialog-error'),
    dialogErrorMensaje: document.getElementById('dialog-error-mensaje'),
    btnNoConfirmar: document.getElementById('btn-no-confirmar'),
    btnConfirmarRecepcion: document.getElementById('btn-confirmar-recepcion'),

    // Paso 4 – Resultado (Placeholder)
    p4ResultadoDetalle: document.getElementById('p4-resultado-detalle'),
    btnReiniciarDesdeP4: document.getElementById('btn-reiniciar-desde-p4')
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

  function formatearRemitos(cantidad) {
    return `${cantidad} ${cantidad === 1 ? 'remito' : 'remitos'}`;
  }

  function formatearDocumentaciones(cantidad) {
    return `${cantidad} ${cantidad === 1 ? 'documentación' : 'documentaciones'}`;
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

    // Avanzar al Paso 2: seleccionar el bolsín en el backend
    const numBolsin = bolsin.numeroBolsin ?? bolsin.numero ?? bolsin.id;
    seleccionarBolsin(numBolsin);
  }

  // --- Carga inicial y filtros ---
  async function cargarBolsinesIniciales(mantenerA6 = false) {
    setCargando(true);
    ocultarBanners(mantenerA6);
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

    // Si ambos están vacíos, equivale a listar/restaurar todo
    if (!precinto && !cmOrigen) {
      limpiarFiltros();
      return;
    }

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
    state.bolsinDetalle = null;
    state.opcionElegida = null;
    state.seleccionRecepcionRespuesta = null;
    state.resultadoConfirmacion = null;
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

    if (state.listaBolsinesOriginal && state.listaBolsinesOriginal.length > 0) {
      renderTablaBolsines(state.listaBolsinesOriginal);
      el.contenedorTabla.hidden = false;
      el.estadoVacio.hidden = true;
    } else {
      cargarBolsinesIniciales(true);
    }
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

  // =========================================================================
  // PASO 2 – Revisión
  // =========================================================================

  async function seleccionarBolsin(numeroBolsin) {
    // Ir al paso 2 y mostrar estado cargando
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
        try {
          const errData = await res.json();
          if (errData && errData.mensaje) msg = errData.mensaje;
        } catch (_) {}
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
    // --- Tarjeta resumen ---
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

    // --- Remitos con <details> ---
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

    // --- Opciones de recepción ---
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
        if (input.checked) {
          state.opcionElegida = opcion.codigo;
          el.btnContinuarP2.disabled = false;
        }
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
        body: JSON.stringify({
          numeroBolsin: Number(state.bolsinDetalle.numeroBolsin),
          opcion: Number(state.opcionElegida)
        })
      });

      el.btnContinuarP2.removeAttribute('aria-busy');
      el.btnContinuarP2.disabled = false;

      if (!res.ok) {
        let msg = 'Ocurrió un problema. Intentá de nuevo en unos segundos.';
        try {
          const errData = await res.json();
          if (errData && errData.mensaje) msg = errData.mensaje;
        } catch (_) {}
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
  // PASO 3 – Confirmación
  // =========================================================================

  function abrirDialogoConfirmacion(data) {
    if (!el.dialogConfirmacion || !state.bolsinDetalle) return;

    const remitos = state.bolsinDetalle.remitos || [];
    const totalDocs = remitos.reduce((acc, r) => acc + (r.documentaciones || []).length, 0);

    if (el.dialogNumeroBolsin) {
      el.dialogNumeroBolsin.textContent = String(state.bolsinDetalle.numeroBolsin);
    }
    if (el.dialogResumenDocs) {
      el.dialogResumenDocs.textContent = formatearDocumentaciones(totalDocs);
    }
    if (el.dialogResumenRemitos) {
      el.dialogResumenRemitos.textContent = formatearRemitos(remitos.length);
    }

    if (el.dialogResumenServidor) {
      if (data && data.resumen) {
        el.dialogResumenServidor.textContent = data.resumen;
        el.dialogResumenServidor.hidden = false;
      } else {
        el.dialogResumenServidor.hidden = true;
      }
    }

    if (el.dialogError) {
      el.dialogError.hidden = true;
    }

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

    // Abrir diálogo como modal
    el.dialogConfirmacion.showModal();

    // Foco inicial en "No confirmar" (acción segura por defecto)
    if (el.btnNoConfirmar) {
      el.btnNoConfirmar.focus();
    }
  }

  async function confirmarRecepcion() {
    if (state.isConfirmingOrCancelling || !state.bolsinDetalle) return;
    state.isConfirmingOrCancelling = true;

    if (el.btnConfirmarRecepcion) {
      el.btnConfirmarRecepcion.disabled = true;
      el.btnConfirmarRecepcion.setAttribute('aria-busy', 'true');
      el.btnConfirmarRecepcion.textContent = 'Registrando…';
    }
    if (el.btnNoConfirmar) {
      el.btnNoConfirmar.disabled = true;
    }
    if (el.dialogError) {
      el.dialogError.hidden = true;
    }

    try {
      const res = await apiFetch('/api/recepcion-bolsin/confirmar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          numeroBolsin: Number(state.bolsinDetalle.numeroBolsin),
          opcion: Number(state.opcionElegida),
          confirmado: true
        })
      });

      if (!res.ok) {
        let msg = 'No se pudo registrar la recepción. No se modificó ningún dato.';
        try {
          const errData = await res.json();
          if (errData && errData.mensaje) msg = errData.mensaje;
        } catch (_) {}

        if (el.btnConfirmarRecepcion) {
          el.btnConfirmarRecepcion.disabled = false;
          el.btnConfirmarRecepcion.removeAttribute('aria-busy');
          el.btnConfirmarRecepcion.textContent = 'Confirmar recepción';
        }
        if (el.btnNoConfirmar) {
          el.btnNoConfirmar.disabled = false;
        }
        if (el.dialogError && el.dialogErrorMensaje) {
          el.dialogErrorMensaje.textContent = msg;
          el.dialogError.hidden = false;
        }
        state.isConfirmingOrCancelling = false;
        return;
      }

      const data = await res.json();
      state.resultadoConfirmacion = data;

      el.dialogConfirmacion.close();
      state.isConfirmingOrCancelling = false;

      // Avanzar al Paso 4 (placeholder)
      renderPaso4(data);
      goToStep(4);

    } catch (err) {
      if (el.btnConfirmarRecepcion) {
        el.btnConfirmarRecepcion.disabled = false;
        el.btnConfirmarRecepcion.removeAttribute('aria-busy');
        el.btnConfirmarRecepcion.textContent = 'Confirmar recepción';
      }
      if (el.btnNoConfirmar) {
        el.btnNoConfirmar.disabled = false;
      }
      if (el.dialogError && el.dialogErrorMensaje) {
        el.dialogErrorMensaje.textContent = 'Ocurrió un problema de conexión al registrar la recepción.';
        el.dialogError.hidden = false;
      }
      state.isConfirmingOrCancelling = false;
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
        body: JSON.stringify({
          numeroBolsin: Number(state.bolsinDetalle.numeroBolsin),
          opcion: Number(state.opcionElegida),
          confirmado: false
        })
      });

      let mensaje = 'No se registró la recepción. No se modificó ningún dato.';
      if (res.ok) {
        try {
          const data = await res.json();
          if (data && data.mensaje) mensaje = data.mensaje;
        } catch (_) {}
      }

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
  // PASO 4 – Resultado (Placeholder)
  // =========================================================================

  function renderPaso4(resultado) {
    if (!el.p4ResultadoDetalle) return;
    el.p4ResultadoDetalle.innerHTML = `
      <p>${escapeHtml(resultado.mensaje || 'La recepción del bolsín ha sido registrada.')}</p>
      <ul>
        <li><strong>Bolsín N°:</strong> ${escapeHtml(String(resultado.numeroBolsin ?? state.bolsinDetalle?.numeroBolsin ?? '-'))}</li>
        <li><strong>Nuevo estado:</strong> ${escapeHtml(resultado.nuevoEstadoBolsin ?? 'Recibido en CM destino')}</li>
        <li><strong>Remitos actualizados:</strong> ${escapeHtml(String(resultado.remitosActualizados ?? 0))}</li>
        <li><strong>Documentaciones actualizadas:</strong> ${escapeHtml(String(resultado.documentacionesActualizadas ?? 0))}</li>
      </ul>
    `;
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

    // Paso 2
    if (el.btnVolverP2) {
      el.btnVolverP2.addEventListener('click', () => {
        goToStep(1);
      });
    }

    if (el.btnCancelarP2) {
      el.btnCancelarP2.addEventListener('click', cancelarProceso);
    }

    if (el.btnContinuarP2) {
      el.btnContinuarP2.addEventListener('click', continuarDesdeP2);
    }

    // Paso 3 – Confirmación
    if (el.dialogConfirmacion) {
      // Esc = no confirmar
      el.dialogConfirmacion.addEventListener('cancel', (e) => {
        e.preventDefault();
        noConfirmarRecepcion();
      });
    }

    if (el.btnNoConfirmar) {
      el.btnNoConfirmar.addEventListener('click', (e) => {
        e.preventDefault();
        noConfirmarRecepcion();
      });
    }

    if (el.btnConfirmarRecepcion) {
      el.btnConfirmarRecepcion.addEventListener('click', (e) => {
        e.preventDefault();
        confirmarRecepcion();
      });
    }

    // Paso 4 – Resultado (Placeholder)
    if (el.btnReiniciarDesdeP4) {
      el.btnReiniciarDesdeP4.addEventListener('click', () => {
        reiniciarWizard();
      });
    }
  }

  // --- Inicio de la aplicación ---
  document.addEventListener('DOMContentLoaded', () => {
    inicializarEventos();
    goToStep(1);
    cargarSesionYEmpleados();
  });
})();

