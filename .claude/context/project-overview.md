---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-28T15:58:42Z
version: 1.0
author: Claude Code PM System
---

# Project Overview

## High-Level Summary

HAPI Fire Arrow is a comprehensive, production-ready FHIR server that enables healthcare data interoperability through standards-compliant resource management, clinical decision support, and advanced healthcare data operations. Built on the mature HAPI FHIR framework with Java 17 and Spring Boot 3.x, it provides enterprise-grade healthcare data infrastructure with extensive customization capabilities.

## Core Features & Capabilities

### FHIR Server Foundation

#### Multi-Version FHIR Support
- **FHIR R4/R5**: Primary focus on current FHIR versions
- **Legacy Support**: DSTU2, DSTU3, R4B compatibility
- **Version Coexistence**: Multiple FHIR versions on single server instance
- **Seamless Upgrade Path**: Migration tools and compatibility layers

#### Complete REST API
- **CRUD Operations**: Create, Read, Update, Delete for all FHIR resources
- **Advanced Search**: Complex queries with chaining, modifiers, and custom parameters
- **Batch/Transaction**: Bulk operations with atomic transaction support  
- **History & Versioning**: Full resource history tracking and version management
- **Conditional Operations**: Conditional create, update, and delete operations

#### Resource Validation
- **Structure Validation**: Schema and cardinality validation
- **Terminology Validation**: Code system and value set validation
- **Profile Validation**: Implementation Guide profile compliance
- **Custom Rules**: Extensible validation framework

### Advanced Healthcare Features

#### Clinical Reasoning (CQL)
- **CQL Engine**: Clinical Quality Language evaluation
- **Library Management**: CQL library versioning and execution
- **Measure Calculation**: Automated clinical quality measure computation
- **Decision Support**: Rule-based clinical decision logic

#### Clinical Decision Support Hooks
- **CDS Hooks Integration**: SMART on FHIR CDS Hooks implementation
- **Point-of-Care Alerts**: Real-time clinical decision support
- **Workflow Integration**: EHR workflow integration points
- **Custom Services**: Extensible CDS service framework

#### Master Data Management (MDM)
- **Patient Identity Resolution**: Automatic patient record linking
- **Duplicate Detection**: Advanced matching algorithms
- **Golden Record Management**: Consolidated patient records
- **Match Scoring**: Configurable matching rules and thresholds

#### Subscription System
- **Real-time Notifications**: WebSocket and REST Hook subscriptions
- **Email Notifications**: SMTP-based subscription delivery
- **Topic-based Subscriptions**: R5 topic subscription support
- **Flexible Filtering**: Complex subscription criteria

### Enterprise Capabilities

#### Scalability & Performance
- **Horizontal Scaling**: Multi-instance deployment support
- **Connection Pooling**: Optimized database connection management
- **Caching Strategies**: Configurable search result and metadata caching
- **Async Processing**: Background task processing for large operations

#### Database Support
- **H2 Database**: Embedded development database
- **PostgreSQL**: Production-grade relational database
- **SQL Server**: Microsoft SQL Server support
- **Custom Dialects**: HAPI FHIR optimized database dialects

#### Security Framework
- **Interceptor Architecture**: Extensible security interceptor chain
- **Authentication Ready**: Framework for OAuth, SMART on FHIR
- **Authorization Hooks**: Fine-grained permission system
- **Audit Logging**: Comprehensive audit trail capabilities

#### Monitoring & Observability
- **Health Checks**: Spring Actuator health endpoints
- **Metrics Collection**: Micrometer and Prometheus integration
- **Distributed Tracing**: OpenTelemetry support for request tracing
- **Performance Monitoring**: Response time and throughput metrics

### Implementation Guide Support

#### Runtime IG Loading
- **NPM Package Installation**: Install FHIR IGs at runtime via $install operation
- **Profile Validation**: Automatic validation against loaded profiles
- **Terminology Integration**: Value sets and code systems from IGs
- **Version Management**: Multiple IG versions and dependency resolution

#### Custom Extensions
- **Extension Registry**: Custom FHIR extension support
- **Profile Definitions**: Custom resource profiles and constraints
- **Search Parameters**: Custom search parameter definitions
- **Operation Definitions**: Custom FHIR operations

### Terminology Services

#### Built-in Terminology Server
- **Value Set Management**: Create, update, and expand value sets
- **Code System Support**: Comprehensive code system management
- **Concept Maps**: Terminology mapping and translation
- **Validation Services**: Code validation and lookup operations

#### External Terminology Integration
- **Remote Terminology**: Integration with external terminology servers
- **Caching**: Local caching of remote terminology
- **Fallback Strategies**: Graceful degradation when external services unavailable

### Search & Indexing

#### Advanced Search Capabilities
- **Full-text Search**: Lucene-based full-text indexing
- **Elasticsearch Integration**: Optional Elasticsearch backend
- **Complex Queries**: Chained searches, reverse chaining, and modifiers
- **Custom Search Parameters**: Define domain-specific search parameters

#### Performance Optimization
- **Search Result Caching**: Configurable cache duration
- **Pagination**: Efficient handling of large result sets
- **Index Optimization**: Automatic database index management
- **Search Statistics**: Query performance monitoring

## Current Implementation State

### Production Ready Features
- **Core FHIR Operations**: Fully implemented and tested
- **Multi-database Support**: Production deployments on PostgreSQL and SQL Server
- **Container Deployment**: Docker and Kubernetes ready
- **Monitoring Integration**: Prometheus metrics and health checks
- **Basic Security**: Interceptor framework for authentication/authorization

### Advanced Features Available
- **Clinical Reasoning**: CQL evaluation and quality measures
- **CDS Hooks**: Clinical decision support integration
- **MDM**: Patient identity resolution and linking
- **Subscriptions**: Real-time notification system
- **Implementation Guides**: Runtime IG package loading

### Development/Beta Features
- **Enhanced Search**: Advanced Elasticsearch features
- **Performance Optimization**: Ongoing scalability improvements
- **Security Enhancements**: Advanced authentication mechanisms
- **Cloud Integrations**: Cloud-specific optimizations

## Integration Points

### EHR Integration
- **FHIR API**: Standard FHIR REST API for EHR integration
- **SMART on FHIR**: App platform for clinical applications
- **Bulk Data Export**: Large-scale data export capabilities
- **Subscription Notifications**: Real-time data synchronization

### HIE Integration
- **Document Sharing**: FHIR Document reference and retrieval
- **Patient Discovery**: Cross-organizational patient matching
- **Care Coordination**: Shared care plan and encounter data
- **Quality Reporting**: Automated quality measure submission

### Research Platform Integration
- **Bulk Data Access**: Research data export capabilities
- **De-identification**: Data privacy and anonymization hooks
- **Cohort Selection**: Advanced patient population queries
- **Longitudinal Data**: Complete patient timeline access

### Payer Integration
- **Claims Data**: Clinical data enriched claims processing
- **Prior Authorization**: Clinical criteria evaluation
- **Risk Adjustment**: Clinical risk scoring data
- **Quality Bonuses**: Quality measure calculation for value-based care

## Deployment Options

### Traditional Deployment
- **WAR File**: Deploy to Tomcat, Jetty, or other servlet containers
- **Spring Boot JAR**: Standalone executable with embedded server
- **Database Setup**: Configure PostgreSQL or SQL Server backend

### Container Deployment
- **Docker Image**: Official Docker image with multi-stage build
- **Docker Compose**: Development environment with PostgreSQL
- **Kubernetes**: Helm charts for production Kubernetes deployment
- **Cloud Platforms**: AWS, Azure, Google Cloud deployment guides

### Development Environment
- **Local Development**: H2 database for quick setup
- **IDE Integration**: IntelliJ IDEA, Eclipse, VS Code support  
- **Hot Reload**: Spring Boot DevTools for rapid development
- **Test Data**: Comprehensive FHIR test resource collections