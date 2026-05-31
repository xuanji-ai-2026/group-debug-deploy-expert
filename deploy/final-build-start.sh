#!/bin/bash
set -e

echo "=== KILL OLD ==="
pkill -9 -f beijixing-app 2>/dev/null || true
sleep 2

echo "=== BUILD ==="
cd /opt/beijixing-ai/backend
mvn clean package -DskipTests -T 4 -q
echo "BUILD DONE"

echo "=== COPY JAR ==="
cp /opt/beijixing-ai/backend/beijixing-app/target/beijixing-app-1.0.0-SNAPSHOT.jar /opt/beijixing-ai/deploy/

echo "=== START ==="
cd /opt/beijixing-ai/deploy
nohup java -Xms512m -Xmx1024m -XX:+UseG1GC -jar beijixing-app-1.0.0-SNAPSHOT.jar > /opt/beijixing-ai/app.log 2>&1 &
echo "PID: $!"

echo "=== WAIT 70s ==="
sleep 70

echo "=== VERIFY ==="
ps aux | grep beijixing-app | grep -v grep | head -1
ss -tlnp | grep :8080 || echo "NO_8080"
grep -E 'Started Beijixing|Tomcat started|Application run failed' /opt/beijixing-ai/app.log | tail -3
curl -s -w 'HTTP_%{http_code}' -o /dev/null http://127.0.0.1:8080/api/auth/login 2>/dev/null || echo "NO_RESPONSE"

echo "=== DONE ==="
