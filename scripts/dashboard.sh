#!/bin/bash

# Función para obtener métrica específica
get_metric() {
    local metric_name=$1
    local filter=$2
    curl -s http://localhost:8080/actuator/prometheus | grep "^$metric_name" | grep "$filter" | awk '{print $2}'
}

# Loop infinito
while true; do
    clear
    
    echo "╔════════════════════════════════════════════════════════╗"
    echo "║     TRANSACTION SERVICE - LIVE DASHBOARD              ║"
    echo "╚════════════════════════════════════════════════════════╝"
    echo ""
    
    # Timestamp
    echo "🕐 $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    # Transacciones
    TOTAL=$(get_metric "transactions_created_total" "")
    echo "📊 TRANSACTIONS"
    echo "   Total: ${TOTAL:-0}"
    
    TRANSFER=$(get_metric "transactions_created_by_type_total" 'type="TRANSFER"')
    PAYMENT=$(get_metric "transactions_created_by_type_total" 'type="PAYMENT"')
    WITHDRAWAL=$(get_metric "transactions_created_by_type_total" 'type="WITHDRAWAL"')
    
    echo "   └─ TRANSFER: ${TRANSFER:-0}"
    echo "   └─ PAYMENT: ${PAYMENT:-0}"
    echo "   └─ WITHDRAWAL: ${WITHDRAWAL:-0}"
    echo ""
    
    # Performance
    P50=$(get_metric "transactions_creation_time_seconds" 'quantile="0.5"')
    P95=$(get_metric "transactions_creation_time_seconds" 'quantile="0.95"')
    P99=$(get_metric "transactions_creation_time_seconds" 'quantile="0.99"')
    
    echo "⚡ PERFORMANCE"
    if [ -n "$P50" ]; then
        P50_MS=$(echo "$P50 * 1000" | bc | cut -d'.' -f1)
        P95_MS=$(echo "$P95 * 1000" | bc | cut -d'.' -f1)
        P99_MS=$(echo "$P99 * 1000" | bc | cut -d'.' -f1)
        echo "   p50: ${P50_MS}ms"
        echo "   p95: ${P95_MS}ms"
        echo "   p99: ${P99_MS}ms"
    else
        echo "   No data yet"
    fi
    echo ""
    
    # JVM
    HEAP_USED=$(get_metric "jvm_memory_used_bytes" 'area="heap"' | tail -1)
    HEAP_MAX=$(get_metric "jvm_memory_max_bytes" 'area="heap"' | tail -1)
    
    echo "💾 JVM MEMORY"
    if [ -n "$HEAP_USED" ]; then
        HEAP_USED_MB=$(echo "scale=0; $HEAP_USED / 1048576" | bc)
        HEAP_MAX_MB=$(echo "scale=0; $HEAP_MAX / 1048576" | bc)
        HEAP_PCT=$(echo "scale=1; ($HEAP_USED / $HEAP_MAX) * 100" | bc)
        
        # Barra de progreso ASCII
        BAR_WIDTH=30
        FILLED=$(echo "scale=0; ($HEAP_PCT / 100) * $BAR_WIDTH" | bc | cut -d'.' -f1)
        EMPTY=$((BAR_WIDTH - FILLED))
        
        BAR=$(printf "█%.0s" $(seq 1 $FILLED))$(printf "░%.0s" $(seq 1 $EMPTY))
        
        echo "   Heap: [$BAR] ${HEAP_PCT}%"
        echo "   ${HEAP_USED_MB}MB / ${HEAP_MAX_MB}MB"
    fi
    echo ""
    
    # GC
    GC_COUNT=$(get_metric "jvm_gc_pause_seconds_count" "" | awk '{sum+=$1} END {print sum}')
    echo "🗑️  GARBAGE COLLECTION"
    echo "   Pauses: ${GC_COUNT:-0}"
    echo ""
    
    echo "Press Ctrl+C to exit"
    echo ""
    
    # Refresca cada 2 segundos
    sleep 2
done
