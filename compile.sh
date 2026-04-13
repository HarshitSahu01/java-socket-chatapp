#!/bin/bash
echo "===== Compiling Chat Application ====="

# Clean old class files
rm -rf out
mkdir -p out

# Compile all Java files
javac -d out \
    common/*.java \
    server/observer/*.java \
    server/command/*.java \
    server/*.java \
    client/*.java

if [ $? -ne 0 ]; then
    echo "COMPILATION FAILED"
    exit 1
fi

echo "===== Compilation Successful ====="
echo ""
echo "To run the server:"
echo "  java -cp out server.ServerDriver"
echo ""
echo "To run a client:"
echo "  java -cp out client.ClientDriver"