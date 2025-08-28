---
name: hapi-fhir-starter
status: backlog
created: 2025-08-27T21:25:23Z
progress: 0%
prd: .claude/prds/hapi-fhir-starter.md
github: https://github.com/evoleen/hapi-fire-arrow/issues/3
---

# Epic: Fire Arrow Server - Phase 1 Foundation

## Overview

Transform the HAPI FHIR JPA starter into "Fire Arrow Server" by implementing a layered architecture that adds OAuth authentication and dynamic RBAC capabilities without modifying the base HAPI code. The implementation uses Spring Boot interceptors and auto-configuration to integrate seamlessly while maintaining upstream sync capability.

## Architecture Decisions

- **Layered Architecture**: All Fire Arrow customizations implemented as Spring Boot auto-configuration modules that extend HAPI FHIR without touching base code
- **Interceptor Pattern**: Use HAPI FHIR's interceptor framework for request processing pipeline integration
- **Identity Mapping**: OAuth token email → FHIR resource lookup → compartment-based access control
- **Caching Strategy**: Multi-layer caching with Spring Cache abstraction (identity resolution, compartment lookups, validation results)
- **Configuration Management**: Environment-based configuration using Spring Boot profiles and externalized properties
- **Observability**: OpenTelemetry integration for Azure Application Insights compatibility

## Technical Approach

### Backend Services

**Authentication Service**
- OAuth 2.0 JWT token validation and parsing
- Email-to-FHIR resource identity mapping with caching
- Spring Security integration for seamless authentication flow

**Validation Framework**
- Abstract validator interface for extensible rule implementation  
- PatientCompartment validator as first concrete implementation
- Request context (user role, FHIR entity, CRUD operation, optional role codes)
- Caching layer for validation results with configurable TTL

**FHIR Integration**
- HAPI FHIR interceptor for request lifecycle integration
- Server metadata customization for "Fire Arrow Server" branding
- Clean startup with warning elimination and proper configuration

### Infrastructure

**Spring Boot Configuration**
- Auto-configuration classes for Fire Arrow components
- Profile-based configuration (dev, staging, prod)
- Externalized configuration for tenant-specific settings

**Caching Infrastructure**
- Spring Cache with Redis backend for production
- In-memory caching for development/testing
- Cache warming strategies for identity resolution

**Observability Stack**
- Structured JSON logging with logback configuration
- OpenTelemetry auto-instrumentation for Azure Application Insights
- Custom metrics for authentication/validation performance
- Health checks for all critical components

## Implementation Strategy

**Phase 1A: Foundation (Weeks 1-2)**
- Server rebranding and clean startup
- Basic Spring Boot auto-configuration structure
- Logging and health check implementation

**Phase 1B: Authentication (Weeks 3-4)**  
- OAuth token validation and parsing
- FHIR resource identity mapping
- Spring Security integration

**Phase 1C: Validation Framework (Weeks 5-6)**
- Abstract validator framework design
- PatientCompartment validator implementation
- HAPI FHIR interceptor integration

**Phase 1D: Performance & Testing (Weeks 7-8)**
- Caching implementation and optimization
- Comprehensive test suite (unit + integration)
- Performance validation and tuning

## Task Breakdown Preview

High-level task categories for implementation:

- [ ] **Server Foundation**: Rebrand server, eliminate startup warnings, configure Spring Boot auto-configuration
- [ ] **OAuth Integration**: Implement JWT token validation, email parsing, and Spring Security configuration  
- [ ] **Identity Resolution**: Build FHIR resource lookup by email with caching layer
- [ ] **Validator Framework**: Create extensible validator architecture with request context handling
- [ ] **Patient Compartment**: Implement PatientCompartment validator with FHIR R4 compartment rules
- [ ] **HAPI Integration**: Integrate validation pipeline via HAPI FHIR interceptors
- [ ] **Caching Layer**: Implement multi-layer caching for identity resolution and validation results
- [ ] **Observability**: Configure OpenTelemetry, structured logging, and custom metrics
- [ ] **Testing Suite**: Comprehensive unit and integration tests with >90% coverage
- [ ] **Performance Validation**: Load testing and performance optimization to meet <100ms targets

## Dependencies

**External Dependencies**
- Fire Arrow reference implementation documentation access
- OAuth 2.0 provider with consistent JWT token format  
- Azure Application Insights for observability pipeline
- FHIR R4 specification for compartment definitions

**Internal Dependencies**
- DevOps team for Kubernetes deployment configuration
- Security team for OAuth token format specification review
- QA team for compliance testing framework development

**Technology Dependencies**
- HAPI FHIR JPA starter (upstream sync capability)
- Spring Boot 3.x with Spring Security 6.x
- OpenTelemetry Java agent for Azure integration
- Redis for production caching backend

## Success Criteria (Technical)

**Performance Benchmarks**
- Authentication latency: <100ms (p95) for cached results, <500ms for uncached
- Validation latency: <100ms (p95) for cached compartment checks
- Cache hit ratio: >90% for identity resolution after warmup
- Concurrent user support: 1000+ authenticated sessions per instance

**Quality Gates**  
- Zero startup warnings or errors
- >90% unit test coverage for all Fire Arrow components
- >99.9% authentication success rate for valid tokens
- 100% access control enforcement accuracy for PatientCompartment

**Integration Criteria**
- Successful HAPI FHIR upstream merge without conflicts
- Clean separation of Fire Arrow code from base HAPI implementation
- Spring Boot auto-configuration working across all deployment profiles

## Estimated Effort

**Overall Timeline**: 8 weeks for Phase 1 completion

**Resource Requirements**: 
- 1 Senior Java Developer (full-time)
- 1 DevOps Engineer (25% allocation)
- 1 QA Engineer (50% allocation for weeks 6-8)

**Critical Path Items**:
1. OAuth integration and identity resolution (foundation for all other work)
2. HAPI FHIR interceptor integration (enables validation pipeline)
3. Caching implementation (required for performance targets)
4. Testing framework (required for compliance validation)

## Tasks Created
- [ ] #10 -  (parallel: )
- [ ] #11 -  (parallel: )
- [ ] #4 -  (parallel: )
- [ ] #5 -  (parallel: )
- [ ] #6 -  (parallel: )
- [ ] #7 -  (parallel: )
- [ ] #8 -  (parallel: )
- [ ] #9 -  (parallel: )

Total tasks: 8
Parallel tasks: 0
Sequential tasks: 8
Estimated total effort: 8 weeks