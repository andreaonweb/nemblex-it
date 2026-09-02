# Nemblex Frontend

Aplicación web (Angular) del sistema de gestión de incidencias IT de Nemblex, con soporte para un agente de IA (clasificación automática, RAG y aprobación humana de acciones).

## Stack

- Angular 18 (standalone, routing habilitado)
- TypeScript
- SCSS
- Angular Material (sistema de componentes)

## Estructura de carpetas

```
src/app
├── auth        # Autenticación (login, guards, interceptors de JWT, etc.)
├── tickets     # Gestión de incidencias/tickets
├── dashboard   # Panel principal / métricas
├── shared      # Componentes, pipes y utilidades compartidas
└── core        # Servicios singleton, configuración transversal
```

## Requisitos previos

- Node.js 20+
- npm

## Configuración

La URL base de la API se configura en:

- `src/environments/environment.development.ts` (usado por `ng serve`)
- `src/environments/environment.ts` (usado en build de producción)

Por defecto ambos apuntan a `http://localhost:8080`, donde corre el backend en local.

## Instalar dependencias

```bash
npm install
```

## Levantar en local

```bash
npm start
# equivalente a: ng serve
```

La aplicación quedará disponible en `http://localhost:4200`.

## Build de producción

```bash
npm run build
```

Los artefactos se generan en `dist/frontend`.

## Tests

```bash
npm test
```
