#!/bin/bash

# Setup database and generate jOOQ code
set -e

echo "🚀 Setting up Subscription Engine database..."

# Start PostgreSQL on port 5440
echo "📦 Starting PostgreSQL container on port 5440..."
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 15

# Run migrations
echo "🔄 Running Flyway migrations..."
./gradlew flywayMigrate

# Generate jOOQ code
echo "⚡ Generating jOOQ code..."
./gradlew :modules:common:generateJooq

echo "✅ Database setup complete!"
echo ""
echo "🔗 Database Connection Info:"
echo "  Host: localhost"
echo "  Port: 5440"
echo "  Database: subscription_engine"
echo "  Username: postgres"
echo "  Password: postgres"
echo ""
echo "📁 Generated jOOQ code location:"
echo "  modules/common/src/main/java/com/subscriptionengine/generated/"
echo ""
echo "🚀 Next steps:"
echo "  - Review generated domain models"
echo "  - Run API: ./gradlew :apps:subscription-api:bootRun"
echo "  - Run Worker: ./gradlew :apps:subscription-worker:bootRun"
echo "  - Optional PgAdmin: docker-compose --profile admin up -d pgadmin"
