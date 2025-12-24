# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) generated from git history.

**Total ADRs:** 43  
**Generated:** 2025-12-21T09:42:18.362Z

## ADRs

- [ADR-1](0001-implement-redis-caching-for-email-verification-tok.md): Implement Redis caching for email verification tokens and update user authentica
- [ADR-2](0002-fix-verifying-email-twice-leading-to-error-cache-r.md): Fix verifying email twice leading to error (cache removed the token email), fix 
- [ADR-3](0003-migrate-from-symmetric-jwt-to-asymmetric-jwt.md): Migrate from symmetric jwt to asymmetric jwt
- [ADR-4](0004-feat-payment-integrate-payment-with-vnpay-stripe-p.md): Feat<payment> integrate payment with Vnpay, Stripe, Paypal
- [ADR-5](0005-added-code-from-zip.md): Added code from ZIP
- [ADR-6](0006-fix-upload-file-automatically-add-prefix-file.md): Fix upload file automatically add prefix file:///
- [ADR-7](0007-add-support-to-modify-video.md): Add support to modify video
- [ADR-8](0008-remove-unsupported-nimbus-algo.md): Remove unsupported nimbus algo
- [ADR-9](0009-architecture-decision.md): Architecture Decision
- [ADR-10](0010-add-api-gateway-and-eureka-server-for-services-dis.md): Add api gateway and eureka server for services discovery
- [ADR-11](0011-update-eureka-client-for-quarkus-notification-serv.md): Update eureka client for quarkus notification service and micronaut review servi
- [ADR-12](0012-begin-to-migrate-auth-to-seperate-service.md): Begin to migrate auth to seperate service
- [ADR-13](0013-fix-routing-error-when-gateway-trying-to-forward-r.md): - fix routing error when gateway trying to forward request to itself
- [ADR-14](0014-update-role-based-access-control-to-attribute-base.md): - update role based access control to attribute based access control
- [ADR-15](0015-update-role-based-access-control-to-attribute-base.md): - update role based access control to attribute based access control
- [ADR-16](0016-migrate-payment-service-to-kotlin-not-tested.md): - migrate payment service to kotlin (not tested)
- [ADR-17](0017-migrate-payment-service-to-kotlin-not-tested.md): - migrate payment service to kotlin (not tested)
- [ADR-18](0018-integrate-new-auth-mechanism-to-front-end.md): - integrate new auth mechanism to front end
- [ADR-19](0019-secure-cloudinary-cloud.md): - secure cloudinary cloud
- [ADR-20](0020-vnpay-api-got-expired.md): - Vnpay api got expired
- [ADR-21](0021-support-both-v1-and-v2-zalopay-support-both-http-g.md): - support both v1 and v2 zalopay - support both HTTP GET via params (vnpay) and 
- [ADR-22](0022-support-both-v1-and-v2-zalopay-support-both-http-g.md): - support both v1 and v2 zalopay - support both HTTP GET via params (vnpay) and 
- [ADR-23](0023-add-feature-to-discount-student.md): - add feature to discount student
- [ADR-24](0024-fixed-batch-processing-for-saving-80k-entries-to-d.md): - fixed batch processing for saving 80k entries to db
- [ADR-25](0025-add-feat-student-status-account.md): - add feat student status account
- [ADR-26](0026-add-feat-student-status-account.md): - add feat student status account
- [ADR-27](0027-disable-csrf.md): - disable  csrf
- [ADR-28](0028-disable-csrf.md): - disable  csrf
- [ADR-29](0029-check-transaction-expire-to-prevent-replay.md): - check transaction expire to prevent replay
- [ADR-30](0030-add-endpoint-to-support-light-way-routing-for-gate.md): - add endpoint to support light way routing for gateway (jwe)
- [ADR-31](0031-migrate-kafka-to-nats-jetstream.md): - migrate kafka to nats jetstream
- [ADR-32](0032-before-final.md): - before final
- [ADR-33](0033-major-change-refactor-jose-implementation-to-a-sin.md): [Major Change] Refactor JOSE implementation to a single provider
- [ADR-34](0034-refractor-file-service-remove-unnecessary-jose-alg.md): - refractor(file-service) + remove unnecessary jose algorithms + add auto init s
- [ADR-35](0035-feat-gateway-add-ratelimit-to-api-gateway.md): - feat(gateway) + add ratelimit to api gateway
- [ADR-36](0036-feat-gateway-add-ratelimit-to-api-gateway.md): - feat(gateway) + add ratelimit to api gateway
- [ADR-37](0037-bug-gateway-wrong-credentials-on-login-now-return-.md): - bug(gateway) + wrong credentials on login now return 401 instead of 404
- [ADR-38](0038-refractor-gateway-add-ratelimit-config-via-yml.md): - refractor(gateway): add ratelimit config via yml
- [ADR-39](0039-refractor-gateway-remove-api-key-and-token-null-ch.md): - refractor(gateway): remove api key and token null check
- [ADR-40](0040-debug-ai-service-add-security-to-the-service.md): - debug(ai-service) add security to the service
- [ADR-41](0041-refactor-ai-service-restructure-package-hierarchy-.md): Refactor(ai-service): restructure package hierarchy, add `AiModeConfiguration`, 
- [ADR-42](0042-chat-architecture.md): Chat-architecture
- [ADR-43](0043-bug-ai-service-prevent-classcastexception-when-tak.md): Bug(ai-service): prevent ClassCastException when taking information from jwt

## How These Were Generated

These ADRs were automatically generated by analyzing git commit history and identifying significant architectural changes. Each ADR represents one or more commits that introduced substantial changes to:

- Service architecture
- Technology choices
- Infrastructure setup
- Major refactorings
- Security implementations

**Note:** These ADRs are retroactive and may not capture all context. Review and refine as needed.
