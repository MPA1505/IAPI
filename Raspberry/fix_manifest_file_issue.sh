#!/bin/bash

# Define paths
ORIGINAL_JAR=".\java\unified_project\target\unified_project-1.0-SNAPSHOT.jar"
FIXED_JAR="unified_project-1.0-SNAPSHOT_fixed.jar"

# Step 1: Create a temporary directory and extract JAR
mkdir -p tmp
cd tmp
jar xvf "../$ORIGINAL_JAR"

# Step 2: Remove signature files
rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA

# Step 3: Repackage the JAR
jar cfvm "../$FIXED_JAR" META-INF/MANIFEST.MF .

# Optional: Cleanup the temporary directory
cd ".."
rm -r ".\tmp"
