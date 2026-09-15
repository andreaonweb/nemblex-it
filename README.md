# Nemblex

Sistema de gestión de incidencias IT (helpdesk) con un agente de IA integrado: clasificación automática de tickets, propuestas de resolución vía RAG (Gemini + pgvector) y aprobación humana antes de aplicar cualquier acción sensible.

Monorepo con backend en Java/Spring Boot, frontend en Angular y PostgreSQL (con pgvector) como base de datos.

## Stack

- **Backend**: Java 21, Spring Boot 4.1.1 (Web, Data JPA, Security, Validation, Lombok), JWT (jjwt), springdoc-openapi (Swagger UI), Maven (con Maven Wrapper)
- **Frontend**: Angular 18 (standalone, routing habilitado), TypeScript, SCSS, Angular Material
- **Base de datos**: PostgreSQL con la extensión `pgvector` (para el RAG del agente de IA), vía Docker Compose
- **IA**: Google Gemini (clasificación automática de tickets y generación de propuestas de resolución)

## Estructura del repositorio

```
nemblex/
├── docker-compose.yml       # Levanta PostgreSQL + pgvector (puerto 5433 en el host)
├── init.sql                 # Esquema inicial de la base de datos (se ejecuta una sola vez)
├── backend/                 # API REST (Spring Boot)
│   ├── src/main/java/com/nemblex/
│   │   ├── controller/      # Controladores REST (TicketController, AuditLogController, ...)
│   │   ├── service/         # Lógica de negocio (interfaces + implementaciones)
│   │   ├── repository/      # Repositorios Spring Data JPA
│   │   ├── entity/          # Entidades JPA (AppUser, Ticket, AuditLog, Category, ...)
│   │   ├── mapper/          # Mappers entidad <-> DTO (MapStruct)
│   │   ├── dto/              # DTOs de request / response
│   │   ├── ai/               # Cliente de Gemini, clasificación y propuestas de acción
│   │   ├── event/            # Eventos de dominio
│   │   ├── exception/        # Excepciones de dominio y GlobalExceptionHandler
│   │   ├── config/            # Beans, CORS, OpenAPI, seed de datos de desarrollo, etc.
│   │   └── security/          # Configuración de seguridad y filtros JWT
│   ├── src/test/              # Tests (JUnit 5 + Mockito)
│   ├── .env.example            # Plantilla de variables de entorno
│   └── README.md                # Detalle específico del backend
└── frontend/                   # Aplicación web (Angular)
    ├── src/app/
    │   ├── auth/                # Login, guards e interceptor JWT
    │   ├── tickets/              # Gestión de incidencias (listado, detalle, "Mis tickets")
    │   ├── audit-logs/            # Registro de auditoría de acciones sobre tickets
    │   ├── approvals/              # Aprobación/rechazo de acciones propuestas por la IA
    │   ├── dashboard/               # Panel principal / métricas
    │   ├── shared/                   # Componentes, layout, i18n y utilidades compartidas
    │   └── core/                      # Servicios singleton, configuración transversal
    └── README.md                       # Detalle específico del frontend
```

## Requisitos previos

- JDK 21
- Node.js 20+ y npm
- Docker + Docker Compose (para levantar PostgreSQL con pgvector)

## Cómo levantar el proyecto en local

### 1. Base de datos

Desde la raíz del repositorio:

```bash
docker compose up -d
```

Esto expone PostgreSQL en `localhost:5433` (base `nemblex`, usuario `nemblex`, password `nemblex_dev_password`) y ejecuta `init.sql` la primera vez para crear el esquema.

> Se usa el puerto **5433** en el host para no chocar con una instalación local de PostgreSQL que ya ocupe el 5432.

### 2. Backend

```bash
cd backend
cp .env.example .env   # completar GEMINI_API_KEY si se va a probar el agente de IA
./mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run       # Linux / Mac
```

La API queda disponible en `http://localhost:8080` (Swagger UI en `http://localhost:8080/swagger-ui.html`).

Con el perfil `dev` activo (por defecto), al arrancar se ejecuta un seed automático (`DataSeeder`) que crea los usuarios de prueba listados más abajo si todavía no existen.

Más detalle (perfiles, tests, troubleshooting) en [`backend/README.md`](backend/README.md).

### 3. Frontend

```bash
cd frontend
npm install
npm start   # equivalente a: ng serve
```

La aplicación queda disponible en `http://localhost:4200` y apunta por defecto al backend en `http://localhost:8080`.

Más detalle en [`frontend/README.md`](frontend/README.md).

## Usuarios de prueba

El seed de desarrollo (`DataSeeder`, solo activo con el perfil `dev`) crea estos usuarios con contraseña conocida. Iniciá sesión en `http://localhost:4200` con cualquiera de estos correos para probar los distintos roles:

| Rol | Nombre | Email | Contraseña |
|---|---|---|---|
| ADMIN | Admin Nemblex | `admin@nemblex.dev` | `admin123` |
| SUPERVISOR | Beatriz Ruiz | `beatriz.ruiz@nemblex.dev` | `supervisor123` |
| TECHNICIAN | Ana Torres | `ana.torres@nemblex.dev` | `technician123` |
| EMPLOYEE | Carlos Mendez | `carlos.mendez@nemblex.dev` | `employee123` |
| EMPLOYEE | Lucia Camara | `lucia.camara@nemblex.dev` | `employee123` |
| EMPLOYEE | Javier Ferrer | `javier.ferrer@nemblex.dev` | `employee123` |
| EMPLOYEE | Paula Nogales | `paula.nogales@nemblex.dev` | `employee123` |
| EMPLOYEE | Sofia Duarte | `sofia.duarte@nemblex.dev` | `employee123` |
| EMPLOYEE | Ricardo Olmos | `ricardo.olmos@nemblex.dev` | `employee123` |

> Credenciales solo para entorno de desarrollo local. El seed nunca se ejecuta fuera del perfil `dev`.

## Funcionalidades principales

- **Autenticación JWT** (`POST /api/auth/login`).
- **Gestión de tickets** (`/api/tickets`): alta, listado con filtros, detalle, actualización y baja.
- **Clasificación automática por IA** (Gemini): categoría y prioridad sugeridas al crear un ticket.
- **Propuestas de resolución vía RAG** sobre una base de conocimiento (pgvector) con **aprobación humana obligatoria**: un ticket en `PENDING_APPROVAL` solo pasa a `RESOLVED` cuando un SUPERVISOR/ADMIN aprueba la acción propuesta (`/api/audit-logs`); si la rechaza, vuelve a `IN_PROGRESS`.
- **Autorización por rol**: TECHNICIAN / SUPERVISOR / ADMIN / EMPLOYEE.

## Contacto

💼 LinkedIn: [linkedin.com/in/andreaoliveraromero](https://linkedin.com/in/andreaoliveraromero)
✉️ Email: andreaonweb.dev@gmail.com
