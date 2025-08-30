---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-30T07:36:48Z
version: 1.2
author: Claude Code PM System
---

# Technical Context

## Core Technology Stack

### Platform & Runtime
- **Java Version**: 17 (LTS) - Required for Fire Arrow Server
- **Build Tool**: Maven 3.8.3+
- **Application Framework**: Spring Boot 3.2.6 (downgraded from 3.5.5 for dependency compatibility)
- **Web Container**: Embedded Tomcat (default) / Jetty (optional)
- **Packaging**: WAR file (`ROOT.war`)
- **Server Identity**: Fire Arrow Server (branded HAPI FHIR implementation)

### Primary Dependencies

#### HAPI FHIR Framework
- **HAPI FHIR Version**: 8.4.0
- **Core Modules**:
  - `hapi-fhir-base` - Core FHIR functionality
  - `hapi-fhir-jpaserver-base` - JPA server implementation
  - `hapi-fhir-jpaserver-subscription` - Subscription support
  - `hapi-fhir-jpaserver-mdm` - Master Data Management
  - `hapi-fhir-server-cds-hooks` - CDS Hooks implementation
  - `hapi-fhir-server-openapi` - OpenAPI documentation
  - `hapi-fhir-testpage-overlay` - Built-in web UI

#### Clinical Reasoning
- **Clinical Reasoning Version**: 3.26.0
- **Module**: `cqf-fhir-cr-hapi` - Clinical Quality Language (CQL) support
- **Critical Exclusions**: Spring Security dependencies excluded to prevent version conflicts
  - Excluded: `spring-security-core`, `spring-security-crypto`
  - Reason: CQL library brought incompatible Spring Security 5.7.14 vs required 6.2.4

#### Database Support
- **Development**: H2 Database (embedded, in-memory for testing)
- **Production Primary**: PostgreSQL with CNPG (Cloud Native PostgreSQL) orchestration
- **Alternative Production**: Microsoft SQL Server with `com.microsoft.sqlserver:mssql-jdbc`
- **Connection Pooling**: HikariCP 5.0.1
- **ORM**: Hibernate JPA with custom HAPI FHIR dialects
- **Multi-tenant Support**: Tenant isolation capabilities for SaaS deployments

### Spring Boot Ecosystem

#### Core Spring Dependencies
- **Spring Boot Version**: 3.x (managed by parent POM)
- **spring-boot-starter-web** - Web layer
- **spring-boot-starter-actuator** - Management endpoints
- **spring-boot-autoconfigure** - Auto-configuration

#### Monitoring & Metrics
- **Micrometer Core**: 1.13.3
- **Prometheus Registry**: 1.13.3 (with SimplClient)
- **Actuator Endpoints**: Health, metrics, info, Prometheus
- **OpenTelemetry**: Azure OpenTelemetry distribution for Application Insights integration
- **Structured Logging**: JSON-formatted logs for authentication and validation events

### Web & UI Technologies

#### Template Engine
- **Thymeleaf** - Server-side template rendering
- **Web Assets** (WebJars):
  - Bootstrap DateTimePicker (Eonasdan)
  - Font Awesome icons
  - Select2 dropdowns
  - jQuery and Moment.js
  - Awesome Bootstrap Checkbox

#### Email Support
- **SimpleJavaMail** - Email subscription handling (excludes Jakarta annotations)

### Testing Framework

#### Test Dependencies
- **JUnit Jupiter** - Unit testing framework
- **Spring Boot Test Starter** - Integration testing
- **Testcontainers**:
  - Core testcontainers
  - Elasticsearch testcontainers
  - JUnit Jupiter integration
- **Awaitility**: 4.2.0 - Asynchronous testing
- **HAPI Test Utilities**:
  - `hapi-fhir-test-utilities`
  - `hapi-fhir-jpaserver-test-utilities`
- **Elasticsearch Java Client** - Search testing

### Development & Build Tools

#### Maven Plugins
- **Spring Boot Maven Plugin** - Application packaging
- **Maven Compiler Plugin**: 3.13.0 (Java 17)
- **Maven WAR Plugin** - WAR file generation with overlays
- **Maven Surefire Plugin**: 3.4.0 - Unit tests
- **Maven Failsafe Plugin**: 3.4.0 - Integration tests
- **Duplicate Finder Plugin** - Dependency conflict detection

#### Build Profiles
- **boot** (default) - Spring Boot standalone mode
- **jetty** - Jetty web server instead of Tomcat
- **cloudsql-postgres** - Google Cloud SQL integration
- **ossrh-repo** - Maven Central publishing

### Fire Arrow Server Authentication & Access Control

#### OAuth 2.0 Integration
- **Authentication Framework**: OAuth 2.0 token-based authentication
- **Spring Security**: Version 6.2.4 (required for Spring Boot 3.2.6 compatibility)
- **OAuth2 Resource Server**: `spring-security-oauth2-jose` for JWT token validation
- **Token Processing**: JWT token parsing and validation with Nimbus JOSE library
- **Identity Mapping**: Email-based user to FHIR resource identity resolution
- **Resource Types**: Support for Patient, Practitioner, Device, RelatedPerson identities
- **Caching**: Multi-layer caching for identity resolution and validation results

#### Dynamic RBAC Framework
- **Extensible Validator Architecture**: Pluggable request validation system
- **Patient Compartment Validator**: FHIR R4 compartment-based access control
- **Request Processing Pipeline**: Authentication → Identity Resolution → Validation
- **Performance Requirements**: <100ms for cached results, <500ms for uncached
- **Multi-tenant Support**: Tenant isolation with 1000+ concurrent users per tenant

#### Access Control Components
- **Compartment Definitions**: FHIR R4 compartment specification implementation
- **Role-Based Filtering**: Resource filtering based on user roles and relationships
- **Request Validation**: CRUD and search operation validation
- **Cache Management**: Configurable cache invalidation and warming
- **Error Handling**: Structured error responses with appropriate HTTP status codes

### Container & Deployment

#### Docker
- **Multi-stage Dockerfile** with JDK 17
- **Distroless variant** using `gcr.io/distroless/java-debian10:11`
- **Docker Compose** with PostgreSQL integration
- **OpenTelemetry Agent** - Included for observability

#### Kubernetes
- **Helm Charts** - Complete Kubernetes deployment
- **CNPG Integration** - Cloud Native PostgreSQL orchestration
- **FluxCD Ready** - GitOps deployment compatibility
- **Service Monitor** - Prometheus metrics collection
- **Ingress Support** - Traffic routing configuration
- **Pod Disruption Budget** - High availability
- **Multi-tenant Architecture**: Namespace-based tenant isolation

### Security & Quality

#### Security Scanning
- **WhiteSource** - Vulnerability scanning (configured)
- **Dependency Analysis** - Maven dependency plugin

#### Code Quality
- **Duplicate Detection** - Prevents duplicate dependencies
- **Integration Testing** - Comprehensive test coverage
- **Smoke Testing** - HTTP endpoint validation

### External Integrations

#### FHIR Ecosystem
- **FHIR Versions Supported**: DSTU2, DSTU3, R4, R4B, R5
- **Implementation Guides** - Runtime package installation
- **NPM Package Loading** - FHIR resource loading from NPM
- **Terminology Services** - Built-in terminology server

#### Search & Indexing
- **Embedded Lucene** - Default full-text search
- **Elasticsearch** - Optional advanced search (configured via properties)
- **Hibernate Search** - JPA entity indexing

## Development Environment

### Required Tools
- **JDK 17+** - Oracle or OpenJDK
- **Maven 3.8.3+** - Build tool
- **Docker** (optional) - Container development
- **IDE Support** - IntelliJ IDEA, Eclipse, VS Code

### Environment Configuration
- **application.yaml** - Main configuration file
- **Environment Variables** - Docker-friendly configuration
- **Spring Profiles** - Environment-specific settings
- **External Config** - File-based configuration overrides

### Local Development Commands
- **`mvn spring-boot:run`** - Start development server
- **`mvn -Pjetty spring-boot:run`** - Start with Jetty
- **`mvn clean install`** - Full build with tests
- **`docker-compose up`** - Full stack with PostgreSQL

### Project Execution Commands

#### Standard Development
- **`mvn spring-boot:run`** - Start development server
- **`mvn -Pjetty spring-boot:run`** - Start with Jetty
- **`mvn clean install`** - Full build with tests
- **`docker-compose up`** - Full stack with PostgreSQL

#### Production Build and Run
**CRITICAL**: Use ONLY the following command sequence for production builds and server execution:
```bash
mvn clean package spring-boot:repackage -DskipTests=true -Pboot && java -jar target/ROOT.war
```

**Command Breakdown:**
- `mvn clean package` - Clean and build the project
- `spring-boot:repackage` - Create executable WAR with embedded dependencies
- `-DskipTests=true` - Skip test execution for faster builds (tests run separately)
- `-Pboot` - Use Spring Boot profile for standalone execution
- `java -jar target/ROOT.war` - Run the packaged application

**Important Notes:**
- This is the ONLY supported method for running the Fire Arrow server
- Alternative commands may cause configuration or dependency issues
- Server requires minimum 40 seconds monitoring for startup validation (updated from 20s)
- Any deviation from this command sequence may result in production inconsistencies

## Update History
- 2025-08-30T07:36:48Z: Fixed Spring Security version conflicts, updated Spring Boot to 3.2.6, added CQL dependency exclusions, updated server timeout requirements
