#!/bin/bash

# Enable debugging
set -euo pipefail
trap 'echo "Error occurred at line $LINENO"; exit 1;' ERR

# Logging start
echo "Starting the requirements installation script..."

# Check if running as root or using sudo
if [ "$EUID" -ne 0 ]; then
  echo "Please run this script as root or use sudo."
  exit 1
fi

# Update package lists
echo "Updating package lists..."
apt-get update -y

# Install core utilities
echo "Installing core utilities: awk, coreutils, gawk..."
apt-get install -y awk gawk coreutils

# Install additional utilities (if needed)
echo "Installing additional tools: curl, wget..."
apt-get install -y curl wget

# Verify installations
echo "Verifying installed tools..."
if ! command -v awk &> /dev/null; then
  echo "Error: awk is not installed."
  exit 1
fi

if ! command -v split &> /dev/null; then
  echo "Error: split (coreutils) is not installed."
  exit 1
fi

if ! command -v curl &> /dev/null; then
  echo "Error: curl is not installed."
  exit 1
fi

if ! command -v wget &> /dev/null; then
  echo "Error: wget is not installed."
  exit 1
fi

# Check necessary directories
echo "Ensuring necessary directory structure..."
REQUIRED_DIR="./Raspberry/datasets/shell_datasets_20hz_1_robot_1_minute"
if [ ! -d "$REQUIRED_DIR" ]; then
  mkdir -p "$REQUIRED_DIR"
  echo "Created directory: $REQUIRED_DIR"
else
  echo "Directory already exists: $REQUIRED_DIR"
fi

# Completion message
echo "All requirements have been successfully installed and configured."
