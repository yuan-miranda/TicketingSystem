#!/bin/bash
echo "==================================================="
echo "   Philippine Express Bus Ticketing System"
echo "   CC 104 - Java Programming Final Project"
echo "==================================================="
echo ""

mkdir -p out

echo "Compiling source files..."
javac -d out src/ticketing/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Compilation failed. Make sure Java JDK is installed."
    exit 1
fi

echo "Compilation successful!"
echo ""
echo "Launching ticketing system..."
java -cp out ticketing.TicketingSystemGUI
