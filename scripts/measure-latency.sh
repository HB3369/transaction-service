#!/bin/bash

echo "Measuring transaction endpoint latency..."
echo "Requests: 50"
echo ""

total=0
success=0
errors=0

for i in {1..50}; do
    start=$(date +%s%3N)
    
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST http://localhost:8080/api/v1/transactions \
      -H "Content-Type: application/json" \
      -d "{
        \"accountId\": \"ACC$(printf '%03d' $i)\",
        \"amount\": 100.50,
        \"type\": \"TRANSFER\"
      }")
    
    end=$(date +%s%3N)
    latency=$((end - start))
    
    if [ "$http_code" -eq 201 ]; then
        total=$((total + latency))
        success=$((success + 1))
    else
        errors=$((errors + 1))
    fi
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "Progress: $i/50"
    fi
done

avg=$((total / success))
echo ""
echo "========================================="
echo "Results:"
echo "  Success: $success"
echo "  Errors: $errors"
echo "  Average latency: ${avg}ms"
echo "========================================="
