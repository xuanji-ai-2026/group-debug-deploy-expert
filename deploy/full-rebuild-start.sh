#!/bin/bash
set -e

echo "=== STEP 1: BUILD ==="
cd /opt/beijixing-ai/backend
mvn clean package -DskipTests -T 4 > /tmp/mvn-final.log 2>&1
if [ $? -ne 0 ]; then
    echo "BUILD FAILED"
    grep 'ERROR.*java' /tmp/mvn-final.log | head -10
    exit 1
fi
echo "BUILD SUCCESS"

echo "=== STEP 2: INJECT application.yml ==="
cd /opt/beijixing-ai/backend/beijixing-app/target
jar uf beijixing-app-1.0.0-SNAPSHOT.jar -C ../src/main/resources application.yml
echo "YML INJECTED"

echo "=== STEP 3: VERIFY YML ==="
unzip -p beijixing-app-1.0.0-SNAPSHOT.jar application.yml | head -8

echo "=== STEP 4: COPY TO DEPLOY ==="
cp beijixing-app-1.0.0-SNAPSHOT.jar /opt/beijixing-ai/deploy/
ls -lh /opt/beijixing-ai/deploy/beijixing-app-1.0.0-SNAPSHOT.jar

echo "=== STEP 5: KILL OLD PROCESS ==="
pkill -9 -f beijixing-app 2>/dev/null || true
sleep 2

echo "=== STEP 6: START SERVICE ==="
cd /opt/beijixing-ai/deploy
nohup java -Xms512m -Xmx1024m -XX:+UseG1GC \
    -Dspring.main.allow-bean-definition-overriding=true \
    -Dspring.main.allow-circular-references=true \
    -Dspring.flyway.enabled=false \
    -Dspring.datasource.url='jdbc:mariadb://localhost:3306/beijixing_ai?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai' \
    -Dspring.datasource.username=root \
    -Dspring.datasource.password='Beijixing@2024!' \
    -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
    -jar beijixing-app-1.0.0-SNAPSHOT.jar \
    > /opt/beijixing-ai/app.log 2>&1 &
echo "SERVICE STARTED, PID: $!"

echo "=== STEP 7: WAIT AND CHECK ==="
sleep 60
echo "--- PROCESS ---"
ps aux | grep beijixing-app | grep -v grep | head -2
echo "--- PORT ---"
ss -tlnp | grep :8080 || echo "NO 8080"
echo "--- PROFILE ---"
grep -i 'profile' /opt/beijixing-ai/app.log | head -3
echo "--- STARTED ---"
grep -E 'Started Beijixing|Tomcat started|Application run failed' /opt/beijixing-ai/app.log | tail -3
echo "--- CONFLICTS ---"
grep 'ConflictingBean' /opt/beijixing-ai/app.log | tail -3 || echo "NO CONFLICTS"
echo "--- CURL ---"
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/api/auth/login 2>/dev/null || echo "NO RESPONSE"

echo "=== DONE ==="
