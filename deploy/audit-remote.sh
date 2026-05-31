#!/bin/bash
echo "=== AUDIT: ALL @Configuration ANNOTATED CLASSES ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@Configuration" {} \; 2>/dev/null | while read f; do
    cls=$(basename "$f" .java)
    echo "$cls|$f"
done | sort | awk -F'|' '{n[$1]=n[$1]" "$2} END{for(i in n) split(n[i],a," "); if(length(a)>2) print "CONFLICT:"i" found in "length(a)-1" modules -> "n[i]}'

echo ""
echo "=== AUDIT: ALL @RestControllerAdvice/@ControllerAdvice ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@RestControllerAdvice\|@ControllerAdvice" {} \; 2>/dev/null | while read f; do
    cls=$(basename "$f" .java)
    echo "$cls|$f"
done | sort | awk -F'|' '{n[$1]=n[$1]" "$2} END{for(i in n) split(n[i],a," "); if(length(a)>2) print "CONFLICT:"i" found in "length(a)-1" modules -> "n[i]}'

echo ""
echo "=== AUDIT: ALL @RestController/@Controller ==="
find /opt/beijixing-ai/backend -name "*.java" -not -path "*/target/*" -exec grep -l "@RestController\|@Controller" {} \; 2>/dev/null | while read f; do
    cls=$(basename "$f" .java)
    echo "$cls|$f"
done | sort | awk -F'|' '{n[$1]=n[$1]" "$2} END{for(i in n) split(n[i],a," "); if(length(a)>2) print "CONFLICT:"i" found in "length(a)-1" modules -> "n[i]}'

echo ""
echo "=== AUDIT: ALL application.yml FILES ==="
find /opt/beijixing-ai/backend -name "application*.yml" -not -path "*/target/*" -type f

echo ""
echo "=== AUDIT: REMOTE vs LOCAL FILE COUNT PER MODULE ==="
for mod in bx-ai bx-billing bx-common bx-content bx-data bx-gateway bx-lead bx-message bx-monitor bx-risk bx-schedule bx-social bx-storage bx-system bx-task bx-tenant bx-user beijixing-app; do
    count=$(find /opt/beijixing-ai/backend/$mod/src -name "*.java" -type f 2>/dev/null | wc -l)
    echo "$mod: $count java files"
done

echo ""
echo "=== AUDIT: FILES ON REMOTE BUT NOT IN LOCAL (stale files) ==="
echo "Checking for GlobalExceptionHandler..."
find /opt/beijixing-ai/backend -name "GlobalExceptionHandler.java" -not -path "*/target/*" -type f
echo "Checking for MybatisPlusConfig..."
find /opt/beijixing-ai/backend -name "MybatisPlusConfig.java" -not -path "*/target/*" -type f
echo "Checking for RedisConfig..."
find /opt/beijixing-ai/backend -name "RedisConfig.java" -not -path "*/target/*" -type f
echo "Checking for SecurityConfig..."
find /opt/beijixing-ai/backend -name "SecurityConfig.java" -not -path "*/target/*" -type f
echo "Checking for XxlJobConfig..."
find /opt/beijixing-ai/backend -name "XxlJobConfig.java" -not -path "*/target/*" -type f

echo ""
echo "=== AUDIT: JAR STRUCTURE CHECK ==="
echo "Main-Class in JAR:"
unzip -p /opt/beijixing-ai/deploy/beijixing-app-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF 2>/dev/null | head -5
echo "application.yml in JAR:"
jar tf /opt/beijixing-ai/deploy/beijixing-app-1.0.0-SNAPSHOT.jar 2>/dev/null | grep "application" | head -10
echo "Spring Boot Starter Web in JAR:"
jar tf /opt/beijixing-ai/deploy/beijixing-app-1.0.0-SNAPSHOT.jar 2>/dev/null | grep "spring-boot-starter-web" | head -3
echo "Tomcat embedded in JAR:"
jar tf /opt/beijixing-ai/deploy/beijixing-app-1.0.0-SNAPSHOT.jar 2>/dev/null | grep "tomcat-embed" | head -3

echo ""
echo "=== DONE ==="
