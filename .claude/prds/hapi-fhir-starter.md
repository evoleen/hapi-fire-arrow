---
name: hapi-fhir-starter
description: Fire Arrow Server - HAPI FHIR server with integrated dynamic RBAC capabilities
status: backlog
created: 2025-08-27T20:55:39Z
---

# PRD: Fire Arrow Server - Phase 1 Foundation

## Executive Summary

Fire Arrow Server is a unified FHIR server that integrates dynamic Role-Based Access Control (RBAC) capabilities directly into the HAPI FHIR JPA starter project. This eliminates the need for a separate facade service, improving performance and simplifying code management while providing sophisticated healthcare data access controls.

The solution transforms users into FHIR resource identities (Patient, Practitioner, Device, RelatedPerson) and applies compartment-based access controls through an extensible validator framework. This Phase 1 focuses on foundational elements: proper server identification, OAuth authentication integration, and a basic patient compartment validator.

## Problem Statement

**Current Challenge**: Organizations deploying FHIR servers need sophisticated access controls that go beyond basic authentication. The existing Fire Arrow solution (https://firearrow.io) provides this functionality as a separate facade service, but this architecture creates:

- Performance overhead from multiple service calls
- Increased deployment complexity with separate service management
- Code maintenance burden across multiple repositories
- Potential single points of failure in the service chain

**Why Now**: Healthcare organizations require mature, compliance-ready FHIR implementations that can support multi-tenant SaaS deployments with fine-grained access controls. Unifying these capabilities into a single server reduces operational complexity while improving performance.

## User Stories

### Primary User Personas

**1. Healthcare Application Developer**
- As a developer, I want to easily sync updates from the upstream HAPI FHIR JPA starter without conflicts, so I can benefit from ongoing improvements while maintaining our custom functionality
- As a developer, I want an extensible validator framework so I can build complex access control rules for different healthcare scenarios
- As a developer, I want comprehensive testing capabilities so I can ensure access control rules work correctly and meet compliance requirements

**2. DevOps Engineer**  
- As a DevOps engineer, I want the server to start cleanly without warnings so I can confidently deploy in production environments
- As a DevOps engineer, I want proper server identification and branding so I can distinguish this service in monitoring and logging systems
- As a DevOps engineer, I want the server to be Kubernetes-native so it integrates seamlessly with our CNPG, FluxCD infrastructure

**3. Healthcare End User (Patient)**
- As a patient, I want to access only my own healthcare data through OAuth authentication so my privacy is protected
- As a patient, I want fast response times when accessing my data so the application feels responsive
- As a patient, I want reliable access to my data so I can depend on the service for critical healthcare decisions

**4. Healthcare Provider**
- As a practitioner, I want to access patient data appropriate to my role and relationships so I can provide effective care while respecting privacy
- As a practitioner, I want the system to authenticate me based on my existing credentials so I don't need separate login processes

## Requirements

### Functional Requirements

**FR-1: Server Identity and Branding**
- Server SHALL identify itself as "Fire Arrow Server" in all appropriate contexts
- Version information SHALL include both HAPI FHIR base version and Fire Arrow Server version
- Server metadata SHALL reflect the Fire Arrow Server identity
- Startup process SHALL complete without warnings or errors

**FR-2: OAuth Authentication Integration**
- Server SHALL support OAuth 2.0 token-based authentication
- Token parsing SHALL extract user identity information (email, role type)
- Authentication system SHALL map users to FHIR resource identities based on configurable rules
- System SHALL support mapping to Practitioner, Patient, Device, and RelatedPerson resource types
- Authentication SHALL use email address as primary identifier for resource lookup

**FR-3: User Identity Resolution**
- System SHALL locate FHIR resources by matching authenticated user's email against resource telecom fields
- User SHALL assume the identity of the matched FHIR resource for all subsequent requests
- System SHALL handle cases where no matching resource is found gracefully
- Identity resolution SHALL be cached for performance

**FR-4: Patient Compartment Validator**
- System SHALL implement a PatientCompartment validator as the first request validator
- Validator SHALL restrict patient-role users to resources within their patient compartment
- Validator SHALL support FHIR R4 compartment definitions
- Validator SHALL be configurable for different compartment scopes

**FR-5: Extensible Validator Framework**
- System SHALL provide a pluggable architecture for request validators
- Each validator SHALL have access to: user role, FHIR entity, request mode (CRUD/search), optional role codes
- Validators SHALL be configurable per request for dynamic rule evaluation
- Framework SHALL support multiple validators executing in sequence
- Validation results SHALL be cacheable for performance optimization

**FR-6: Request Processing Pipeline**
- All FHIR requests SHALL pass through the validation pipeline
- Pipeline SHALL execute authentication, identity resolution, and validation in sequence
- Failed validations SHALL return appropriate HTTP error responses
- Pipeline SHALL be instrumentable for monitoring and debugging

### Non-Functional Requirements

**NFR-1: Performance**
- Authentication and validation SHALL complete within 100ms for cached results
- System SHALL implement multi-layer caching for identity resolution and validation results
- Cache invalidation SHALL be configurable and automatic
- System SHALL handle 1000+ concurrent users per tenant deployment

**NFR-2: Maintainability**  
- Custom code SHALL be implemented as layers over the base HAPI FHIR JPA starter
- Base HAPI FHIR code SHALL remain unmodified to enable easy upstream synchronization
- All customizations SHALL use Spring Boot auto-configuration patterns
- Code SHALL follow established HAPI FHIR extension patterns

**NFR-3: Observability**
- System SHALL provide structured logging for all authentication and validation events
- Metrics SHALL be exposed for Prometheus collection (or streamed via Azure's OpenTelemtry distribution, whatever works best to feed it into Azure Application Insights)
- Health checks SHALL validate all critical components
- Tracing SHALL be available for request flow debugging

**NFR-4: Deployment Readiness**
- Application SHALL be containerized and Kubernetes-ready
- Configuration SHALL support environment-based overrides
- System SHALL support multi-tenant deployments with tenant isolation
- Resource limits SHALL be configurable for different deployment sizes

**NFR-5: Testing Coverage**
- Unit tests SHALL cover all validator logic with >90% coverage
- Integration tests SHALL verify authentication and validation flows end-to-end
- Performance tests SHALL validate caching and throughput requirements
- Security tests SHALL verify access control enforcement

## Success Criteria

### Measurable Outcomes

1. **Clean Server Startup**: Zero warnings or errors during application startup
2. **Authentication Success Rate**: >99.9% successful OAuth token validation for valid tokens
3. **Patient Compartment Enforcement**: 100% accuracy in restricting patient access to their compartment
4. **Performance Targets**: 
   - <100ms response time for cached validation results
   - <500ms response time for uncached validation results
   - Support for 1000+ concurrent authenticated sessions
5. **Test Coverage**: >90% unit test coverage for all custom validator code
6. **Upstream Sync Capability**: Successful integration of HAPI FHIR updates without conflicts

### Key Metrics and KPIs

- Authentication latency (p50, p95, p99)
- Validation latency (p50, p95, p99)  
- Cache hit ratios for identity resolution and validation
- Failed authentication rate
- Failed authorization rate
- System uptime and availability
- Error rates by validation type

## Constraints & Assumptions

### Technical Limitations
- Must maintain compatibility with HAPI FHIR JPA starter update cycle
- OAuth token format must be compatible with existing authentication infrastructure
- FHIR resource structure must follow R4 specification for compartment definitions
- Java 17+ and Spring Boot framework constraints

### Timeline Constraints
- Phase 1 implementation target: 8-10 weeks
- Must not block other development streams requiring FHIR server functionality
- Integration testing must be completed before any production deployment

### Resource Limitations
- Development team capacity: 2-3 developers
- Infrastructure costs must remain within SaaS economic model
- Performance testing limited to staging environment capacity

### Assumptions
- Fire Arrow reference implementation at https://firearrow.io will remain accessible
- OAuth provider will supply consistent token format
- FHIR R4 compartment definitions are sufficient for initial use cases
- Kubernetes deployment environment is stable and available

## Out of Scope

### Explicitly NOT Building (Phase 1)

1. **Advanced Validators**: Complex multi-resource validators, custom business logic validators, or specialty care pathway validators
2. **Additional Compartments**: Practitioner, Device, or RelatedPerson compartment validators  
3. **Multi-Tenant UI**: Administrative interfaces for tenant configuration or validator management
4. **Compliance Implementation**: Specific ISO 27001, SOC-2, or GDPR implementation details (architecture will be compliance-ready)
5. **Advanced Caching**: Distributed caching across multiple instances or advanced cache warming strategies
6. **Performance Optimization**: Database query optimization, connection pooling tuning, or horizontal scaling features
7. **Audit Logging**: Detailed compliance audit trails (basic request logging will be included)
8. **Custom FHIR Operations**: Beyond standard CRUD and search operations

### Future Phase Considerations

- Advanced validator implementations (Phase 2)
- Multi-compartment access scenarios (Phase 2)  
- Administrative and configuration interfaces (Phase 3)
- Advanced performance and scaling features (Phase 3)

## Dependencies

### External Dependencies
- **Fire Arrow Reference Implementation**: Access to https://firearrow.io and https://docs.firearrow.io for behavior specification
- **HAPI FHIR JPA Starter**: Continued access to upstream repository for sync capability
- **OAuth Provider**: External authentication service providing consistent token format
- **PostgreSQL Database**: CNPG-managed PostgreSQL for FHIR data persistence

### Internal Team Dependencies  
- **DevOps Team**: Kubernetes cluster setup with CNPG and FluxCD
- **Security Team**: OAuth token format specification and authentication flow review
- **QA Team**: Test plan development and compliance validation approach

### Technology Dependencies
- Java 17+ runtime environment
- Spring Boot 3.x framework compatibility  
- Maven build system for dependency management
- Docker containerization platform
- Kubernetes orchestration platform

### Integration Dependencies
- OAuth 2.0 compatible authentication service
- FHIR R4 compatible client applications
- Monitoring infrastructure (OpenTelemetry, preferably Azure's OpenTelemtry distribution)
- Logging infrastructure compatible with structured JSON logs