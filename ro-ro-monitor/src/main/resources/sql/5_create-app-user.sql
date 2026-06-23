-- Create dedicated application user (replacing root for app connections)
CREATE USER IF NOT EXISTS 'roro_app'@'%' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON ro_ro_monitor.* TO 'roro_app'@'%';
FLUSH PRIVILEGES;
