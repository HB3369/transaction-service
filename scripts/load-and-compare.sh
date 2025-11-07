#!/bin/bash

echo "========================================="
echo "Load Testing with Metrics Comparison"
echo "========================================="
echo ""

# Captura métricas iniciales
echo "📸 Capturing initial metrics..."
INITIAL_COUNT=$(curl -s http://localhost:8080/actuator/prometheus | grep "^transactions_created_total" | awk '{print $2}')
echo "   Initial count: $INITIAL_COUNT"

# Genera carga sostenida
echo ""
echo "🔥 Generating load (100 requests)..."
START_TIME=$(date +%s)

for i in {1..100}; do
    TYPE="TRANSFER"
    [ $((i % 4)) -eq 0 ] && TYPE="PAYMENT"
    [ $((i % 7)) -eq 0 ] && TYPE="WITHDRAWAL"
    
    curl -X POST http://localhost:8080/api/v1/transactions \
      -H "Content-Type: application/json" \
      -d "{
        \"accountId\": \"ACC$(printf '%04d' $i)\",
        \"amount\": $((50 + RANDOM % 950)).$(printf '%02d' $((RANDOM % 100))),
        \"type\": \"$TYPE\"
      }" -s > /dev/null &
    
    # Limita concurrencia a 10
    if [ $((i % 10)) -eq 0 ]; then
        wait
        echo "   Progress: $i/100"
    fi
done

wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "✅ Load completed in ${DURATION}s"
echo ""

# Espera a que las métricas se actualicen
echo "⏳ Waiting for metrics to update..."
sleep 3

# Captura métricas finales
echo ""
echo "📊 Final Metrics:"
echo ""  

METRICS=$(curl -s http://localhost:8080/actuator/prometheus)

# Total
FINAL_COUNT=$(echo "$METRICS" | grep "^transactions_created_total" | awk '{print $2}')
NEW_TXS=$(echo "$FINAL_COUNT - $INITIAL_COUNT" | bc)
echo "✅ Total transactions: $FINAL_COUNT (+$NEW_TXS)"

# Por tipo
echo ""
echo "📈 By type:"
echo "$METRICS" | grep "^transactions_created_by_type_total" | while read line; do
    TYPE=$(echo "$line" | grep -o 'type="[^"]*"' | cut -d'"' -f2)
    COUNT=$(echo "$line" | awk '{print $2}')
    echo "   - $TYPE: $COUNT"
done

# Latencias
echo ""
echo "⏱️  Performance:"
P50=$(echo "$METRICS" | grep "transactions_creation_time_seconds{" | grep 'quantile="0.5"' | awk '{print $2}')
P95=$(echo "$METRICS" | grep "transactions_creation_time_seconds{" | grep 'quantile="0.95"' | awk '{print $2}')
P99=$(echo "$METRICS" | grep "transactions_creation_time_seconds{" | grep 'quantile="0.99"' | awk '{print $2}')

if [ -n "$P50" ]; then
    P50_MS=$(echo "$P50 * 1000" | bc | cut -d'.' -f1)
    P95_MS=$(echo "$P95 * 1000" | bc | cut -d'.' -f1)
    P99_MS=$(echo "$P99 * 1000" | bc | cut -d'.' -f1)
    echo "   - p50: ${P50_MS}ms"
    echo "   - p95: ${P95_MS}ms"
    echo "   - p99: ${P99_MS}ms"
    
    # Throughput
    THROUGHPUT=$(echo "scale=2; $NEW_TXS / $DURATION" | bc)
    echo "   - Throughput: ${THROUGHPUT} req/s"
fi

# Errores
ERRORS=$(echo "$METRICS" | grep "^transactions_creation_errors_total" | awk '{sum+=$2} END {print sum}')
if [ -n "$ERRORS" ] && [ "$ERRORS" != "0" ]; then
    ERROR_RATE=$(echo "scale=2; ($ERRORS / $NEW_TXS) * 100" | bc)
    echo ""
    echo "⚠️  Errors: $ERRORS (${ERROR_RATE}%)"
else
    echo ""
    echo "✅ No errors"
fi

# JVM Stats
echo ""
echo "💾 JVM Memory:"
HEAP_USED=$(echo "$METRICS" | grep "^jvm_memory_used_bytes.*heap" | grep 'area="heap"' | tail -1 | awk '{print $2}')
HEAP_MAX=$(echo "$METRICS" | grep "^jvm_memory_max_bytes.*heap" | grep 'area="heap"' | tail -1 | awk '{print $2}')

if [ -n "$HEAP_USED" ]; then
    HEAP_USED_MB=$(echo "scale=0; $HEAP_USED / 1048576" | bc)
    HEAP_MAX_MB=$(echo "scale=0; $HEAP_MAX / 1048576" | bc)
    HEAP_PCT=$(echo "scale=1; ($HEAP_USED / $HEAP_MAX) * 100" | bc)
    echo "   - Heap: ${HEAP_USED_MB}MB / ${HEAP_MAX_MB}MB (${HEAP_PCT}%)"
fi

# GC Stats
GC_COUNT=$(echo "$METRICS" | grep "^jvm_gc_pause_seconds_count" | awk '{sum+=$2} END {print sum}')
if [ -n "$GC_COUNT" ]; then
    echo "   - GC pauses: $GC_COUNT"
fi

echo ""
echo "========================================="
