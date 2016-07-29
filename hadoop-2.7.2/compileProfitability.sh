#!/bin/bash

# Delete the previous output
rm -r output/
rm -r output_median/
rm -r output_emptytaxis/
rm -r output_totalemptytaxis/
rm -r output_merge/

# Compile the Main class
bin/hadoop com.sun.tools.javac.Main TopTenProfitability.java

# Create the jar
jar cf TopTenProfitability.jar TopTenProfitability*.class

# Run the jar
bin/hadoop jar TopTenProfitability.jar TopTenProfitability input output "2013-01-01 00:30:00"

# Output the results
cat output/part-r-00000
