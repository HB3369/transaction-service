#!/bin/bash

echo "==================================="
echo "Transaction Service - Metrics"
echo "==================================="
echo ""

METRICS=$(curl -s http://localhost:8080/actuator/prometheus)

# Transacciones creadas
CREATED=$(echo "$METRICS" | grep "^transactions_created_total" | awk '{print $2}')
echo "✅ Total transactions created: $CREATED"

# Por tipo
echo ""
echo "📊 By type:"
echo "$METRICS" | grep "^transactions_created_by_type_total" | while read line; do
    TYPE=$(echo "$line" | grep -o 'type="[^"]*"' | cut -d'"' -f2)
    COUNT=$(echo "$line" | awk '{print $2}')
    echo "   - $TYPE: $COUNT"
done

# Latencia
echo ""
echo "⏱️  Creation time (percentiles):"
P50=$(echo "$METRICS" | grep "transactions_creation_time_seconds" | grep 'quantile="0.5"' | awk '{print $2}')
P95=$(echo "$METRICS" | grep "transactions_creation_time_seconds" | grep 'quantile="0.95"' | awk '{print $2}')
P99=$(echo "$METRICS" | grep "transactions_creation_time_seconds" | grep 'quantile="0.99"' | awk '{print $2}')

if [ -n "$P50" ]; then
    P50_MS=$(echo "$P50 * 1000" | bc)
    P95_MS=$(echo "$P95 * 1000" | bc)
    P99_MS=$(echo "$P99 * 1000" | bc)
    echo "   - p50: ${P50_MS}ms"
    echo "   - p95: ${P95_MS}ms"
    echo "   - p99: ${P99_MS}ms"
else
    echo "   - No data yet (create some transactions first)"
fi

# JVM Memory
echo ""
echo "💾 JVM Memory:"
HEAP_USED=$(echo "$METRICS" | grep "^jvm_memory_used_bytes.*heap" | grep 'area="heap"' | awk '{print $2}')
HEAP_MAX=$(echo "$METRICS" | grep "^jvm_memory_max_bytes.*heap" | grep 'area="heap"' | awk '{print $2}')

if [ -n "$HEAP_USED" ]; then
    HEAP_USED_MB=$(echo "$HEAP_USED / 1048576" | bc)
    HEAP_MAX_MB=$(echo "$HEAP_MAX / 1048576" | bc)
    echo "   - Heap used: ${HEAP_USED_MB} MB / ${HEAP_MAX_MB} MB"
fi

echo ""
echo "==================================="
