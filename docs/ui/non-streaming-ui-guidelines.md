# Non-Streaming UI: arquitectura, estados y guía de migración

## 1) Capa UI separada para pantallas no críticas de streaming

Se define una capa dedicada para pantallas no críticas bajo `com.limelight.ui.nonstream` y `com.limelight.ui.components`.

### Alcance inicial
- Home
- Lista de PCs
- Ajustes
- Help

### Objetivo
Aislar las decisiones de presentación de estas pantallas del flujo crítico de streaming para:
- reducir riesgo operativo,
- facilitar iteraciones visuales,
- mejorar consistencia y testabilidad.

### Estructura propuesta
- `com.limelight.ui.nonstream`
  - `UiState<T>`: contrato de estado de pantalla.
  - `UiStateRenderer<T>`: renderizador reusable de estados.
- `com.limelight.ui.components`
  - `TopBarConfig`
  - `UiCellModel`
  - `ActionButtonModel`
  - `EmptyStateModel`

---

## 2) Patrones reutilizables de estado UI

Todas las pantallas no críticas deben expresar su estado con `UiState<T>`:

- `Loading`: cuando se está preparando o consultando data.
- `Empty`: cuando no hay contenido útil que mostrar.
- `Error`: cuando falla una operación recuperable (incluye `retryAction`).
- `Content`: cuando hay datos válidos.

### Reglas de uso
- No mezclar flags ad-hoc (`isLoading`, `hasError`, etc.) por pantalla cuando se pueda modelar con `UiState`.
- Mantener **una sola fuente de verdad** de estado por vista.
- En `Error`, mostrar acción de reintento cuando sea técnicamente posible.

---

## 3) Componentes comunes encapsulados

Se estandarizan modelos reutilizables para alimentar vistas compartidas:

- `TopBarConfig`: título y acción de navegación.
- `UiCellModel`: item reusable para listas/grids.
- `ActionButtonModel`: CTA primario/secundario.
- `EmptyStateModel`: plantilla uniforme para vacíos.

### Convención
- Priorizar composición de componentes antes de crear layouts específicos por pantalla.
- Evitar duplicar copy o estilos entre Home, PCs, Ajustes y Help.

---

## 4) Plan de migración incremental (pantalla por pantalla)

### Fase 0 (preparación)
- Introducir contratos base (`UiState`, `UiStateRenderer`, modelos de componentes).
- Definir checklist de paridad funcional con baseline actual.

### Fase 1: Help
- Bajo riesgo técnico y pocas dependencias.
- Migrar estructura visual y manejo de estados.
- Verificar navegación y enlaces.

### Fase 2: Ajustes
- Migrar secciones de UI gradualmente conservando lógica de preferencias existente.
- Validar que cambios de configuración persistan igual.

### Fase 3: Lista de PCs (PcView)
- Migrar top bar, empty state y celdas con wrappers reutilizables.
- Mantener discovery/polling actual sin cambios funcionales.
- Añadir fallback para rollback rápido si hay regresión.

### Fase 4: Home
- Integrar componentes y estado unificado.
- Cerrar deudas de consistencia visual entre secciones.

### Criterios de salida por fase
- Paridad funcional confirmada.
- Sin impacto en sesiones de streaming.
- Métricas de crash estables.

---

## 5) Guía de estilo y motion

### Estilo visual
- Jerarquía tipográfica consistente (título, cuerpo, metadata).
- Espaciado base en múltiplos de 4dp/8dp.
- CTA principal único por pantalla; secundarios con menor peso visual.

### Motion
- Duración estándar:
  - Entrada/salida de contenedor: 180–220ms
  - Micro-interacciones (botón/celda): 100–140ms
- Curvas recomendadas:
  - Entrada: ease-out
  - Salida: ease-in
  - Cambio de estado: standard ease-in-out
- Evitar animaciones en operaciones críticas de streaming.

### Accesibilidad
- Mantener contraste AA mínimo.
- Estados vacíos y de error con texto accionable.
- Focus/navegación de control remoto consistente en Android TV.

---

## Checklist de adopción por pantalla
- [ ] Usa `UiState<T>` como fuente única de estado.
- [ ] Renderiza con `UiStateRenderer<T>` o equivalente.
- [ ] Usa componentes compartidos (`TopBarConfig`, `UiCellModel`, `ActionButtonModel`, `EmptyStateModel`).
- [ ] Mantiene paridad funcional.
- [ ] No afecta el pipeline de streaming.
