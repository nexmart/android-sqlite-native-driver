#!/bin/bash

set -e

echo "Initializing submodules"
echo "========================================================================="
git submodule update -i --recursive
echo ""

echo "Building OpenSSL"
echo "========================================================================="
./gradlew buildOpenSSL
echo ""
echo "Building SQLCipher"
echo "========================================================================="
./gradlew buildSQLCipher

echo ""
echo "Building SQLite Native Driver"
echo "========================================================================="
make
