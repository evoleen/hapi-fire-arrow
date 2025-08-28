---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-28T15:58:42Z
version: 1.0
author: Claude Code PM System
---

# System Patterns & Architecture

## Overall Architecture Pattern

### Layered Architecture (N-Tier)
The system follows a classic layered architecture pattern:
- **Presentation Layer**: Web controllers, REST endpoints, UI templates
- **Service Layer**: FHIR operations, clinical reasoning, business logic
- **Data Access Layer**: JPA repositories, database interactions
- **Database Layer**: H2/PostgreSQL with FHIR resource storage

### Hexagonal Architecture Elements
- **Adapters**: FHIR version-specific configurations
- **Ports**: Interceptor interfaces, provider contracts
- **Core Domain**: FHIR resource management and clinical operations

## Design Patterns Implementation

### Configuration Patterns

#### Conditional Configuration
- **@ConditionalOnProperty**: Feature toggles for modules
- **Custom Conditions**: Version-specific configurations
- **Profile-based Configuration**: Environment-specific setups

Examples:
```java
@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir", name = "mdm_enabled", havingValue = "true")
public class MdmConfig {
    // MDM configuration only when enabled
}
```

#### Factory Pattern
- **IRepositoryValidationInterceptorFactory**: Version-specific validation
- **Channel Factory**: Message broker implementations
- **Provider Factories**: Dynamic provider registration

### Spring Framework Patterns

#### Dependency Injection
- **Constructor Injection**: Primary DI mechanism
- **Configuration Properties**: `@ConfigurationProperties` binding
- **Bean Registration**: Programmatic and annotation-based

#### Aspect-Oriented Programming (AOP)
- **Interceptors**: Cross-cutting concerns (logging, security, validation)
- **Transaction Management**: Declarative transaction handling
- **Monitoring**: Metrics collection and health checks

### Data Access Patterns

#### Repository Pattern
- **JPA Repositories**: Abstracted data access
- **Custom Repositories**: FHIR-specific query methods
- **Entity Management**: Hibernate-based persistence

#### Unit of Work
- **Transaction Boundaries**: Service-level transaction management
- **Batch Operations**: Bulk resource processing
- **Rollback Strategies**: Error handling and recovery

### Integration Patterns

#### Interceptor Chain
- **Pre/Post Processing**: Request/response interceptors
- **Validation Pipeline**: Multi-stage validation
- **Authorization Chain**: Security interceptor stack

#### Observer Pattern
- **Subscription System**: FHIR subscription notifications
- **Event Publishing**: Resource change events
- **Webhook Notifications**: External system integration

#### Template Method
- **FHIR Version Handlers**: Common operations with version-specific implementations
- **Configuration Templates**: Base configurations with specializations
- **Operation Providers**: Standardized operation patterns

## Modularity Patterns

### Plugin Architecture
- **Provider Registration**: Dynamic provider loading
- **Interceptor Registration**: Runtime interceptor addition
- **Implementation Guide Loading**: Runtime IG installation

### Feature Toggle Pattern
- **Configuration-Driven Features**: Enable/disable via properties
- **Conditional Bean Creation**: Feature-specific components
- **Graceful Degradation**: Fallback when features disabled

## Concurrency Patterns

### Thread Safety
- **Immutable Configuration**: Read-only configuration objects
- **Stateless Services**: Thread-safe service implementations
- **Connection Pooling**: Shared database connections

### Asynchronous Processing
- **Subscription Processing**: Async notification handling
- **Background Tasks**: Long-running operations
- **Message Queues**: Decoupled processing (configurable)

## Error Handling Patterns

### Exception Translation
- **FHIR Exception Mapping**: HTTP status code translation
- **Validation Error Aggregation**: Multiple validation failures
- **Graceful Degradation**: Fallback behaviors

### Circuit Breaker (Implicit)
- **Database Connection Handling**: Connection pool limits
- **External Service Integration**: Timeout configurations
- **Health Check Integration**: Service availability monitoring

## Security Patterns

### Interceptor-Based Security
- **Authentication Interceptors**: Request authentication
- **Authorization Interceptors**: Permission checking
- **Audit Logging**: Security event tracking

### Configuration-Based Security
- **Property-Driven Settings**: Security feature toggles
- **Environment-Specific Security**: Different security per environment
- **Extensible Security**: Custom interceptor registration

## Scalability Patterns

### Horizontal Scaling
- **Stateless Design**: No session state in server
- **Database Sharing**: Multiple instances, shared database
- **Load Balancer Ready**: Session-independent operations

### Caching Strategies
- **Search Result Caching**: Configurable cache duration
- **Metadata Caching**: FHIR capability statements
- **Terminology Caching**: Value set and code system caching

### Resource Optimization
- **Lazy Loading**: On-demand resource loading
- **Pagination**: Large result set handling
- **Projection**: Minimal resource representation

## Monitoring & Observability Patterns

### Metrics Collection
- **Micrometer Integration**: Application metrics
- **Prometheus Export**: Time-series metrics
- **Custom Metrics**: Domain-specific measurements

### Health Checks
- **Spring Actuator**: Built-in health endpoints
- **Database Health**: Connection and query health
- **Dependency Health**: External service availability

### Distributed Tracing
- **OpenTelemetry Support**: Request tracing capability
- **Correlation IDs**: Request tracking across services
- **Performance Monitoring**: Response time tracking

## Testing Patterns

### Test Double Patterns
- **Testcontainers**: Real database testing
- **Spring Boot Test**: Integration test framework
- **Mock Services**: External dependency mocking

### Test Organization
- **Unit Tests**: Single component testing
- **Integration Tests**: Full stack testing
- **Smoke Tests**: Basic functionality validation

## Data Flow Patterns

### Request/Response Pipeline
```
HTTP Request → Security → Validation → Business Logic → Data Access → Response
```

### Subscription Flow
```
Resource Change → Subscription Matching → Notification Processing → Delivery
```

### Clinical Reasoning Flow
```
FHIR Operation → CQL Evaluation → Library Execution → Result Transformation
```

## Anti-Patterns Avoided

- **God Object**: Configuration split into focused classes
- **Singleton Abuse**: Spring container manages singletons
- **Hardcoded Dependencies**: Configuration-driven dependencies
- **Monolithic Configuration**: Modular configuration structure
- **Database Vendor Lock-in**: Abstract database dialects