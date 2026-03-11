#!/bin/bash

echo "🚀 Starting Finly Backend..."

PORT=8080

# Find process on port (errorni ignore qilamiz)
PID=$(lsof -ti tcp:$PORT 2>/dev/null)

if [ -n "$PID" ]; then
  echo "⛔ Stopping existing process on port $PORT (PID: $PID)..."
  kill -9 $PID
  sleep 2
else
  echo "✅ Port $PORT is free"
fi

echo "🔨 Running Spring Boot application..."
mvn spring-boot:run