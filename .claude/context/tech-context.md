---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-28T15:58:42Z
version: 1.0
author: Claude Code PM System
---

# Technical Context

## Core Technology Stack

### Platform & Runtime
- **Java Version**: 17 (LTS)
- **Build Tool**: Maven 3.8.3+
- **Application Framework**: Spring Boot 3.x
- **Web Container**: Embedded Tomcat (default) / Jetty (optional)
- **Packaging**: WAR file (`ROOT.war`)

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

#### Database Support
- **Primary**: H2 Database (embedded, in-memory for development)
- **Production Options**:
  - PostgreSQL with `org.postgresql:postgresql`
  - Microsoft SQL Server with `com.microsoft.sqlserver:mssql-jdbc`
- **Connection Pooling**: HikariCP 5.0.1
- **ORM**: Hibernate JPA with custom HAPI FHIR dialects

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

### Container & Deployment

#### Docker
- **Multi-stage Dockerfile** with JDK 17
- **Distroless variant** using `gcr.io/distroless/java-debian10:11`
- **Docker Compose** with PostgreSQL integration
- **OpenTelemetry Agent** - Included for observability

#### Kubernetes
- **Helm Charts** - Complete Kubernetes deployment
- **Service Monitor** - Prometheus metrics collection
- **Ingress Support** - Traffic routing configuration
- **Pod Disruption Budget** - High availability

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