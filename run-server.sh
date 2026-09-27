#!/usr/bin/env bash
# Compila o mod, copia para o servidor local e sobe o servidor.
# Uso: ./run-server.sh   (Ctrl+C para parar; rode de novo depois de mudar o código)
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$PROJECT_DIR/../server"
HYTALE_DIR="$HOME/Library/Application Support/Hytale/install/release/package/game/latest"
SERVER_JAR="$HYTALE_DIR/Server/HytaleServer.jar"
ASSETS_ZIP="$HYTALE_DIR/Assets.zip"

if [[ ! -f "$SERVER_JAR" || ! -f "$ASSETS_ZIP" ]]; then
    echo "Erro: não encontrei o Hytale em $HYTALE_DIR" >&2
    exit 1
fi

# Se o Hytale foi atualizado, compila contra a versão nova da API
if ! cmp -s "$SERVER_JAR" "$PROJECT_DIR/libs/HytaleServer.jar"; then
    echo "==> Hytale atualizado: copiando HytaleServer.jar novo para libs/"
    cp "$SERVER_JAR" "$PROJECT_DIR/libs/HytaleServer.jar"
fi

echo "==> Compilando o mod"
(cd "$PROJECT_DIR" && ./gradlew build -q)

echo "==> Copiando o mod para o servidor"
mkdir -p "$SERVER_DIR/mods"
rm -f "$SERVER_DIR/mods"/LOASystems-*.jar
cp "$PROJECT_DIR"/build/libs/LOASystems-*.jar "$SERVER_DIR/mods/"

echo "==> Iniciando o servidor (Ctrl+C para parar)"
cd "$SERVER_DIR"
exec java -jar "$SERVER_JAR" --assets "$ASSETS_ZIP"
