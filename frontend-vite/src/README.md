# Frontend Source Code (Migrated)

This directory contains the migrated frontend source code from `src_old` to `src` with improvements to the API structure and cleanup of deprecated endpoints.

## Migration Summary

- All components, pages, services, hooks, styles, and utility files have been moved from `src_old` to `src`
- Directory structure has been preserved to maintain existing import paths
- API endpoints have been standardized with consistent versioning (`/api/v1/` prefix)
- Deprecated and inconsistent API endpoints have been updated

## API Endpoint Changes

### Authentication Service (`services/authService.ts`)
- `/api/auth/login` → `/api/v1/auth/login`
- `/api/auth/v2/logout` → `/api/v1/auth/logout`
- `/api/auth/v2/access-token` → `/api/v1/auth/refresh-token`
- `/api/auth/abac/new-access-token` → `/api/v1/auth/abac/access-token`
- `/api/auth/user-agent` → `/api/v1/auth/user-agent`
- `/api/device/v1/sessions/all` → `/api/v1/device/sessions`
- `/api/device/v1/sessions/revoke` → `/api/v1/device/sessions/revoke`

### File Service (`services/fileService.ts`)
- `/api/file/upload/v1` → `/api/v1/files/upload`
- `/api/file/upload/v2` → `/api/v1/files/upload/stream`
- `/api/file/upload/test` → `/api/v1/files/upload/test`

### Watchlist Service (`services/watchlistService.ts`)
- `/api/watchlist/collections` → `/api/v1/watchlist/collections`
- All watchlist endpoints updated to use `/api/v1/watchlist/`

### Payment Service (`services/paymentService.ts`)
- `/api/v1/subscription/plans` → `/api/v1/subscriptions/plans`
- `/api/v1/subscription/` → `/api/v1/subscriptions/quote`
- `/api/payment/initiate` → `/api/v1/subscriptions/initiate`
- All payment endpoints updated to use `/api/v1/subscriptions/`

### API Wrapper (`services/apiWrapper.ts`)
- `/api/movies` → `/api/v1/movies`
- `/api/movies/:id` → `/api/v1/movies/:id`
- `/api/categories` → `/api/v1/categories`
- `/api/categories/:id/movies` → `/api/v1/categories/:id/movies`
- `/api/search/similar-search` → `/api/v1/search/similar`
- `/api/movies/:id/similar` → `/api/v1/movies/:id/similar`

## File Structure
```
src/
├── assets/           # Static assets
├── components/       # Reusable UI components
│   ├── ai/          # AI-related components
│   ├── security/    # Security-related components
│   └── ui/          # UI library components
├── hooks/            # Custom React hooks
├── lib/              # Utility libraries
├── pages/            # Page components
├── redux/            # Redux store and state management
├── routes/           # Application routing
├── services/         # API services and business logic
├── styles/           # Global styles
├── types/            # TypeScript type definitions
├── utils/            # Utility functions
├── App.tsx           # Main application component
└── main.tsx          # Application entry point
```

## Improvements Made

1. Standardized API endpoint versioning using `/api/v1/` prefix
2. Fixed deprecated API endpoints
3. Improved error handling consistency
4. Cleaned up redundant API calls
5. Updated service base URL mappings to include new paths
6. Maintained backward compatibility where possible

## Notes

This migration cleaned up the messy API structure and deprecated endpoints mentioned in the original codebase, standardizing the API calls with proper versioning.