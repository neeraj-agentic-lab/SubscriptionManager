# Quick Setup Guide

## Prerequisites
- Java 21
- Docker & Docker Compose
- Gradle

## Getting Started

1. **Start the database:**
   ```bash
   docker-compose up -d postgres
   ```

2. **Run migrations:**
   ```bash
   ./gradlew flywayMigrate
   ```

3. **Generate jOOQ code:**
   ```bash
   ./gradlew generateJooq
   ```

4. **Build the project:**
   ```bash
   ./gradlew build
   ```

## Database Access
- **PostgreSQL**: `localhost:5432`
- **Database**: `subscription_engine`
- **User/Password**: `postgres/postgres`

## Optional: PgAdmin
```bash
docker-compose --profile admin up -d pgadmin
```
Access at: http://localhost:8080 (admin@subscription-engine.com / admin)

## Next Steps
- Run API: `./gradlew :apps:subscription-api:bootRun`
- Run Worker: `./gradlew :apps:subscription-worker:bootRun`
