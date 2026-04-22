# BBMovie - Feature Suggestions

> Generated: April 14, 2026  
> Based on current microservices architecture analysis

---

## 🎬 User-Facing Features

### Content & Engagement

#### 1. Movie Reviews & Ratings
- **Description:** Allow users to rate (1-10 or 5-star) and write reviews for movies
- **Features:**
  - Star/rating system with half-star support
  - Rich text reviews with spoiler tags
  - "Helpful / Not Helpful" voting on reviews
  - Sort reviews by: Most Recent, Highest Rated, Most Helpful, Spoiler-Free
  - Review moderation queue for reported content
- **Why:** Core engagement feature missing from current system; gateway already has `/api/reviews/**` routes defined but no service exists
- **Suggested Service:** New `review-service` (Spring Boot + MySQL + Elasticsearch for full-text search)
- **Priority:** 🔴 High

#### 2. Comments & Discussions
- **Description:** Per-movie comment threads with real-time updates
- **Features:**
  - Nested/threaded comments (up to 3 levels)
  - Spoiler tags with blur overlay (integrates with "Spoiler Shield" concept from features.md)
  - Upvote/downvote system
  - Pin important comments (admin/moderator only)
  - @mentions with notifications
- **Why:** Drives community engagement and return visits
- **Suggested Service:** New `discussion-service` (Spring Boot + Redis for real-time + MySQL)
- **Priority:** 🟡 Medium

#### 3. Social Sharing & Watch Parties
- **Description:** Share content externally or watch together in real-time
- **Features:**
  - Generate shareable links with preview images (OG tags)
  - "Watch Together" rooms with synchronized playback
  - Real-time chat during watch parties
  - Room privacy: Public, Friends Only, Invite Only
  - Host controls: Play/Pause, Skip, Kick
- **Why:** Social features drive user acquisition and retention
- **Suggested Service:** Extend `ai-service` (already has WebSocket/SSE infrastructure) or new `social-service`
- **Priority:** 🟡 Medium

#### 4. Achievements & Badges (Gamification)
- **Description:** Reward users for platform engagement
- **Features:**
  - Watch milestones: "First Movie", "Binge Watcher (5 movies in a day)", "100 Club"
  - Genre badges: "Horror Fan", "Sci-Fi Explorer", "Documentary Buff"
  - Review achievements: "First Review", "Top Crit (10 helpful reviews)"
  - Social badges: "Party Host", "Social Butterfly (5 referrals)"
  - Display badges on user profile
  - Optional: Unlock perks (early access, exclusive content)
- **Why:** Increases engagement, watch time, and platform loyalty
- **Suggested Service:** New `gamification-service` or extend `user-service`
- **Priority:** 🟢 Low (but high retention impact)

#### 5. User Profiles
- **Description:** Customizable public profiles showcasing user activity
- **Features:**
  - Profile photo, bio, favorite genres
  - Public watchlists (integrates with `watchlist-quarkus`)
  - Activity feed: Recent watches, reviews, ratings
  - Privacy controls: Public / Friends Only / Private
  - Profile URL: `bbmovie.com/u/username`
  - Optional: Link social media accounts
- **Why:** Enables social features and community building
- **Suggested Service:** New `user-service` (gateway has `/api/users/**` routes but service doesn't exist)
- **Priority:** 🔴 High (foundation for other social features)

#### 6. Notifications System
- **Description:** Multi-channel notifications for user engagement
- **Features:**
  - **Channels:** In-app, Email, Push (Web/Mobile), SMS (optional)
  - **Triggers:**
    - New movie in favorite genre
    - Movie price change / on sale
    - Subscription expiring (integrates with existing `email-service`)
    - Reply to your comment/review
    - Watch party invitation
    - New episode of followed series
  - **Preferences:** User-configurable notification settings per type
  - **Digest:** Daily/weekly email digest option
- **Why:** Drives re-engagement and reduces churn
- **Suggested Service:** Extend `email-service` to become `notification-service` (NATS event-driven)
- **Priority:** 🔴 High

---

### Viewing Experience

#### 7. Watch Parties
- **Description:** Real-time synchronized group viewing
- **Features:**
  - Synchronized play/pause/seek across all participants
  - Built-in voice/video chat (WebRTC)
  - Reaction emojis (live reactions during playback)
  - "Catch up" feature for late joiners
  - Recording option (with host permission)
- **Why:** Social viewing is a key differentiator for streaming platforms
- **Suggested Service:** New `watchparty-service` + WebSocket server
- **Priority:** 🟡 Medium

#### 8. Offline Downloads
- **Description:** Download content for offline viewing (premium subscribers)
- **Features:**
  - DRM-protected downloads (Widevine/FairPlay)
  - Expiring downloads (48hr after first play, or subscription active)
  - Download quality selection (SD for mobile storage)
  - Download progress tracking
  - Limit on concurrent downloads (e.g., 10 movies max)
- **Why:** Essential for mobile users and emerging markets with poor connectivity
- **Suggested Service:** Extend `media-streaming-service` with download token generation
- **Priority:** 🟡 Medium

#### 9. Parental Controls
- **Description:** Content filtering and restrictions for family accounts
- **Features:**
  - Content rating filtering (G, PG, PG-13, R, NC-17)
  - PIN-protected restrictions for mature content
  - Kids profile mode (auto-filtered content)
  - Watch history monitoring for parent accounts
  - Time limits (e.g., max 2hrs/day for kids)
- **Why:** Expands market to families; compliance requirement in some regions
- **Suggested Service:** Extend `auth-service` (profile-level settings) + `media-service` (rating metadata)
- **Priority:** 🟡 Medium

#### 10. Multiple Audio/Subtitle Tracks
- **Description:** Per-user language and accessibility preferences
- **Features:**
  - Multiple audio tracks (original, dubbed languages)
  - Subtitle tracks in multiple languages
  - Per-user default language preferences
  - Auto-select preferred subtitle track on play
  - SDH (Subtitles for Deaf/Hard of Hearing) support
  - Custom subtitle styling (size, color, background)
- **Why:** Critical for international markets and accessibility
- **Suggested Service:** Extend `transcode-worker` (multi-track HLS) + `media-streaming-service`
- **Priority:** 🔴 High (if targeting international audience)

#### 11. "Continue Watching" Carousel
- **Description:** Homepage section showing movies/shows in progress
- **Features:**
  - Thumbnail with progress bar overlay
  - "Resume" button jumps to last position
  - "Remove" option to hide from list
  - Sort by most recently watched
  - Integrates with existing `watch-history` service
- **Why:** Reduces friction for users returning to content
- **Suggested Service:** Extend `homepage-recommendations` to consume `watch-history` data
- **Priority:** 🔴 High (quick win)

#### 12. Smart Playback
- **Description:** Intelligent playback controls for better UX
- **Features:**
  - Auto-play next episode (for series)
  - "Skip Intro" button (detected via heatmap data from `movie-analytics-service`)
  - "Skip Recap" button
  - "Next Episode" countdown (10 seconds)
  - Playback speed control (0.5x, 1x, 1.25x, 1.5x, 2x)
- **Why:** Industry standard feature; improves binge-watching experience
- **Suggested Service:** Extend `media-streaming-service` + frontend integration
- **Priority:** 🔴 High (quick win)

---

### Search & Discovery

#### 13. Voice-Activated Search
- **Description:** Search movies/shows using voice commands
- **Features:**
  - Voice input via Web Speech API (browser) or mobile SDK
  - Natural language queries: "Show me action movies from 2024 with Tom Cruise"
  - Intent parsing: genre, actor, year, mood-based
  - Integrates with existing `search-service` (Elasticsearch)
  - Voice search history
- **Why:** Listed in your features.md but not yet implemented
- **Suggested Service:** Extend `search-service` with voice processing layer
- **Priority:** 🟡 Medium

#### 14. Advanced Filters
- **Description:** Granular search filtering options
- **Features:**
  - Multi-select genres (AND/OR logic)
  - IMDb/Rating range filter
  - Release year range
  - Runtime/duration filter
  - Language filter
  - Content rating filter
  - Availability filter (Free, Subscription, Rental, Purchase)
  - Save filter presets
- **Why:** Power users want precise control over search results
- **Suggested Service:** Extend `search-service` (already has Elasticsearch query params)
- **Priority:** 🟡 Medium

#### 15. "Because You Watched..." Recommendations
- **Description:** Explicit recommendation triggers based on recent viewing
- **Features:**
  - Display after finishing a movie/episode
  - "Because you watched [Movie Title]" carousel
  - Similar movies by: Director, Cast, Genre, Themes
  - Integrates with `personalization-recommendation` (Qdrant vector similarity)
  - "Not Interested" feedback button to improve algorithm
- **Why:** Increases content discovery and watch time
- **Suggested Service:** Extend `personalization-recommendation` with trigger-based API
- **Priority:** 🔴 High

#### 16. Curated Collections (Editorial)
- **Description:** Admin-created themed movie collections
- **Features:**
  - Themed collections: "Oscar Winners 2025", "Best of K-Drama", "Hidden Gems"
  - Featured on homepage with custom banner art
  - Seasonal/holiday collections (auto-rotate)
  - Collection descriptions and curator notes
  - Shareable collection links
- **Why:** Human curation complements algorithmic recommendations
- **Suggested Service:** Extend `homepage-recommendations` with admin collection management
- **Priority:** 🟡 Medium

---

## 👨‍💼 Admin Features

### Content Management

#### 17. Content Moderation Dashboard
- **Description:** Central hub for moderating user-generated content
- **Features:**
  - Review reported reviews/comments with reason categories
  - Bulk actions: Approve, Remove, Ban User
  - User ban/suspend with reason and duration
  - Moderation queue with priority scoring
  - Moderation audit log
  - Auto-moderation rules (profanity filter, spam detection)
- **Why:** Essential for maintaining platform health as user base grows
- **Suggested Service:** Admin panel within `review-service` / `discussion-service`
- **Priority:** 🔴 High (before launching reviews/comments)

#### 18. Bulk Import/Export
- **Description:** Import movie catalogs in bulk from external sources
- **Features:**
  - CSV/JSON template-based import
  - TMDB API integration for automatic metadata population
  - IMDb dataset import (for licensed users)
  - Import validation: Duplicate detection, required fields
  - Import progress tracking and error reporting
  - Export movie catalog for backups/analytics
  - Schedule recurring imports (for data sync)
- **Why:** Manual movie creation is not scalable for large catalogs
- **Suggested Service:** Extend `media-service` with import/export module
- **Priority:** 🔴 High

#### 19. A/B Testing Framework
- **Description:** Test different content presentations to optimize engagement
- **Features:**
  - Test different thumbnails (A/B/C variants)
  - Test different titles/descriptions
  - Test homepage layouts
  - Random user assignment to test groups
  - Statistical significance calculator
  - Results dashboard with conversion metrics
- **Why:** Data-driven optimization of content presentation
- **Suggested Service:** New `ab-testing-service` or integrate with `monitoring`
- **Priority:** 🟢 Low (but valuable at scale)

#### 20. Content Scheduling
- **Description:** Schedule movie releases and removals
- **Features:**
  - Set publish date/time for movies (future publishing)
  - Schedule content removal (licensing expiration)
  - Pre-publish visibility: "Coming Soon" with countdown
  - Notification scheduling: Alert users before premiere
  - Calendar view of scheduled content
- **Why:** Essential for planned releases and licensing compliance
- **Suggested Service:** Extend `media-service` with Quartz scheduler (already in `payment-service`)
- **Priority:** 🟡 Medium

#### 21. Regional Availability & Geo-Restrictions
- **Description:** Control content availability by geographic region
- **Features:**
  - Per-movie region allowlist/blocklist
  - Licensing territory management
  - Geo-IP detection for access control
  - Region-specific pricing
  - Regional homepage customization
  - VPN detection (optional)
- **Why:** Required for content licensing compliance in most markets
- **Suggested Service:** Extend `media-service` + `gateway` (geo-routing)
- **Priority:** 🟡 Medium (if going international)

---

### Analytics & Insights

#### 22. AI Data Analyst
- **Description:** Natural language queries over platform analytics
- **Features:**
  - Ask questions in plain English: "What was revenue last month?", "Top 5 movies by watch time this week"
  - Powered by `ai-service` + ClickHouse (`movie-analytics-service`)
  - Auto-generate charts/visualizations
  - Saved reports and scheduled reports
  - Export results (CSV, PDF, PNG)
  - Pre-built templates: Revenue, Engagement, Content Performance
- **Why:** Listed in your features.md; democratizes data access for non-technical admins
- **Suggested Service:** Extend `ai-service` with ClickHouse tool integration
- **Priority:** 🟡 Medium

#### 23. Churn Prediction
- **Description:** Identify users at risk of canceling subscription
- **Features:**
  - ML model scoring users on churn probability
  - Risk factors: Declining watch time, payment failures, inactivity
  - Automated retention campaigns: Email offers, push notifications
  - Churn dashboard with cohort analysis
  - Intervention tracking (did the retention email work?)
- **Why:** Retaining users is cheaper than acquiring new ones
- **Suggested Service:** New `ml-service` or extend `personalization-recommendation`
- **Priority:** 🟢 Low (but high ROI at scale)

#### 24. Revenue & Business Dashboard
- **Description:** Comprehensive business metrics for admins
- **Features:**
  - **Metrics:** MRR, ARR, ARPU, LTV, CAC, Churn Rate
  - **Payments:** Conversion rate, payment failure rate, refund rate
  - **Subscriptions:** Active subscribers by plan, upgrade/downgrade rates
  - **Content:** Revenue per title, watch time vs. licensing cost
  - **Funnels:** Registration → Free Trial → Paid Subscription
  - Export to CSV/PDF
  - Date range comparisons (MoM, YoY)
- **Why:** Single source of truth for business health
- **Suggested Service:** New `analytics-dashboard-service` consuming ClickHouse + `payment-service` data
- **Priority:** 🔴 High

#### 25. Content Performance Analytics
- **Description:** Deep dive into how content is performing
- **Features:**
  - Watch completion rates per title
  - Drop-off points (heatmap integration from `movie-analytics-service`)
  - Regional popularity breakdown
  - Peak viewing hours
  - Device breakdown (mobile vs. desktop vs. TV)
  - Correlation between ratings and watch time
  - "Sleeper hits" (low marketing, high organic growth)
- **Why:** Informs content acquisition and production decisions
- **Suggested Service:** Extend `movie-analytics-service` with admin dashboard
- **Priority:** 🟡 Medium

---

### User Management

#### 26. User Management Panel
- **Description:** Admin interface for managing user accounts
- **Features:**
  - Search/filter users (email, name, ID, status)
  - View user details: Profile, subscriptions, watch history, reviews
  - Actions: Reset password, verify email, ban/suspend, delete account
  - View user's devices/sessions (integrates with `auth-service`)
  - Impersonate user (for support debugging)
  - Activity timeline for user
  - Export user data (GDPR compliance)
- **Why:** Essential for customer support and platform management
- **Suggested Service:** New `user-service` (admin endpoints)
- **Priority:** 🔴 High

#### 27. Manual Subscription Management
- **Description:** Admin tools for managing user subscriptions
- **Features:**
  - Grant free premium (duration or permanent)
  - Extend subscription expiration date
  - Apply manual discounts/credits
  - View subscription history for user
  - Override auto-cancellation
  - Issue refunds (integrates with `payment-service`)
  - Reason logging for audit trail
- **Why:** Required for customer support and promotional campaigns
- **Suggested Service:** Extend `payment-service` admin endpoints
- **Priority:** 🟡 Medium

#### 28. Support Ticket System
- **Description:** User-to-admin communication for issues
- **Features:**
  - Users can submit tickets: Billing, Technical, Content Request, Other
  - Priority levels: Low, Medium, High, Urgent
  - Ticket categories with auto-routing
  - Admin response and resolution tracking
  - SLA monitoring (response time targets)
  - Ticket history for user
  - Canned responses for common issues
  - Escalation rules
- **Why:** Professionalizes customer support
- **Suggested Service:** New `support-service` or integrate with notification system
- **Priority:** 🟢 Low

---

## 💰 Monetization Features

#### 29. Pay-Per-View / Rentals
- **Description:** Time-limited access without full subscription
- **Features:**
  - Rental window: 48 hours from first play
  - Rental pricing per title
  - Purchase option (permanent access, higher price)
  - Early access rentals (new releases at premium)
  - Bundle deals (rent 3, get 1 free)
  - Integrates with existing `payment-service`
- **Why:** Captures users unwilling to commit to subscription; revenue from occasional viewers
- **Suggested Service:** Extend `payment-service` with rental product type
- **Priority:** 🔴 High

#### 30. Gift Subscriptions
- **Description:** Users can purchase subscriptions for others
- **Features:**
  - Gift any subscription plan
  - Custom gift message
  - Scheduled delivery (birthday, holiday)
  - Gift card codes (redeemable later)
  - Bulk gift purchases (corporate)
  - Gift recipient doesn't need account yet (email claim)
- **Why:** Seasonal revenue spike; user acquisition channel
- **Suggested Service:** Extend `payment-service` + `email-service`
- **Priority:** 🟡 Medium

#### 31. Referral Program
- **Description:** Reward users for inviting friends
- **Features:**
  - Unique referral code/link per user
  - Reward: Free month, discount credits, or cash
  - Referee also gets reward (double-sided incentive)
  - Referral tracking dashboard for user
  - Fraud detection (self-referrals, fake accounts)
  - Leaderboard (top referrers)
  - Tiered rewards (refer 5, 10, 25 friends)
- **Why:** Low-cost user acquisition with built-in virality
- **Suggested Service:** New `referral-service` or extend `user-service`
- **Priority:** 🟡 Medium

#### 32. Loyalty Points System
- **Description:** Earn points for engagement, redeem for perks
- **Features:**
  - **Earn points for:** Watching movies, writing reviews, referrals, daily login
  - **Redeem for:** Discount on subscription, free rental, exclusive content, badges
  - Points expiration (e.g., 12 months)
  - Tiered membership (Bronze, Silver, Gold) with increasing perks
  - Points balance display in user profile
  - Admin-configurable point values
- **Why:** Increases daily active users and platform engagement
- **Suggested Service:** New `loyalty-service` or integrate with gamification
- **Priority:** 🟢 Low

#### 33. Bundle & Family Plans
- **Description:** Multi-user or discounted subscription options
- **Features:**
  - **Family Plan:** Up to 5 profiles, shared billing, parental controls
  - **Student Plan:** Already partially implemented in `auth-service`
  - **Annual Plan:** Discounted yearly billing (2 months free)
  - **Partner Bundles:** Co-marketing with telcos, ISPs, device manufacturers
  - Profile switching UI
  - Per-profile watch history and recommendations
- **Why:** Higher ARPU; family plans reduce churn (harder to cancel shared account)
- **Suggested Service:** Extend `payment-service` (subscription plans) + `auth-service` (profile management)
- **Priority:** 🔴 High

---

## 🔧 Technical Improvements

#### 34. CDN Integration
- **Description:** Serve HLS content via CDN for global performance
- **Features:**
  - CloudFront / Akamai / CloudFlare integration
  - Cache invalidation on content updates
  - Multi-CDN failover
  - Edge-side authentication (signed URLs)
  - Cache hit rate monitoring
  - Regional CDN routing
- **Why:** Critical for streaming quality at scale; reduces origin server load
- **Suggested Service:** Infrastructure change for `media-streaming-service` + MinIO
- **Priority:** 🟡 Medium (before scaling to large user base)

#### 35. DRM Integration
- **Description:** Digital rights management for premium content protection
- **Features:**
  - Google Widevine (Chrome, Android, Firefox)
  - Apple FairPlay (Safari, iOS, macOS)
  - Microsoft PlayReady (Edge, Xbox)
  - License server integration
  - Per-title encryption keys
  - Offline download DRM
- **Why:** Required by major studios for premium content licensing
- **Suggested Service:** Extend `transcode-worker` + `media-streaming-service`
- **Priority:** 🟡 Medium (for premium content deals)

#### 36. GraphQL API Layer
- **Description:** Flexible API querying for frontend efficiency
- **Features:**
  - Single endpoint for all data needs
  - Fetch movie + reviews + recommendations in one query
  - Eliminate over-fetching/under-fetching
  - Type-safe schema with introspection
  - DataLoader for N+1 query prevention
  - Subscriptions for real-time updates
- **Why:** Improves frontend performance and developer experience
- **Suggested Service:** New `graphql-gateway` or extend `gateway` with GraphQL plugin
- **Priority:** 🟢 Low

#### 37. Webhook System
- **Description:** Allow external services to subscribe to platform events
- **Features:**
  - Event types: New Movie, Price Change, Subscription Event, Review Posted
  - User-configurable webhook endpoints
  - Retry logic with exponential backoff
  - Webhook delivery dashboard (success/failure rates)
  - HMAC signature verification
  - Webhook secret management
  - Event payload customization
- **Why:** Enables third-party integrations and automation
- **Suggested Service:** New `webhook-service` (NATS consumer)
- **Priority:** 🟢 Low

#### 38. API Versioning Strategy
- **Description:** Proper versioning for backward compatibility
- **Features:**
  - URL-based versioning: `/api/v1/`, `/api/v2/`
  - Deprecation headers in responses
  - Version sunset timeline documentation
  - Backward compatibility testing in CI/CD
  - Version migration guides
  - Support multiple versions simultaneously (6-month overlap)
- **Why:** Prevents breaking changes for existing clients
- **Suggested Service:** Gateway-level configuration + all services
- **Priority:** 🟡 Medium (before public API launch)

#### 39. Feature Flags
- **Description:** Gradual rollout of new features
- **Features:**
  - Toggle features per user, per segment, or globally
  - Percentage-based rollout (1%, 5%, 25%, 100%)
  - A/B test integration
  - Kill switch for instant rollback
  - Flag evaluation caching (Redis/Consul)
  - Admin dashboard for flag management
  - Flag audit log
- **Why:** Reduces deployment risk; enables canary releases
- **Suggested Service:** New `feature-flags-service` or use existing Consul
- **Priority:** 🟡 Medium

---

## 📊 Implementation Priority Matrix

### 🔴 Quick Wins (Low Effort, High Impact)
| # | Feature | Effort | Impact |
|---|---------|--------|--------|
| 1 | Continue Watching Carousel | Low | High |
| 2 | Smart Playback (Skip Intro, Auto-play) | Low | High |
| 3 | "Because You Watched..." Recommendations | Medium | High |
| 4 | Advanced Filters | Medium | Medium |
| 5 | User Management Panel | Medium | High |
| 6 | Content Moderation Dashboard | Medium | High |
| 7 | Bulk Import/Export | Medium | High |
| 8 | Movie Reviews & Ratings | Medium | High |
| 9 | Notifications System | Medium | High |
| 10 | Revenue Dashboard | Medium | High |

### 💰 Revenue Drivers
| # | Feature | Revenue Impact | Complexity |
|---|---------|---------------|------------|
| 1 | Pay-Per-View / Rentals | High | Medium |
| 2 | Bundle & Family Plans | High | Medium |
| 3 | Gift Subscriptions | Medium | Low |
| 4 | Referral Program | Medium | Medium |
| 5 | Offline Downloads (Premium) | Medium | High |

### 🔄 User Retention
| # | Feature | Retention Impact | Complexity |
|---|---------|-----------------|------------|
| 1 | Notifications System | High | Medium |
| 2 | Watch Parties | High | High |
| 3 | Achievements & Badges | Medium | Low |
| 4 | Loyalty Points | Medium | Medium |
| 5 | User Profiles | Medium | Medium |

### 🛠 Admin Productivity
| # | Feature | Time Saved | Complexity |
|---|---------|-----------|------------|
| 1 | AI Data Analyst | High | Medium |
| 2 | Bulk Import/Export | High | Medium |
| 3 | Content Scheduling | Medium | Low |
| 4 | Content Moderation Dashboard | High | Medium |
| 5 | Manual Subscription Management | Medium | Low |

---

## 🏗 Suggested New Services

Based on the features above, here are new services you may need to create:

| Service | Purpose | Dependencies |
|---------|---------|--------------|
| `review-service` | Movie reviews, ratings, comments | MySQL, Elasticsearch, NATS |
| `user-service` | User profiles, management, social features | MySQL, Redis, MinIO (avatars) |
| `notification-service` | Multi-channel notifications (evolution of email-service) | NATS, Redis, Email providers, Push providers |
| `discussion-service` | Comments, threads, community features | MySQL, Redis (real-time), NATS |
| `watchparty-service` | Watch party rooms, synchronization | Redis, WebSocket server |
| `support-service` | Ticketing, customer support | MySQL, NATS, Email |
| `loyalty-service` | Points, rewards, tiers | MySQL, Redis |
| `referral-service` | Referral tracking, rewards | MySQL, Redis, NATS |
| `analytics-dashboard-service` | Business metrics, reporting | ClickHouse, Redis |
| `feature-flags-service` | Feature toggle management | Consul/Redis |
| `webhook-service` | External event webhooks | NATS, Redis (retry queue) |
| `ml-service` | Churn prediction, advanced ML | Python, ClickHouse, Qdrant |

---

## 📝 Notes

- Features marked with 🔴 **High Priority** are recommended for near-term implementation
- Many features integrate with existing services (NATS events, Redis, ClickHouse, Qdrant)
- Consider implementing **Feature Flags** (#39) early to enable safe rollout of all new features
- Your current architecture is well-positioned for these additions due to event-driven design

---

*This document is a living specification. Update as features are implemented or priorities change.*
