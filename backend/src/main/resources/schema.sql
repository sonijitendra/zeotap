-- ════════════════════════════════════════════════════════════════
-- IMS PostgreSQL Schema
-- ════════════════════════════════════════════════════════════════

-- Incidents / Work Items
CREATE TABLE IF NOT EXISTS incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id    VARCHAR(255)    NOT NULL,
    severity        VARCHAR(10)     NOT NULL,
    state           VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    title           VARCHAR(500)    NOT NULL,
    description     TEXT,
    signal_count    INTEGER         NOT NULL DEFAULT 1,
    first_signal_at TIMESTAMPTZ     NOT NULL,
    resolved_at     TIMESTAMPTZ,
    closed_at       TIMESTAMPTZ,
    mttr_seconds    BIGINT,
    assigned_to     VARCHAR(255),
    version         INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incidents_component ON incidents(component_id);
CREATE INDEX IF NOT EXISTS idx_incidents_state ON incidents(state);
CREATE INDEX IF NOT EXISTS idx_incidents_severity ON incidents(severity);
CREATE INDEX IF NOT EXISTS idx_incidents_created ON incidents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_incidents_component_state ON incidents(component_id, state);

-- Root Cause Analysis
CREATE TABLE IF NOT EXISTS root_cause_analyses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id         UUID            NOT NULL REFERENCES incidents(id),
    incident_start_time TIMESTAMPTZ     NOT NULL,
    incident_end_time   TIMESTAMPTZ     NOT NULL,
    root_cause_category VARCHAR(100)    NOT NULL,
    root_cause_detail   TEXT            NOT NULL,
    fix_applied         TEXT            NOT NULL,
    prevention_steps    TEXT            NOT NULL,
    submitted_by        VARCHAR(255),
    submitted_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rca_incident UNIQUE(incident_id)
);

CREATE INDEX IF NOT EXISTS idx_rca_incident ON root_cause_analyses(incident_id);

-- Incident Timeline / State Transitions
CREATE TABLE IF NOT EXISTS incident_timelines (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID            NOT NULL REFERENCES incidents(id),
    from_state  VARCHAR(20),
    to_state    VARCHAR(20)     NOT NULL,
    changed_by  VARCHAR(255),
    notes       TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_timeline_incident ON incident_timelines(incident_id);
CREATE INDEX IF NOT EXISTS idx_timeline_created ON incident_timelines(created_at DESC);

-- Linked Signals (junction table linking signals to incidents)
CREATE TABLE IF NOT EXISTS incident_signals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID            NOT NULL REFERENCES incidents(id),
    signal_id   VARCHAR(255)    NOT NULL,
    linked_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_incident_signal UNIQUE(incident_id, signal_id)
);

CREATE INDEX IF NOT EXISTS idx_incident_signals_incident ON incident_signals(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_signals_signal ON incident_signals(signal_id);
