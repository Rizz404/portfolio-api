CREATE TABLE users (
  -- * Menggunakan Snowflake ID (64-bit integer)
  id BIGINT PRIMARY KEY,
  nickname VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  email VARCHAR(255) NOT NULL UNIQUE,
  -- * Password nullable karena user bisa login via GitHub (OAuth2)
  password VARCHAR(255),
  -- * Enum Role
  role VARCHAR(50) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
  -- * Enum AuthProvider
  provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL' CHECK (provider IN ('LOCAL', 'GITHUB')),
  -- * Profil Portofolio
  profile_picture VARCHAR(255),
  place_of_birth VARCHAR(255),
  date_of_birth DATE,
  -- * Enum Gender
  gender VARCHAR(50) CHECK (
    gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')
  ),
  phone_number VARCHAR(50),
  bio TEXT,
  address TEXT,
  -- * Audit Trails
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
