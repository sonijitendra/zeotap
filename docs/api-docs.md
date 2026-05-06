# API Documentation

## Base URL
`http://localhost:8080/api/v1`

## Authentication
None (internal SRE tooling). In production: OAuth2/OIDC with RBAC.

---

## Signal Ingestion

### POST /signals
Ingest a single monitoring signal.

**Request Body:**
```json
{
  "signalId": "uuid-string",
  "componentId": "CACHE_CLUSTER_01",
  "severity": "P2",
  "timestamp": "2026-05-06T10:20:30Z",
  "message": "Redis latency spike",
  "metadata": { "host": "cache-1", "region": "ap-south-1" }
}
```

**Response (202 Accepted):**
```json
{ "success": true, "message": "ACCEPTED", "data": "Signal accepted for processing" }
```

### POST /signals/batch
Ingest multiple signals in one request.

---

## Incidents

### GET /incidents
List incidents with optional filters.

**Query Params:** `state`, `severity`, `componentId`, `page`, `size`, `sort`

### GET /incidents/active
Active incidents sorted by severity (for dashboard).

### GET /incidents/{id}
Get incident detail.

### PATCH /incidents/{id}/transition
Transition incident state.

**Request Body:**
```json
{ "targetState": "INVESTIGATING", "changedBy": "engineer@co.com", "notes": "Starting investigation" }
```

**Valid Transitions:** OPEN→INVESTIGATING, INVESTIGATING→RESOLVED, RESOLVED→CLOSED (requires RCA), INVESTIGATING→OPEN, RESOLVED→INVESTIGATING

### GET /incidents/{id}/timeline
Full state transition history.

### GET /incidents/{id}/signals
Raw signals linked to this incident.

### GET /incidents/stream (SSE)
Server-Sent Events stream for real-time updates.

---

## Root Cause Analysis

### POST /incidents/{incidentId}/rca
Submit RCA. All fields mandatory.

**Request Body:**
```json
{
  "incidentStartTime": "2026-05-06T10:00:00Z",
  "incidentEndTime": "2026-05-06T11:30:00Z",
  "rootCauseCategory": "Infrastructure",
  "rootCauseDetail": "Redis node ran out of memory due to missing eviction policy",
  "fixApplied": "Added maxmemory-policy allkeys-lru, expanded node memory",
  "preventionSteps": "Automated memory monitoring alerts, capacity planning review",
  "submittedBy": "sre@company.com"
}
```

### GET /incidents/{incidentId}/rca
Retrieve RCA for an incident.

---

## Dashboard

### GET /dashboard
Aggregated metrics: active count, signals today, severity/state breakdown, avg MTTR.

---

## Observability

- **GET /actuator/health** — Health check
- **GET /actuator/prometheus** — Prometheus metrics
- **GET /swagger-ui.html** — Swagger UI
