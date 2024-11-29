#!/bin/bash

# Define paths
ORIGINAL_JAR=".\java\convert_data\target\convert_data-1.0-SNAPSHOT.jar"
FIXED_JAR="convert_data_fixed.jar"

# Step 1: Create a temporary directory and extract JAR
mkdir -p tmp
cd tmp
jar xvf "../$ORIGINAL_JAR"

# Step 2: Remove signature files
rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA

# Step 3: Repackage the JAR
jar cfvm "../$FIXED_JAR" META-INF/MANIFEST.MF .

# Optional: Cleanup the temporary directory
rm -rf tmp
