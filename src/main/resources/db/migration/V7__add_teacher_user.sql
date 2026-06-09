-- Adds teacher user: teacher@educationpro.com / Teacher@123
INSERT INTO users (full_name, email, password_hash, role, created_at, updated_at)
VALUES (
  'Demo Teacher',
  'teacher@educationpro.com',
  '$2b$10$kzNMiV6fZPP64.XBWF.EUuqsEezd0OIxYpGIrHpTwkePk1Tp6aBv2',
  'TEACHER',
  NOW(),
  NOW()
)
ON CONFLICT (email) DO NOTHING;
