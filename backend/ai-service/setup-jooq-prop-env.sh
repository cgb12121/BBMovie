#!/bin/bash

echo "=== JOOQ Codegen Environment Setup ==="

if [ ! -f .env ]; then
    echo "Creating .env from template..."
    cp .env.template .env
    echo " Created .env file"
    echo ""
    echo "  IMPORTANT: Edit .env with your actual database credentials"
    echo "    nano .env"
else
    echo " .env file already exists"
fi

echo ""
echo "=== Loading environment variables ==="
export $(cat .env | grep -v '^#' | xargs)

echo "DB_DRIVER: $DB_DRIVER"
echo "DB_URL: $DB_URL"
echo "DB_USERNAME: $DB_USERNAME"
echo "DB_PASSWORD: ******* (hidden)"
echo "DB_SCHEMA: $DB_SCHEMA"

echo ""
echo "=== Running JOOQ Codegen ==="
mvn clean generate-sources
