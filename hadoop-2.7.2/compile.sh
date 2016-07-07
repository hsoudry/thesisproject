#!/bin/bash

# Delete the previous output
rm -r output/

# Compile the Main class
bin/hadoop com.sun.tools.javac.Main Query1.java

# Create the jar
jar cf Query1.jar Query1*.class

# Run the jar
bin/hadoop jar Query1.jar Query1 input output

# Output the results
cat output/part-r-00000
