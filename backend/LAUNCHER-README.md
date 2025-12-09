# BBMovie Service Launcher

This directory contains scripts to launch multiple microservices at once.

## Available Scripts

### start-services.bat
A batch script to start multiple services simultaneously in separate Command Prompt windows.

#### Usage:
```cmd
start-services.bat [service1] [service2] [service3] ...
```

#### Available Services:
- `ai-service` - AI service with environment variable loading
- `eureka-sever` - Eureka discovery server
- `file-service` - File service
- `auth-service` - Authentication service
- `email-service` - Email service
- `gateway` - API gateway
- `payment-service` - Payment service
- `search-service` - Search service
- `watchlist-quarkus` - Watchlist service (Quarkus framework)
- `rust-ai-context-refinery` - AI context refinement service (Rust with cargo run --release)

#### Examples:
```cmd
# Start all services
start-services.bat ai-service eureka-sever file-service

# Start only eureka and file service
start-services.bat eureka-sever file-service

# Start just the ai service
start-services.bat ai-service
```

### start-services.ps1
A PowerShell script with the same functionality as the batch script, plus additional options.

#### Usage:
```powershell
.\start-services.ps1 -Services "ai-service", "eureka-sever", "file-service"
```

## Notes
- Services will launch in separate windows to prevent output conflicts
- Each service runs its own `run.bat` script from its respective directory
- Log files are saved to the `logs` directory if you modify the scripts to use logging
- Services run independently and can be closed individually

## Adding New Services
To add a new service to the launcher:
1. Create a `run.bat` file in your service directory
2. Add the service name to the `available_services` list in both scripts