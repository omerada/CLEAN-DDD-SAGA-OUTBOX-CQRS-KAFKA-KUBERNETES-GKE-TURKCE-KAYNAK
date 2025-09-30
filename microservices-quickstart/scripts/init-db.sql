-- Database initialization script
-- This creates separate databases for each microservice

-- Create databases
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payment_db;

-- Grant permissions (PostgreSQL automatically creates the user from environment variables)
GRANT ALL PRIVILEGES ON DATABASE order_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO admin;