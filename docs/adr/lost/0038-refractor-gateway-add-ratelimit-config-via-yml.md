# 38. - refractor(gateway): add ratelimit config via yml

**Status:** Proposed  
**Date:** 2025-10-19  
**Deciders:** Cao Gia Bao  
**Commit:** c01bfcef

## Context

The decision to add ratelimit config via YAML in the gateway service was needed to address performance issues caused by excessive requests to the BBMovie platform. The monolithic 'file-service' had no proper rate limiting mechanisms, leading to system overload and poor user experience. By adopting a configuration-driven approach using Bucket4j and Yaml, we can easily scale the number of requests processed per second without modifying the gateway's codebase.

**Commit Message:** - refractor(gateway): add ratelimit config via yml





## Decision

The decision to add ratelimit config via YAML in the gateway service was made by adding Bucket4jConfigProperties and CompiledFilterConfig classes with corresponding methods to read and parse the configuration file. This allows for dynamic adjustment of rate limiting rules based on current load without changing gateway code.

## Consequences

- Positive: Improved user experience through better handling of excessive requests, reducing system overload.
- Positive: Facilitated easier scaling by allowing dynamic configuration without code changes.
- Negative: Trade-off between flexibility and maintainability. YAML config may lead to hard-to-follow errors if not properly managed.
- Negative: Risk of misconfiguration due to incorrect YAML syntax or missing values.
## Alternatives Considered

- Alternative 1: Implement rate limiting directly in the gateway service code, which would require changes and maintenance every time the rules change.
- Alternative 2: Use a command-line tool to generate the ratelimit configuration file, which could lead to inconsistencies between development, staging, and production environments.

## Technical Details

- **Commit Hash:** `c01bfcef021c16bb256076ebc63f9048d716fddc`
- **Files Changed:** 5
- **Lines Added:** 561
- **Lines Removed:** 0
- **Affected Areas:** infrastructure, refactor, service, technology

## Related Files

- `backend/gateway/pom.xml` (+0/-0)
- `.../com/bbmovie/gateway/config/JacksonConfig.java` (+0/-0)
- `.../config/ratelimit/Bucket4jConfigProperties.java` (+0/-0)
- `.../config/ratelimit/Bucket4jRedisConfig.java` (+0/-0)
- `.../config/ratelimit/CompiledFilterConfig.java` (+0/-0)

