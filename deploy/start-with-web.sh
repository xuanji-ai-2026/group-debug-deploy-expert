#!/bin/bash
pkill -9 -f beijixing-app 2>/dev/null
sleep 2

cd /opt/beijixing-ai/deploy

nohup java \
  -Xms512m -Xmx1024m -XX:+UseG1GC \
  -Dspring.main.web-application-type=servlet \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.main.allow-circular-references=true \
  -Dspring.flyway.enabled=false \
  -Dserver.port=8080 \
  -Dspring.datasource.url='jdbc:mariadb://localhost:3306/beijixing_ai?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai' \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password='Beijixing@2024!' \
  -Dspring.datasource.driver-class-name=org.mariadb.jdbc.Driver \
  -Dspring.data.redis.host=localhost \
  -Dspring.data.redis.port=6379 \
  -Dspring.data.redis.password= \
  -jar beijixing-app-1.0.0-SNAPSHOT.jar \
  > /opt/beijixing-ai/app.log 2>&1 &

echo "STARTED PID: $!"
sleep 60

echo "=== VERIFY ==="
ps aux | grep beijixing-app | grep -v grep | head -1
ss -tlnp | grep :8080 || echo "NO 8080"
grep -E 'Started Beijixing|Tomcat started|Application run failed|web-application-type' /opt/beijixing-ai/app.log | tail -5
curl -s -w 'HTTP_%{http_code}' -o /dev/null http://127.0.0.1:8080/api/auth/login 2>/dev/null || echo "NO_RESPONSE"
