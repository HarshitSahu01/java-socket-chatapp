#!/bin/bash
echo "===== Compiling Chat Application ====="

rm -rf out
mkdir -p out

javac -d out common/*.java server/*.java client/*.java

if [ $? -ne 0 ]; then
    echo "COMPILATION FAILED"
    exit 1
fi

echo "===== Compilation Successful ====="
echo ""
echo "To run the server:  java -cp out server.ServerDriver"
echo "To run a client:    java -cp out client.ClientDriver"