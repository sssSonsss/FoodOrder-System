#!/usr/bin/env bash
set -euo pipefail

# Script rebuild nhanh cho FoodOrder-System
# Chạy từ thư mục gốc dự án:
#   bash scripts/rebuild.sh

APP_NAME="FoodOrder-System"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build"
CLASS_DIR="$BUILD_DIR/classes"

echo "==> Dọn thư mục build cũ"
rm -rf "$CLASS_DIR"
mkdir -p "$CLASS_DIR"

echo "==> Compile Java source"
javac -cp "$ROOT_DIR/lib/postgresql-42.7.3.jar:$ROOT_DIR/lib/jakarta.servlet-api-6.0.0.jar" \
      -d "$CLASS_DIR" \
      "$ROOT_DIR"/src/java/utils/*.java \
      "$ROOT_DIR"/src/java/model/*.java \
      "$ROOT_DIR"/src/java/dao/*.java \
      "$ROOT_DIR"/src/java/controller/*.java

echo "==> Chuẩn bị cấu trúc WAR"
mkdir -p "$CLASS_DIR/WEB-INF/classes"
mkdir -p "$CLASS_DIR/WEB-INF/lib"

cp -R "$ROOT_DIR/web/"* "$CLASS_DIR/"
cp -R "$CLASS_DIR"/controller "$CLASS_DIR/WEB-INF/classes/"
cp -R "$CLASS_DIR"/dao "$CLASS_DIR/WEB-INF/classes/"
cp -R "$CLASS_DIR"/model "$CLASS_DIR/WEB-INF/classes/"
cp -R "$CLASS_DIR"/utils "$CLASS_DIR/WEB-INF/classes/"

cp "$ROOT_DIR/lib/postgresql-42.7.3.jar" "$CLASS_DIR/WEB-INF/lib/"
cp "$ROOT_DIR/lib/jakarta.servlet-api-6.0.0.jar" "$CLASS_DIR/WEB-INF/lib/"

echo "==> Tạo file WAR"
cd "$CLASS_DIR"
jar -cf "$BUILD_DIR/$APP_NAME.war" .

echo "==> Hoàn tất: $BUILD_DIR/$APP_NAME.war"
