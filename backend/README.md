# Nemblex Backend

API REST del sistema de gestión de incidencias IT de Nemblex, con soporte para un agente de IA (clasificación automática, RAG y aprobación humana de acciones).

## Stack

- Java 21
- Spring Boot 4.1.1 (Web, Data JPA, Security, Validation, Lombok)
- PostgreSQL (driver JDBC)
- JWT (jjwt) para autenticación
- springdoc-openapi (Swagger UI)
- Maven (con Maven Wrapper, no requiere instalación local de Maven)

> **Nota sobre la versión de Spring Boot:** al momento de generar este proyecto, Spring Initializr ya no ofrece
> ninguna versión de la línea 3.x (fuera de soporte OSS). Se usó la última versión estable disponible, 4.1.1,
> que mantiene compatibilidad con Java 21.

## Estructura de paquetes

```
com.nemblex
├── controller   # Controladores REST
├── service      # Lógica de negocio
├── repository   # Repositorios Spring Data JPA
├── model        # Entidades JPA
├── dto          # Objetos de transferencia de datos
├── config       # Configuración (beans, CORS, OpenAPI, etc.)
└── security     # Configuración de seguridad y JWT
```

## Requisitos previos

- JDK 21
- Docker + Docker Compose (para levantar PostgreSQL con pgvector en local), o una instancia propia de PostgreSQL en `localhost:5433` con una base de datos llamada `nemblex` (y opcionalmente `nemblex_test` para el perfil de test)
- No necesitas tener Maven instalado: el proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Base de datos (PostgreSQL + pgvector) con Docker Compose

En la **raíz del repositorio** (mismo nivel que `backend/` y `frontend/`, ya que es infraestructura compartida) hay
un `docker-compose.yml` que levanta PostgreSQL con la extensión `pgvector` (necesaria para el RAG del agente de IA).
`init.sql`, también en la raíz, se ejecuta en la primera inicialización del volumen: habilita la extensión y crea el
esquema inicial (`app_user`, `category`, `ticket`, `audit_log` con sus constraints e índices).

Ejecuta los comandos de `docker compose` desde la raíz del repositorio:

```bash
cd ..            # desde backend/ hasta la raíz del repo
docker compose up -d
```

Esto expone Postgres en **`localhost:5433`** con:

- Base de datos: `nemblex`
- Usuario: `nemblex`
- Password: `nemblex_dev_password` (credencial de desarrollo local, no usar en otros entornos)

> Se usa el puerto **5433** en el host (mapeado al 5432 del contenedor) para no chocar con una instalación
> local de PostgreSQL que ya ocupe el 5432. El `DB_URL` de `.env` / `application.yml` ya apunta al 5433.

Para verificar que el contenedor está saludable (también desde la raíz del repo):

```bash
docker compose ps
```

Para pararlo (conservando los datos en el volumen `nemblex_pgdata`):

```bash
docker compose down
```

> Si cambias el esquema de `init.sql` y necesitas re-ejecutarlo, elimina el volumen: `docker compose down -v`.

## Configuración

1. Copia `.env.example` a `.env` y completa los valores (por defecto ya coinciden con el `docker-compose.yml`):

   ```
   DB_URL=jdbc:postgresql://localhost:5433/nemblex
   DB_USER=nemblex
   DB_PASSWORD=nemblex_dev_password
   SPRING_PROFILES_ACTIVE=dev
   GEMINI_API_KEY=tu_api_key
   ```

2. Exporta esas variables en tu entorno antes de levantar la aplicación (o usa un plugin/herramienta de tu IDE que cargue el `.env`).

## Levantar en local

```bash
# Windows
./mvnw.cmd spring-boot:run

# Linux / Mac
./mvnw spring-boot:run
```

La API quedará disponible en `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Compilar

```bash
./mvnw.cmd clean install
```

> Los tests que arrancan el contexto completo de Spring (`@SpringBootTest`) requieren una instancia de PostgreSQL
> accesible y las variables `DB_USER`/`DB_PASSWORD` configuradas (`docker compose up -d` la deja lista). Para
> compilar y empaquetar sin necesidad de una base de datos activa, usa `./mvnw.cmd clean install -DskipTests`.

## Perfiles

- `dev`: apunta a la base de datos `nemblex`, `ddl-auto: update`, SQL visible en logs.
- `test`: apunta a la base de datos `nemblex_test`, `ddl-auto: create-drop`.

El perfil activo se controla con la variable de entorno `SPRING_PROFILES_ACTIVE` (por defecto `dev`).
