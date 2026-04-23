# Refactoring Plan: identity-service

The `identity-service` will serve as the Source of Truth for user accounts, profiles, and roles.

## Responsibilities
- User Registration and Onboarding.
- Password Management (Hashing, Resets, Changes).
- User Profile Management (CRUD, Picture updates).
- Email Verification flows.
- Role/Permission management.
- Source of truth for `user_id`, `email`, and `displayed_username`.

## Components to Migrate
- **Entity**: `User`.
- **Controller**: Portions of `AuthController` (`/register`, `/change-password`, `/forgot-password`, `/reset-password`, `/user-agent`).
- **Service**: `RegistrationService`, `PasswordService`, `UserService`.
- **NATS Producers**: `EmailEventProducer`, `UserEventProducer`.

## New Internal API (for `sso-service`)
The `identity-service` must expose a secure internal endpoint for the SSO gateway:
- **`POST /internal/users/verify-credentials`**
  - **Input**: `{ email, password }`
  - **Output**: `{ valid: true, userId, email, roles, isEnabled, isMfaEnabled }`
  - **Logic**: Performs password hash comparison using the chosen algorithm (Argon2/BCrypt).

## Database Migration
1. Move the `users` table to the `identity-service` database.
2. **Action Item**: The `totp_secret` column should be migrated to the `mfa-service` database to isolate sensitive 2nd-factor secrets.
3. **Action Item**: The `student_profile_id` column will be removed, as the `student-service` will now link back to the `User` via `user_id`.

## Technical Challenges
- **Password Encoder Migration**: Ensure the `DelegatingPasswordEncoder` logic is preserved to support the multiple hashing algorithms currently in use.
- **Transactional Integrity**: Registration now involves saving a user and then (asynchronously) triggering an email. This remains an async NATS-based flow.
