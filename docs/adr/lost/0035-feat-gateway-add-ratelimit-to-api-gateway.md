# 35. - feat(gateway) + add ratelimit to api gateway

**Status:** Proposed  
**Date:** 2025-10-18  
**Deciders:** Cao Gia Bao  
**Commit:** da029062

## Context

The decision to add rate limiting to the API gateway was needed to mitigate DDoS attacks and protect the platform's performance and reliability. The growing user base and increasing media uploads have put pressure on the system, leading to potential bottlenecks and slow response times.

**Commit Message:** - feat(gateway) + add ratelimit to api gateway





## Decision

To address this issue, a ratelimiter has been added to the API Gateway to limit request rate per client and IP. This ensures fair resource distribution among users, preventing the gateway from being overwhelmed by excessive requests.

## Consequences

- Positive: Protects platform against DDoS attacks.
- Positive: Ensures system stability under high load.
- Negative: May slightly increase latency for some users due to additional processing.
- Negative: Requires careful configuration and monitoring of ratelimit thresholds.
## Alternatives Considered

- Alternative 1: Implementing rate limiting on individual services instead of the gateway.
- Alternative 2: Using a centralized load balancer with built-in rate limiting.

## Technical Details

- **Commit Hash:** `da02906222b95998a49bb61fd4875779cbddad87`
- **Files Changed:** 1
- **Lines Added:** 18
- **Lines Removed:** 0
- **Affected Areas:** service, infrastructure

## Related Files

- `backend/gateway/src/main/resources/application.yml` (+0/-0)

