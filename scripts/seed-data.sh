#!/bin/bash
# ═══════════════════════════════════════════════════════
# IMS Seed Data Script — Generates realistic test data
# ═══════════════════════════════════════════════════════

API_URL="${1:-http://localhost:8080}"

echo "🔧 Seeding IMS with sample signals..."

COMPONENTS=("CACHE_CLUSTER_01" "DB_PRIMARY_01" "API_GATEWAY_01" "PAYMENT_SVC_01" "AUTH_SVC_01" "CDN_EDGE_01" "QUEUE_BROKER_01" "SEARCH_ENGINE_01")
SEVERITIES=("P0" "P1" "P2" "P2" "P1" "P2")
MESSAGES=(
  "Redis latency spike detected"
  "PostgreSQL connection pool exhausted"
  "API response time exceeding SLA threshold"
  "Payment processing timeout observed"
  "JWT validation failures increasing"
  "CDN cache hit ratio dropped below 60%"
  "RabbitMQ queue depth exceeding 100k"
  "Elasticsearch cluster health turned yellow"
  "Memory utilization exceeding 90%"
  "Disk IOPS reaching throttle limit"
)
REGIONS=("us-east-1" "us-west-2" "eu-west-1" "ap-south-1")

for i in $(seq 1 50); do
  COMP=${COMPONENTS[$((RANDOM % ${#COMPONENTS[@]}))]}
  SEV=${SEVERITIES[$((RANDOM % ${#SEVERITIES[@]}))]}
  MSG=${MESSAGES[$((RANDOM % ${#MESSAGES[@]}))]}
  REGION=${REGIONS[$((RANDOM % ${#REGIONS[@]}))]}
  SIG_ID=$(uuidgen 2>/dev/null || python3 -c "import uuid; print(uuid.uuid4())")

  curl -s -X POST "$API_URL/api/v1/signals" \
    -H "Content-Type: application/json" \
    -d "{
      \"signalId\": \"$SIG_ID\",
      \"componentId\": \"$COMP\",
      \"severity\": \"$SEV\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
      \"message\": \"$MSG\",
      \"metadata\": {
        \"host\": \"host-$((RANDOM % 10 + 1))\",
        \"region\": \"$REGION\"
      }
    }" > /dev/null

  echo "  [$i/50] Signal $SEV for $COMP"
  sleep 0.1
done

echo "✅ Seeded 50 signals successfully!"
echo ""
echo "📊 Check the dashboard at: http://localhost:3000"
echo "📚 Swagger UI at: http://localhost:8080/swagger-ui.html"
