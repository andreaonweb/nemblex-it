-- Se ejecuta una unica vez, en la primera inicializacion del volumen de datos.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('TECHNICIAN', 'SUPERVISOR', 'ADMIN', 'EMPLOYEE')),
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE category (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT
);

CREATE TABLE ticket (
    id                  SERIAL PRIMARY KEY,
    title               VARCHAR(200) NOT NULL,
    description         TEXT NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'NEW'
                         CHECK (status IN ('NEW', 'AI_CLASSIFIED', 'IN_PROGRESS', 'PENDING_APPROVAL', 'RESOLVED', 'CLOSED')),
    priority            VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
                         CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    category_id         INTEGER REFERENCES category(id),
    created_by          INTEGER NOT NULL REFERENCES app_user(id),
    assigned_to         INTEGER REFERENCES app_user(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
    id                  SERIAL PRIMARY KEY,
    ticket_id           INTEGER NOT NULL REFERENCES ticket(id),
    action              VARCHAR(50) NOT NULL,
    reason              TEXT,
    result_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (result_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    approved_by         INTEGER REFERENCES app_user(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE knowledge_document (
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    embedding       vector(768) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_category ON ticket(category_id);
CREATE INDEX idx_audit_log_ticket ON audit_log(ticket_id);
