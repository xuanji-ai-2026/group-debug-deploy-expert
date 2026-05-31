#!/bin/bash
echo "=== LOCAL AUDIT: ALL application.yml FILES ==="
find /d/BeijiXing-AI/backend -name "application*.yml" -not -path "*/target/*" -not -path "*/node_modules/*" -type f

echo ""
echo "=== LOCAL AUDIT: @Configuration CLASSES ==="
find /d/BeijiXing-AI/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@Configuration" {} \; 2>/dev/null | while read f; do
    cls=$(basename "$f" .java)
    echo "$cls|$f"
done | sort | awk -F'|' '{n[$1]=n[$1]" "$2} END{for(i in n) split(n[i],a," "); if(length(a)>2) print "CONFLICT:"i" -> "n[i]}'

echo ""
echo "=== LOCAL AUDIT: @RestControllerAdvice ==="
find /d/BeijiXing-AI/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@RestControllerAdvice\|@ControllerAdvice" {} \; 2>/dev/null | while read f; do
    cls=$(basename "$f" .java)
    echo "$cls|$f"
done | sort | awk -F'|' '{n[$1]=n[$1]" "$2} END{for(i in n) split(n[i],a," "); if(length(a)>2) print "CONFLICT:"i" -> "n[i]}'

echo ""
echo "=== LOCAL AUDIT: @RestController/@Controller ==="
find /d/BeijiXing-AI/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@RestController\|@Controller" {} \; 2>/dev/null | while read f; do
    cls=$(basename "$f" .java)
    echo "$cls|$f"
done | sort | awk -F'|' '{n[$1]=n[$1]" "$2} END{for(i in n) split(n[i],a," "); if(length(a)>2) print "CONFLICT:"i" -> "n[i]}'

echo ""
echo "=== LOCAL AUDIT: FILE COUNT PER MODULE ==="
for mod in bx-ai bx-billing bx-common bx-content bx-data bx-gateway bx-lead bx-message bx-monitor bx-risk bx-schedule bx-social bx-storage bx-system bx-task bx-tenant bx-user beijixing-app; do
    count=$(find /d/BeijiXing-AI/backend/$mod/src -name "*.java" -type f 2>/dev/null | wc -l)
    echo "$mod: $count java files"
done

echo ""
echo "=== LOCAL: GlobalExceptionHandler ==="
find /d/BeijiXing-AI/backend -name "GlobalExceptionHandler.java" -not -path "*/target/*" -type f
echo "=== LOCAL: MybatisPlusConfig ==="
find /d/BeijiXing-AI/backend -name "MybatisPlusConfig.java" -not -path "*/target/*" -type f
echo "=== LOCAL: RedisConfig ==="
find /d/BeijiXing-AI/backend -name "RedisConfig.java" -not -path "*/target/*" -type f
echo "=== LOCAL: SecurityConfig ==="
find /d/BeijiXing-AI/backend -name "SecurityConfig.java" -not -path "*/target/*" -type f
echo "=== LOCAL: XxlJobConfig ==="
find /d/BeijiXing-AI/backend -name "XxlJobConfig.java" -not -path "*/target/*" -type f

echo ""
echo "=== DONE ==="
