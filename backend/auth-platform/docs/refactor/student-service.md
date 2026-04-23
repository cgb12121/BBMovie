# Refactoring Plan: student-service

The `student-service` will handle all business logic related to the BBMovie Student Program.

## Responsibilities
- Processing student verification applications.
- Managing document uploads (PDF/Image) for student proof.
- Integration with the Camunda Workflow Engine for automated/manual review.
- Managing the lifecycle of "Student Status" (e.g., expiry dates).

## Components to Migrate
- **Entity**: `StudentProfile`.
- **Controller**: `StudentProgramController`.
- **Service**: `StudentVerificationService`.
- **Security**: `StudentApplicationSecurity` (SpEL-based logic).

## Database Migration
1. Move the `student_profiles` table to the `student-service` database.
2. **Action Item**: Remove the `@OneToOne` mapping to the `User` entity. Replace it with a `user_id` (UUID) column.
3. **Action Item**: Add indices on `user_id` for fast lookups.

## Technical Challenges
- **Loose Coupling**: When the student service needs to display a user's name or email (e.g., for an admin view), it must make an internal REST call to the `identity-service` instead of performing a DB join.
- **Camunda Callbacks**: Ensure that the external Camunda engine is updated to call the new `student-service` endpoint when a workflow is finalized, rather than the old `auth-service` URL.
