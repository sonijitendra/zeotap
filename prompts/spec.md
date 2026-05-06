# Engineering Challenge: Mission-Critical IMS

## Original Specification

Build a production-grade Incident Management System (IMS) that handles high-throughput signal ingestion from distributed monitoring agents, performs intelligent debouncing and deduplication, manages incident lifecycle with proper state machine transitions, and provides real-time dashboards with comprehensive root cause analysis capabilities.

## Key Design Decisions

### Why Virtual Threads over WebFlux?
- Virtual threads (Project Loom) provide the same throughput benefits as reactive without the cognitive complexity
- Debugging is simpler — stack traces are meaningful
- Existing blocking libraries (JPA, Redisson) work seamlessly
- For incident management tooling where correctness > latency, this is the right tradeoff

### Why Kafka for Signal Ingestion?
- Decouples HTTP ingestion from processing — backpressure is handled naturally
- Partitioning by componentId ensures ordering per component
- Built-in retry and DLQ support
- Horizontal scaling by adding consumer instances

### Why Both PostgreSQL and MongoDB?
- PostgreSQL: ACID transactions for incident state machine (correctness is critical)
- MongoDB: High-throughput writes for raw signals with flexible schema (metadata varies)
- Each database is used for what it does best

### Why Redis for Debouncing?
- Sub-millisecond operations for atomic SETNX and INCR
- TTL-based windows auto-expire — no cleanup needed
- Distributed locks via Redisson for cross-instance coordination
