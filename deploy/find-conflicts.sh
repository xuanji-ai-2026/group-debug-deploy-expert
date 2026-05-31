#!/bin/bash
echo "=== FIND ALL @Configuration CONFLICTS ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@Configuration" {} \; 2>/dev/null | while read f; do
    basename "$f" .java
done | sort | uniq -c | sort -rn | awk '$1 > 1 {print "CONFLICT:"$0}'

echo ""
echo "=== FIND ALL @RestControllerAdvice CONFLICTS ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@RestControllerAdvice\|@ControllerAdvice" {} \; 2>/dev/null | while read f; do
    basename "$f" .java
done | sort | uniq -c | sort -rn | awk '$1 > 1 {print "CONFLICT:"$0}'

echo ""
echo "=== FIND ALL @RestController CONFLICTS ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@RestController" {} \; 2>/dev/null | while read f; do
    basename "$f" .java
done | sort | uniq -c | sort -rn | awk '$1 > 1 {print "CONFLICT:"$0}'

echo ""
echo "=== LIST ALL @Configuration FILES ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@Configuration" {} \; 2>/dev/null | sort
