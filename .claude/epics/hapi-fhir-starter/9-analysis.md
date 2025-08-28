---
issue: 9
title: OAuth JWT Integration
analyzed: 2025-08-28T21:27:52Z
estimated_hours: 16
parallelization_factor: 2.8
---

# Parallel Work Analysis: Issue #9

## Overview
Implement OAuth 2.0 JWT token authentication for the HAPI FHIR server, including token validation, user identity extraction, and Spring Security integration. This foundational authentication system enables secure access control and is required by the Identity Resolution Service.

## Parallel Streams

### Stream A: JWT Token Validation Core
**Scope**: JWT parsing, validation logic, and token claim extraction
**Files**:
- `src/main/java/ca/uhn/fhir/jpa/starter/security/JwtTokenValidator.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/security/TokenClaimExtractor.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/config/JwtConfiguration.java`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 6
**Dependencies**: none

### Stream B: Spring Security Configuration
**Scope**: Spring Security setup, filter chains, and authentication flow
**Files**:
- `src/main/java/ca/uhn/fhir/jpa/starter/security/SecurityConfig.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/security/OAuth2ResourceServerConfig.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/security/AuthenticationEntryPoint.java`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 5
**Dependencies**: none

### Stream C: HAPI FHIR Interceptor Integration
**Scope**: Authentication interceptors for HAPI FHIR request pipeline
**Files**:
- `src/main/java/ca/uhn/fhir/jpa/starter/interceptors/AuthenticationInterceptor.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/interceptors/AuthorizationInterceptor.java`
**Agent Type**: backend-specialist
**Can Start**: after Stream A completes
**Estimated Hours**: 4
**Dependencies**: Stream A (needs token validation)

### Stream D: Configuration and Properties
**Scope**: Application configuration, YAML properties, and Azure Identity support
**Files**:
- `src/main/resources/application.yaml`
- `src/main/java/ca/uhn/fhir/jpa/starter/config/AuthConfigurationProperties.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/security/AzureIdentityConfiguration.java`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 3
**Dependencies**: none

### Stream E: Comprehensive Testing
**Scope**: Unit tests, integration tests, and security testing
**Files**:
- `src/test/java/ca/uhn/fhir/jpa/starter/security/JwtTokenValidatorTest.java`
- `src/test/java/ca/uhn/fhir/jpa/starter/security/SecurityConfigTest.java`
- `src/test/java/ca/uhn/fhir/jpa/starter/interceptors/AuthenticationInterceptorTest.java`
- `src/test/java/ca/uhn/fhir/jpa/starter/integration/OAuthIntegrationTest.java`
**Agent Type**: backend-specialist
**Can Start**: after Streams A & B complete
**Estimated Hours**: 6
**Dependencies**: Streams A, B, C

## Coordination Points

### Shared Files
- `pom.xml` - Stream A (JWT dependencies), Stream B (Spring Security dependencies)
- `src/main/resources/application.yaml` - Stream D (OAuth config), Stream B (security config)

### Sequential Requirements
1. JWT validation core (Stream A) before HAPI interceptors (Stream C)
2. Spring Security config (Stream B) before integration tests (Stream E)
3. Configuration properties (Stream D) before Azure Identity setup
4. All core streams (A, B, C) before comprehensive testing (Stream E)

## Conflict Risk Assessment
- **Low Risk**: Streams A, B, D work on different functional areas
- **Medium Risk**: Stream C depends on Stream A interfaces
- **Low Risk**: Shared configuration files have clear separation of concerns

## Parallelization Strategy

**Recommended Approach**: hybrid

Launch Streams A, B, D simultaneously. Start C when A completes. Start E when A, B, C complete. This maximizes parallel work while respecting dependencies.

## Expected Timeline

With parallel execution:
- Wall time: 6 hours (max of A+C sequential path)
- Total work: 24 hours
- Efficiency gain: 75%

Without parallel execution:
- Wall time: 24 hours

## Notes
- Stream A is critical path - JWT validation must be solid before interceptor integration
- Azure Identity support (part of Stream D) can be implemented as enhancement after core OAuth
- Performance testing should focus on <100ms cached validation target
- Security testing must include token tampering and expiration scenarios
- Consider implementing OAuth provider auto-discovery as stretch goal