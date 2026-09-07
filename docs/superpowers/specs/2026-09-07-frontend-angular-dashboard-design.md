# Frontend Angular: Dashboard de gestión de incidencias IT

## Origen

Diseño importado desde Claude Design (`claude.ai/design/p/6e358ce7-163e-4dda-a427-cf75f458a8cf`,
archivo `Nemblex.dc.html`, proyecto "Dashboard de gestión de incidencias IT"). El canvas define
dos vistas (Incidencias, Aprobaciones) con datos y lógica de ejemplo embebidos en su propio JS
(`class Component extends DCLogic`), pensadas como referencia visual e interactiva, no como
código a copiar literalmente.

## Contexto

- Backend: Spring Boot, CRUD de tickets y de auditoría (`AuditLog`) ya implementados y probados
  (rama `dev`), con JWT + roles (TECHNICIAN/SUPERVISOR/ADMIN), flujo de aprobación
  ticket↔auditoría, y las correcciones de seguridad de la revisión de código previa.
- Frontend: scaffold puro de `ng new` (Angular 18, standalone, Angular Material y Angular CDK
  instalados, routing habilitado). Sin `HttpClient` configurado, sin rutas, sin componentes
  propios — carpetas `auth/`, `tickets/`, `dashboard/`, `shared/`, `core/` vacías.
- El agente de IA (clasificación automática, RAG) **no está implementado** en el backend. El
  mock del canvas asume que sí existe (badges de confianza, motivo de una IA, cola de
  aprobaciones con propuestas automáticas).

## Decisiones de diseño

1. **UI real conectada al backend real**, no una réplica estática del mock. Donde el backend no
   tenga el dato porque depende del agente de IA (aún no construido), se deja la UI preparada
   pero con esos campos específicos ausentes/estáticos en vez de inventar números — no se
   fabrican confianzas ni razonamientos falsos para datos reales.
2. **Fuera de alcance para esta v1** (el modelo de dominio no los tiene, no es solo un tema de
   IA pendiente):
   - SLA / fecha límite por ticket (columna SLA, KPI "SLA en riesgo") — `Ticket` no tiene ese
     campo.
   - Niveles de escalado N1/N2 (botón "Escalar a N2") — el modelo de roles es
     TECHNICIAN/SUPERVISOR/ADMIN, sin jerarquía de niveles.
   - Barra de confianza IA / origen de clasificación automática — sin agente de IA, no hay de
     dónde sacarlo. La pestaña se mantiene como "Clasificación" mostrando la categoría real
     (`categoryName`) con nota de "clasificación manual".
3. **Selector de rol "Ver como" del mock se elimina.** Era un truco de demo. El rol viaja en el
   claim `role` del JWT (no en el body de `POST /api/auth/login`, que solo devuelve
   `{token, email}`); `AuthService` lo decodifica client-side para pintar la UI, la autorización
   real la sigue validando el backend en cada request.
4. **Botón "Resolver" del panel de detalle**: una resolución manual del técnico se ejecuta de
   inmediato, sin pasar por el flujo de aprobación de un supervisor (esa aprobación es
   exclusiva de las propuestas del futuro agente de IA). Requiere un método nuevo en el backend
   — ver más abajo.
5. **Actividad del ticket** (`GET /api/audit-logs/ticket/{id}`) va a mostrarse mucho más corta
   que en el mock (que tiene 2-4 entradas narrativas por ticket): en la práctica va a haber 0-1
   entradas hasta que exista el agente de IA o se resuelva manualmente. Es el comportamiento
   correcto, no un bug.
6. **Cola de Aprobaciones**: tras aprobar/rechazar, la tarjeta se mantiene en el estado local del
   componente (no desaparece de la vista), marcada como resuelta con un botón "Deshacer" visible
   — reproduce la UX del mock y le da un lugar de uso obvio al endpoint `undo` ya construido.
   Cada tarjeta pendiente enriquece sus datos con un fetch del ticket asociado (asunto,
   prioridad, categoría); los campos de confianza/regla de la IA se omiten por no existir en
   `AuditLogResponse`.

## Adición de backend necesaria: resolución manual directa

**Nuevo método:** `AuditLogService.resolveDirectly(Long ticketId, AuditLogRequest dto, Long technicianId)`

- Busca el ticket por `ticketId` (404 `ResourceNotFoundException` si no existe).
- Rechaza con `BadRequestException` si el ticket ya está `RESOLVED` o `CLOSED` (nada que
  resolver). A diferencia de `createLog`, **no exige** que el ticket esté en
  `PENDING_APPROVAL` — funciona desde `NEW`, `AI_CLASSIFIED`, `IN_PROGRESS` o
  `PENDING_APPROVAL`.
- Crea el `AuditLog` con `action`/`reason` del body, `resultStatus = APPROVED` desde el inicio
  (nunca pasa por `PENDING`), y `approvedBy = technicianId` — reinterpretando ese campo como
  "quién respalda esta resolución" (el propio técnico en una resolución directa, un supervisor
  cuando aprueba una propuesta pendiente).
- Mueve el ticket a `RESOLVED` y lo guarda.
- El guard existente en `updateTicket` (bloquea `PUT /api/tickets/{id}` con `status=RESOLVED`
  directo) queda intacto — este es un método/endpoint nuevo y específico, no un agujero en el
  genérico.

**Endpoint:** `POST /api/audit-logs/resolve-now`, mismo shape de body que `createLog`
(`{ticketId, action, reason}`), mismo rol que `createLog` (TECHNICIAN, SUPERVISOR, ADMIN).

## Arquitectura del frontend

- **Angular Material-first**: componentes estructurales (toolbar, tabs, botones, chips, campos
  de formulario, panel lateral) con Angular Material, retemados con la paleta del mock (navy
  `#0C3B77`, ámbar `#FFC24D`, Source Sans 3 / IBM Plex Mono) vía tema Material custom + overrides
  puntuales. Ya es el stack elegido en el proyecto (README del frontend).
- **Estado**: signals nativos de Angular 18 en servicios/componentes — sin librería de estado
  (NgRx), la app no lo justifica.
- **Servicios** (uno por dominio, mapeando los controllers reales):
  - `AuthService`: login, logout, signal de usuario/rol actual, decodifica el claim `role` del
    JWT.
  - `HttpInterceptorFn`: adjunta `Authorization: Bearer <token>` a cada request.
  - `TicketService`: list/get/update/delete/create + `resolveNow(id, dto)`.
  - `AuditLogService`: listByTicket/listPending/resolve/undo.
- **Rutas**: `/login` (pública), `/tickets` (cualquier rol autenticado), `/aprobaciones` (guard
  SUPERVISOR/ADMIN, igual que ya protege `SecurityConfig` en el backend).

## Testing

Mismo criterio que en el backend — TDD para lo que tiene lógica real:
- `AuthService`, `TicketService`, `AuditLogService`: tests con `HttpClientTestingModule`
  verificando URL/método/body de cada llamada y manejo de errores.
- Lógica de filtros, cálculo de KPIs, gating de rutas por rol: tests unitarios de la
  función/computed.
- Sin tests de markup/estilos pixel a pixel — se verifica visualmente corriendo la app.

## Fases de implementación

Se ejecutan **una por una, con confirmación explícita del usuario entre cada fase**. No se
avanza a la siguiente sin luz verde.

1. **Backend**: `AuditLogService.resolveDirectly` + endpoint `POST /api/audit-logs/resolve-now`
   (TDD, mismo patrón que `resolveLog`/`undoResolution`).
2. **Frontend base**: `provideHttpClient`, tema Material, `AuthService` + interceptor + guards,
   página de login, layout/header con navegación real (sin selector de rol demo).
3. **Listado de incidencias**: KPIs (sin SLA), filtros (estado/prioridad/clasificación),
   búsqueda, tabla — conectado a `TicketService`.
4. **Panel de detalle**: tabs (Resumen/Clasificación/Descripción), actividad, "Asignarme",
   "Resolver" (vía `resolve-now`).
5. **Aprobaciones**: cola conectada a `AuditLogService`, aprobar/rechazar/deshacer, tarjetas
   resueltas con "Deshacer" inline.
