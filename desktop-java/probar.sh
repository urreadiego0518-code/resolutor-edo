#!/usr/bin/env bash
# Compila el proyecto y ejecuta todas las pruebas unitarias (JUnit 5).
#
#   ./probar.sh            -> compila y corre las pruebas
#   ./probar.sh --rapido   -> solo corre las pruebas (no recompila)
#
# No necesita NetBeans ni Ant: solo el JDK y el jar de JUnit que está en
# lib/test/. Si el jar no existe, el script lo avisa y se detiene.
set -euo pipefail

cd "$(dirname "$0")"

JUNIT="lib/test/junit-platform-console-standalone-1.11.4.jar"
if [ ! -f "$JUNIT" ]; then
    echo "ERROR: falta $JUNIT" >&2
    exit 1
fi

if [ "${1:-}" != "--rapido" ]; then
    echo "== Compilando src =="
    rm -rf build/classes build/test/classes
    mkdir -p build/classes build/test/classes
    find src -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -Xlint:-serial -d build/classes
    echo "== Compilando pruebas =="
    find test -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -cp "build/classes:$JUNIT" -d build/test/classes
fi

echo "== Ejecutando pruebas =="
java -jar "$JUNIT" execute \
    --class-path "build/classes:build/test/classes" \
    --select-package edo \
    --details=tree \
    --disable-ansi-colors
