-- ==============================================================================
# MarketGrid Commerce — Database Initialization & Least Privilege Provisioning
-- ==============================================================================

-- 1. Create Service Databases
CREATE DATABASE IF NOT EXISTS marketgrid_user_db;
CREATE DATABASE IF NOT EXISTS marketgrid_vendor_db;
CREATE DATABASE IF NOT EXISTS marketgrid_product_db;
CREATE DATABASE IF NOT EXISTS marketgrid_order_db;

-- 2. Create Service-Specific MySQL Users with Least Privilege
-- User Service Database User
CREATE USER IF NOT EXISTS 'user_service_user'@'%' IDENTIFIED BY 'user_db_password_123';
GRANT ALL PRIVILEGES ON marketgrid_user_db.* TO 'user_service_user'@'%';

-- Vendor Service Database User
CREATE USER IF NOT EXISTS 'vendor_service_user'@'%' IDENTIFIED BY 'vendor_db_password_123';
GRANT ALL PRIVILEGES ON marketgrid_vendor_db.* TO 'vendor_service_user'@'%';

-- Product Service Database User
CREATE USER IF NOT EXISTS 'product_service_user'@'%' IDENTIFIED BY 'product_db_password_123';
GRANT ALL PRIVILEGES ON marketgrid_product_db.* TO 'product_service_user'@'%';

-- Order Service Database User
CREATE USER IF NOT EXISTS 'order_service_user'@'%' IDENTIFIED BY 'order_db_password_123';
GRANT ALL PRIVILEGES ON marketgrid_order_db.* TO 'order_service_user'@'%';

FLUSH PRIVILEGES;
