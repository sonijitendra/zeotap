#!/bin/bash
# ═══════════════════════════════════════════════════════
# IMS Load Test — Simulates burst traffic
# ═══════════════════════════════════════════════════════

API_URL="${1:-http://localhost:8080}"
RATE="${2:-100}"
DURATION="${3:-10}"

echo "🔥 Load test: $RATE signals/sec for ${DURATION}s to $API_URL"

TOTAL=$((RATE * DURATION))
SENT=0

for i in $(seq 1 $TOTAL); do
  SIG_ID=$(python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null || echo "sig-$RANDOM-$i")
  COMP="LOAD_TEST_COMP_$((i % 5))"

  curl -s -X POST "$API_URL/api/v1/signals" \
    -H "Content-Type: application/json" \
    -d "{
      \"signalId\": \"$SIG_ID\",
      \"componentId\": \"$COMP\",
      \"severity\": \"P2\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
      \"message\": \"Load test signal $i\",
      \"metadata\": {\"test\": true, \"batch\": $i}
    }" > /dev/null &

  SENT=$((SENT + 1))

  if [ $((SENT % RATE)) -eq 0 ]; then
    echo "  Sent $SENT / $TOTAL signals"
    sleep 1
  fi
done

wait
echo "✅ Load test complete: $TOTAL signals sent"
