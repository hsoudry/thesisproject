#!/bin/bash

# Delete the previous output
rm -r output/
rm -r output_counted/

# Compile the Main class
bin/hadoop com.sun.tools.javac.Main TopTenRoutes.java
# javac -classpath $HADOOP_HOME/share/hadoop/common/hadoop-common-2.2.7.jar:$HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-client-core-2.2.7.jar:$HADOOP_HOME/share/hadoop/common/lib/commons-cli-1.2.jar -d classes TopTenRoutes.java

# Create the jar
jar cfm TopTenRoutes.jar MANIFESTROUTES.txt TopTenRoutes*.class
#jar cf TopTenRoutes.jar TopTenRoutes*.class

# Run the jar
bin/hadoop jar TopTenRoutes.jar TopTenRoutes input output '2013-05-06 12:00:00'

# Output the results
cat output/part-r-00000
