#!/bin/bash

# Delete the previous output
rm -r output/
rm -r output_counted/

# Compile the Main class
bin/hadoop com.sun.tools.javac.Main TopTenRoutes.java

# Create the jar
jar cf TopTenRoutes.jar TopTenRoutes*.class

# Run the jar
bin/hadoop jar TopTenRoutes.jar TopTenRoutes input output '2013-05-06 12:00:00'

# Output the results
cat output/part-r-00000
