#!/usr/bin/env bash
set -euo pipefail

# Deploy WAR vào Tomcat local rồi khởi động.
# Ví dụ:
#   TOMCAT_HOME=/tmp/foodorder-tomcat/apache-tomcat-10.1.40 bash scripts/deploy-local-tomcat.sh

APP_NAME="FoodOrder-System"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WAR_FILE="$ROOT_DIR/build/$APP_NAME.war"
TOMCAT_HOME="${TOMCAT_HOME:-/tmp/foodorder-tomcat/apache-tomcat-10.1.40}"

if [[ ! -f "$WAR_FILE" ]]; then
  echo "Chưa có WAR tại: $WAR_FILE"
  echo "Hãy chạy: bash scripts/rebuild.sh"
  exit 1
fi

if [[ ! -d "$TOMCAT_HOME" ]]; then
  echo "Không tìm thấy TOMCAT_HOME: $TOMCAT_HOME"
  exit 1
fi

echo "==> Dừng Tomcat cũ (nếu có)"
pkill -f "$TOMCAT_HOME" || true

echo "==> Xóa bản deploy cũ"
rm -rf "$TOMCAT_HOME/webapps/$APP_NAME" "$TOMCAT_HOME/webapps/$APP_NAME.war"

echo "==> Copy WAR mới"
cp "$WAR_FILE" "$TOMCAT_HOME/webapps/$APP_NAME.war"

echo "==> Khởi động Tomcat"
"$TOMCAT_HOME/bin/startup.sh"

echo "==> Xong. Mở: http://localhost:8081/$APP_NAME/index.html"
