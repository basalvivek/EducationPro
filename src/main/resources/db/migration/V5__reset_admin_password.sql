-- Resets admin password to Admin@123
-- BCrypt(10) of "Admin@123"
UPDATE users
SET password_hash = '$2a$10$urbzmJjk3roFcVy.VAkR/.W9tbfkmVHfwXTD28OZTooz.6bKyAMPa'
WHERE email = 'admin@educationpro.com' AND role = 'ADMIN';
