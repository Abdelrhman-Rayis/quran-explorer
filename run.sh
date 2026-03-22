#!/bin/bash
set -e
cd "$(dirname "$0")"

JAR="../jqurantree-1.0.0-bin/jqurantree-1.0.0.jar"

echo "Compiling..."
mkdir -p out
javac -cp "$JAR" -d out src/QuranServer.java

echo "Starting server..."
java -cp "out:$JAR" QuranServer
