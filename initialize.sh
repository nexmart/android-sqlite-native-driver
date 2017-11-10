#!/bin/bash

set -e

echo "Initializing submodules"
echo "========================================================================="
git submodule update -i --recursive
echo ""

cd android-database-sqlcipher
echo "Building OpenSSL"
echo "========================================================================="
./gradlew buildOpenSSL
echo ""
echo "Building Amalgamation"
echo "========================================================================="
./gradlew buildAmalgamation

cd ..

echo ""
echo "Building SQLite Native Driver"
echo "========================================================================="
make
