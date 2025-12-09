# BBMovie Bash Scripts Migration

## Overview
This project now includes bash script equivalents for all Windows batch files to support Unix/Linux/macOS systems while maintaining backward compatibility with Windows.

## Files Added
- `launcher.sh` - Main launcher script equivalent to `launcher.bat`
- `make-executable.sh` - Utility to set execute permissions on Unix systems
- `*/run.sh` - Individual service run scripts in each microservice directory

## Usage on Unix/Linux/macOS Systems

### 1. Make the scripts executable:
```bash
chmod +x launcher.sh
chmod +x */run.sh
# Or run the utility script:
./make-executable.sh
```

### 2. Launch the application:
```bash
./launcher.sh
```

### 3. To run individual services:
```bash
cd service-directory
./run.sh
```

## Backward Compatibility
All original `.bat` files remain unchanged for Windows users:
- `launcher.bat` in root directory
- `run.bat` files in each microservice directory

## Features Preserved
- Environment variable loading from `.env` files
- Colored output and interactive menus
- Service startup sequences (Eureka first)
- Process management and cleanup
- Support for SPRING, RUST, and QUARKUS services