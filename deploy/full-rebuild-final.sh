#!/bin/bash
set -e

echo "=== STEP 1: KILL OLD ==="
pkill -9 -f beijixing-app 2>/dev/null || true
sleep 2

echo "=== STEP 2: BUILD ==="
cd /opt/beijixing-ai/backend
mvn clean package -DskipTests -T 4 > /tmp/mvn-final2.log 2>&1
if [ $? -ne 0 ]; then
    echo "BUILD FAILED"
    grep 'ERROR.*java\|cannot find' /tmp/mvn-final2.log | head -10
    exit 1
fi
echo "BUILD SUCCESS"

echo "=== STEP 3: INJECT application.yml ==="
cd /opt/beijixing-ai/backend/beijixing-app/target
jar uf beijixing-app-1.0.0-SNAPSHOT.jar -C ../src/main/resources application.yml
echo "YML INJECTED"

echo "=== STEP 4: COPY TO DEPLOY ==="
cp beijixing-app-1.0.0-SNAPSHOT.jar /opt/beijixing-ai/deploy/

echo "=== STEP 5: START SERVICE ==="
cd /opt/beijixing-ai/deploy
nohup java \
  -Xms512m -Xmx1024m -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.main.allow-circular-references=true \
  -Dspring.flyway.enabled=false \
  -Dspring.datasource.url='jdbc:mariadb://localhost:3306/beijixing_ai?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai' \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password='Beijixing@2024!' \
  -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
  -Dspring.data.redis.password= \
  -jar beijixing-app-1.0.0-SNAPSHOT.jar \
  > /opt/beijixing-ai/app.log 2>&1 &
echo "STARTED PID: $!"

echo "=== STEP 6: WAIT 70s AND VERIFY ==="
sleep 70
echo "--- PROCESS ---"
ps aux | grep beijixing-app | grep -v grep | head -2
echo "--- PORT ---"
ss -tlnp | grep :8080 || echo "NO 8080"
echo "--- PROFILE ---"
grep -i 'profile' /opt/beijixing-ai/app.log | head -3
echo "--- STARTED ---"
grep -E 'Started Beijixing|Tomcat started|Application run failed' /opt/beijixing-ai/app.log | tail -3
echo "--- ERRORS ---"
grep 'ERROR\|ConflictingBean\|Caused by' /opt/beijixing-ai/app.log | tail -5 || echo "NO ERRORS"
echo "--- CURL ---"
curl -s -w 'HTTP_%{http_code}' -o /dev/null http://127.0.0.1:8080/api/auth/login 2>/dev/null || echo "NO RESPONSE"

echo "=== DONE ==="
