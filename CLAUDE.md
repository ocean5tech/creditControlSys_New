# Credit Control System - Development Guide

This file helps Claude Code (and future AI instances) understand the codebase architecture and common operations.

## CRITICAL RULES - MUST FOLLOW

**These rules are MANDATORY and cannot be violated under any circumstances:**

### Rule 1: Resource Usage - Documentation First
When using system resources (ports, volumes, networks) or infrastructure software:
- **MUST** document the resource usage plan in design documents BEFORE development or execution
- **MUST** obtain explicit client approval before using any resource
- **MUST** use ONLY resources explicitly defined in design documentation
- **MUST** check if resources (ports, volumes, networks) are available
- **MUST** immediately report to client if any resource is occupied or conflicts exist
- **MUST** wait for client confirmation before proceeding with changes
- **MUST** update documentation based on client's decision
- **MUST** update design documentation FIRST before implementing any changes
- **MUST** follow the documented plan exactly - absolutely forbidden to use undocumented resources

**Examples of Resources Requiring Pre-Approval**:
- Network ports (8080-8086, 5432, 6379, etc.)
- Container volumes and networks
- External software packages or dependencies
- Cloud resources or services
- Database schemas and tables

**Workflow**:
1. Create/update documentation with resource requirements
2. Check resource availability
3. Report conflicts immediately if any
4. Wait for client confirmation
5. Update documentation per client instructions
6. Execute according to documentation

### Rule 2: Containerization - Podman Only
All development and deployment MUST use Podman:
- **MUST** use Podman v4.9.3 (not Docker) for all container operations
- **MUST** obtain explicit client approval before installing ANY software to host system
- **MUST** run all build and runtime processes inside containers when possible
- **MUST NOT** install software, packages, or dependencies on host without permission

**If Software Cannot Run in Podman**:
1. Document why containerization is not possible
2. Specify exact software name, version, and purpose
3. Request client approval for host installation
4. Wait for confirmation before installing
5. Document the exception in design documentation

**Host Software Exceptions** (Pre-Approved):
- Podman v4.9.3
- Git 2.x
- Text editors (vim, nano, VS Code, IntelliJ IDEA)
- curl/wget for testing

Any other software requires explicit client approval.

### Rule 3: Design Document Authority
When encountering any situation inconsistent with design documentation:
- **MUST** only propose suggestions, not make decisions
- **MUST** wait for client confirmation
- **MUST** update documentation first before starting development
- Client has final authority on all design decisions

**Never deviate from documented design without explicit client approval and documentation update.**

---

**Note**: Complete development standards and rules are documented in `docs/claudecode/develop_rules.md`. The above critical rules are also reflected there with additional examples and context.

---

## Project Overview

**Credit Control System** is an incomplete microservices-based credit management platform being planned for Azure migration. The system manages customer credit limits, risk assessment, payments, reporting, and notifications.

**Current Status**: 70% deployment complete. All services build successfully but require database configuration resolution and port conflict fixes.

## Architecture

### Microservices (Spring Boot 3.2.0 + Java 17)

```
┌─────────────────────────────────────────────────────────┐
│                    Nginx API Gateway                     │
│                    (Port 8080)                          │
└──────────────┬──────────────────────────────────────────┘
               │
       ┌───────┴────────┬─────────┬─────────┬─────────┬──────────┐
       │                │         │         │         │          │
┌──────▼──────┐  ┌─────▼─────┐ ┌─▼────┐ ┌──▼──┐ ┌────▼────┐ ┌───▼────┐
│  Customer   │  │   Credit  │ │ Risk │ │ Pay │ │ Report  │ │ Notify │
│  Service    │  │  Service  │ │ Svc  │ │ Svc │ │ Service │ │ Svc    │
│  (8081)     │  │  (8082)   │ │(8083)│ │(8084│ │ (8085)  │ │ (8086) │
└──────┬──────┘  └─────┬─────┘ └─┬────┘ └──┬──┘ └────┬────┘ └───┬────┘
       │                │         │         │         │          │
       └────────────────┴─────────┴─────────┴─────────┴──────────┘
                                  │          │
                    ┌─────────────┴──┐  ┌───▼──────┐
                    │  PostgreSQL    │  │  Redis   │
                    │ 35.77.54.203   │  │  (6379)  │
                    │     (5432)     │  └──────────┘
                    └────────────────┘
```

### Service Responsibilities

- **customer-service** (`backend/customer-service/`): Customer CRUD operations, credit profiles
- **credit-service** (`backend/credit-service/`): Credit limit management, approval workflows
- **risk-service** (`backend/risk-service/`): Risk assessment and scoring
- **payment-service** (`backend/payment-service/`): Payment processing and tracking
- **report-service** (`backend/report-service/`): Report generation and analytics
- **notification-service** (`backend/notification-service/`): Email/SMS notifications

### Infrastructure Components

- **API Gateway**: Nginx (`backend/infrastructure/nginx/`) - Routes requests to microservices
- **Cache**: Redis 7 Alpine - Session and data caching
- **Database**: PostgreSQL 13 (remote at 35.77.54.203:5432)
  - Database: `creditcontrol`
  - User: `creditapp`
  - Password: `secure123`
  - Superuser: `postgres` / `postgres`
- **Network**: Podman network `creditcontrol-network` for inter-service communication

### Deployment Strategy

**Native Podman Only** - No host dependencies except Podman v4.9.3. All compilation happens in containers.

- Maven builds use `maven:3.9-eclipse-temurin-17-alpine` container
- Runtime uses `openjdk:17-jdk-slim` base image
- Maven dependencies cached in `maven-repo` volume

## Common Commands

### Build All Services
```bash
./podman-build.sh
```
This script:
1. Pulls Maven and OpenJDK images if needed
2. Compiles each service using Maven container (skips tests)
3. Builds Docker images for each service (~538MB each)
4. Tags images as `creditcontrol/<service>:latest`

### Start All Services
```bash
./podman-start.sh
```
Starts (in order):
1. Redis cache
2. All 6 microservices (with database env vars)
3. Nginx API Gateway
4. Waits 30s for startup
5. Shows container status and health check URLs

### Stop All Services
```bash
./podman-stop.sh
```
Stops and removes all containers (preserves images and volumes).

### View Logs
```bash
podman logs -f <service-name>
# Example: podman logs -f credit-service
```

### Health Checks
```bash
# Individual services
curl http://localhost:8081/actuator/health  # Customer
curl http://localhost:8082/actuator/health  # Credit
curl http://localhost:8083/actuator/health  # Risk
curl http://localhost:8084/actuator/health  # Payment
curl http://localhost:8085/actuator/health  # Report
curl http://localhost:8086/actuator/health  # Notification

# API Gateway
curl http://localhost:8080/health
```

### Rebuild Single Service
```bash
cd backend/<service-name>
podman run --rm -v .:/app:Z -v maven-repo:/root/.m2:Z -w /app \
  maven:3.9-eclipse-temurin-17-alpine mvn clean package -DskipTests
podman build -t creditcontrol/<service-name>:latest .
```

## Known Issues and Solutions

### Issue 1: Port 8081 Already In Use
**Symptom**: `rootlessport listen tcp 0.0.0.0:8081: bind: address already in use`

**Solution**:
```bash
# Find process using port
lsof -i :8081
# or
ss -tlnp | grep 8081

# Kill the process if safe
kill <PID>

# Or modify podman-start.sh to use different port mapping
-p 8091:8080  # Maps host 8091 to container 8080
```

### Issue 2: Database Authentication Failed
**Symptom**: `FATAL: password authentication failed for user "creditapp"`

**Solution**: Ensure `podman-start.sh` uses correct password:
```bash
DB_PASSWORD="${DB_PASSWORD:-secure123}"  # NOT "creditapp"
```

Password reference: https://github.com/ocean5tech/creditControlSys_Legacy/tree/main/conf

### Issue 3: Nginx Gateway Fails - "host not found in upstream"
**Symptom**: `nginx: [emerg] host not found in upstream "customer-service:8080"`

**Root Cause**: Nginx starts before microservices are ready.

**Solution**:
- Increase wait time in `podman-start.sh` (line 158)
- Or start Nginx separately after verifying services are up
- Check all services are running: `podman ps`

### Issue 4: Maven Build Fails - "manifest unknown"
**Symptom**: Error pulling `maven:3.9-openjdk-17-slim`

**Solution**: Use `maven:3.9-eclipse-temurin-17-alpine` instead (already fixed in scripts).

### Issue 5: JAR File Not Found During Docker Build
**Symptom**: `no items matching glob *-service-*.jar`

**Root Cause**: Either:
1. `.dockerignore` excludes `target/` directory
2. JAR filename mismatch in Dockerfile COPY command

**Solution**:
```bash
# Fix .dockerignore
sed -i 's/^target\//# target\//g' .dockerignore
echo "!target/*.jar" >> .dockerignore

# Fix Dockerfile COPY command to match actual JAR name
COPY target/<correct-service-name>-*.jar app.jar
```

## Project Structure

```
creditControlSys_New/
├── backend/
│   ├── customer-service/       # Customer management
│   ├── credit-service/         # Credit limits
│   ├── risk-service/           # Risk assessment
│   ├── payment-service/        # Payments
│   ├── report-service/         # Reporting
│   ├── notification-service/   # Notifications
│   └── infrastructure/
│       └── nginx/              # API Gateway config
├── podman-build.sh             # Build automation
├── podman-start.sh             # Start automation
├── podman-stop.sh              # Stop automation
├── progress.md                 # Deployment guide
├── DEPLOYMENT_STATUS.md        # Current status (70% complete)
└── docs/
    ├── azure-migration/        # Azure migration plans
    └── architecture/           # Architecture diagrams
```

## Development Workflow

1. **Make code changes** in `backend/<service>/src/`
2. **Rebuild service**: `./podman-build.sh` or rebuild single service
3. **Restart services**: `./podman-stop.sh && ./podman-start.sh`
4. **Check logs**: `podman logs -f <service-name>`
5. **Test endpoint**: `curl http://localhost:8080/api/v1/<resource>`

## Configuration Files

### Service Configuration
- `backend/<service>/src/main/resources/application.yml` - Main config
- `backend/<service>/src/main/resources/application-development.yml` - Dev profile
- Database connection uses environment variables (set in `podman-start.sh`)

### Docker Configuration
- `backend/<service>/Dockerfile` - Multi-stage build (Maven → JRE)
- `backend/<service>/.dockerignore` - Excludes IDE/test files, allows JARs

### Infrastructure Configuration
- `backend/infrastructure/nginx/nginx.conf` - API Gateway routing

## Database Schema

Schema is managed by individual services using JPA/Hibernate auto-DDL. No centralized schema files exist yet.

**Expected tables** (based on service names):
- `customers` - Customer master data
- `credit_limits` - Credit limit configurations
- `risk_assessments` - Risk scores and factors
- `payments` - Payment transactions
- `reports` - Generated reports metadata
- `notifications` - Notification logs

## Testing

**Current Status**: Tests are skipped during build (`-DskipTests`)

To run tests manually:
```bash
cd backend/<service>
podman run --rm -v .:/app:Z -v maven-repo:/root/.m2:Z -w /app \
  maven:3.9-eclipse-temurin-17-alpine mvn test
```

## Frontend

**Status**: Not yet implemented. Planned as React application.

## Security Notes

- Database credentials are hardcoded in `podman-start.sh` (development only)
- Redis has no authentication (development only)
- Services communicate over internal Podman network
- API Gateway exposes services externally on ports 8080-8086

## Future Work

See `DEPLOYMENT_STATUS.md` for detailed task list:
- Resolve port conflicts
- Initialize database schema
- Create frontend application
- Implement proper secret management
- Add health check retry logic to Nginx
- Set up CodeQL static analysis

## Azure Migration Context

This system is being evaluated for Azure migration. See `docs/azure-migration/` for:
- Azure-migration-plan.md - Migration strategy
- Cost analysis and service mapping
- Infrastructure-as-Code templates (planned)

Reference legacy system: https://github.com/ocean5tech/creditControlSys_Legacy

---

**Last Updated**: 2025-10-09
**Deployment Status**: 70% complete (all services build, runtime issues remain)
**Required Host Software**: Podman v4.9.3 only
