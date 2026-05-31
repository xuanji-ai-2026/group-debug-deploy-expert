#!/bin/bash
set -e
APP_NAME=beijixing-app
APP_JAR=/opt/beijixing-ai/deploy/${APP_NAME}-1.0.0-SNAPSHOT.jar
APP_LOG=/opt/beijixing-ai/logs/app.log
APP_PID=/opt/beijixing-ai/logs/app.pid
ENV_FILE=/opt/beijixing-ai/deploy/.env
if [ -f $APP_PID ]; then
  OLD_PID=$(cat $APP_PID)
  if kill -0 $OLD_PID 2>/dev/null; then
    echo "Stopping PID: $OLD_PID"
    kill -15 $OLD_PID
    sleep 10
    kill -9 $OLD_PID 2>/dev/null || true
  fi
  rm -f $APP_PID
fi
source $ENV_FILE
export DB_PASSWORD REDIS_PASSWORD JWT_SECRET
echo "Starting $APP_NAME..."
nohup java -Xms512m -Xmx1024m -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar $APP_JAR --spring.profiles.active=prod > $APP_LOG 2>&1 &
echo $! > $APP_PID
echo "Started PID: $(cat $APP_PID)"