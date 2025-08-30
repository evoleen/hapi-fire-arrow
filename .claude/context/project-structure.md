---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-30T07:36:48Z
version: 1.3
author: Claude Code PM System
---

# Project Structure

## Root Directory Organization

```
hapi-fire-arrow/
├── .claude/                    # Claude Code configuration and project management
├── charts/                     # Helm charts for Kubernetes deployment
├── configs/                    # Configuration files for various environments
├── custom/                     # Custom branding and UI assets
├── src/                        # Main source code directory
├── target/                     # Maven build output directory
├── build-docker-image.sh       # Docker image build script
├── docker-compose.yml          # Docker Compose configuration
├── Dockerfile                  # Container definition
├── pom.xml                     # Maven project configuration
└── README.md                   # Project documentation
```

## Claude Configuration Structure

```
.claude/
├── agents/                     # AI agent configurations
│   ├── code-analyzer.md        # Code analysis and bug detection
│   ├── file-analyzer.md        # File content analysis
│   ├── parallel-worker.md      # Multi-threaded coordination
│   └── test-runner.md          # Test execution and analysis
├── commands/                   # Custom command definitions
│   ├── context/               # Context management (create, prime, update)
│   ├── pm/                    # Project management (30+ epic/issue commands)
│   ├── testing/               # Testing infrastructure
│   ├── code-rabbit.md         # Code analysis workflow
│   ├── prompt.md              # Prompt management
│   └── re-init.md             # Re-initialization command
├── context/                   # Project context documentation (9 files)
├── epics/                     # Epic management with new files
├── hooks/                     # Git and workflow hooks with automation
├── prds/                      # Product requirements documentation
├── rules/                     # Development rules (datetime, testing, etc.)
├── scripts/                   # Automation scripts
│   ├── pm/                    # Project management automation
│   └── test-and-log.sh        # Enhanced test execution
├── settings.json.example      # Settings template
├── settings.local.json        # Local configuration
└── CLAUDE.md                  # Development rules and guidelines
```

## Source Code Organization

```
src/
├── main/
│   ├── java/
│   │   ├── ca/uhn/fhir/jpa/starter/       # Base HAPI FHIR code (DO NOT MODIFY)
│   │   │   ├── annotations/               # Custom annotations
│   │   │   ├── cdshooks/                  # CDS Hooks implementation
│   │   │   ├── common/                    # Common configurations
│   │   │   ├── cr/                        # Clinical Reasoning module
│   │   │   ├── ig/                        # Implementation Guide support
│   │   │   ├── ips/                       # International Patient Summary
│   │   │   ├── mdm/                       # Master Data Management
│   │   │   ├── terminology/               # Terminology services
│   │   │   ├── util/                      # Utility classes
│   │   │   ├── web/                       # Web controllers and configuration
│   │   │   ├── AppProperties.java         # Application properties
│   │   │   └── Application.java           # Main Spring Boot application
│   │   └── com/evoleen/hapi/faserver/     # Fire Arrow custom code (ALL custom development)
│   │       ├── auth/                      # Authentication providers and configuration
│   │       │   ├── AuthConfigurationProperties.java
│   │       │   ├── AuthProvider.java          # Base auth provider interface
│   │       │   ├── AuthProviderConfig.java    # Configuration classes
│   │       │   ├── AuthProviderFactory.java   # Provider factory
│   │       │   ├── AuthProviderManager.java   # Provider management
│   │       │   ├── AzureIdentityProvider.java # Azure Identity SDK integration
│   │       │   ├── AzureIdentityProviderConfig.java
│   │       │   ├── OAuthProvider.java         # Standard OAuth/OIDC implementation
│   │       │   └── OAuthProviderConfig.java   # OAuth configuration
│   │       ├── config/                    # Spring configuration
│   │       │   └── JwtConfiguration.java      # JWT-related Spring configuration
│   │       ├── interceptors/              # HAPI FHIR interceptors
│   │       │   ├── AuthenticationInterceptor.java
│   │       │   ├── AuthorizationInterceptor.java
│   │       │   └── InterceptorConfig.java     # Interceptor registration
│   │       └── security/                  # Security infrastructure
│   │           ├── AuthenticationEntryPoint.java
│   │           ├── AzureIdentityConfiguration.java
│   │           ├── CustomAuthenticationEntryPoint.java
│   │           ├── JwtAccessDeniedHandler.java
│   │           ├── JwtAuthenticationEntryPoint.java
│   │           ├── JwtTokenValidator.java     # JWT validation with caching
│   │           ├── JwtValidationException.java
│   │           ├── JwtValidationResult.java   # Validation result container
│   │           ├── OAuth2ResourceServerConfig.java
│   │           ├── SecurityConfig.java        # Spring Security configuration
│   │           ├── TokenClaimExtractor.java   # JWT claim extraction
│   │           └── UserIdentity.java          # User identity with FHIR claims
│   ├── resources/                     # Resource files
│   │   ├── application.yaml           # Main configuration
│   │   ├── cds.application.yaml       # CDS-specific configuration
│   │   ├── logback.xml               # Logging configuration
│   │   └── mdm-rules.json            # MDM matching rules
│   └── webapp/                        # Web application assets
│       ├── WEB-INF/templates/         # Thymeleaf templates
│       ├── img/                       # Images and icons
│       └── js/                        # JavaScript files
└── test/                              # Test code
    ├── java/                          # Java test classes (mirrors package structure)
    │   ├── ca/uhn/fhir/jpa/starter/   # Base HAPI FHIR tests
    │   └── com/evoleen/hapi/faserver/ # Fire Arrow custom tests
    │       ├── config/                # Test configuration infrastructure
    │       │   ├── BaseAuthenticationTest.java    # Abstract base for auth tests
    │       │   ├── TestConfiguration.java         # Test Spring configuration
    │       │   └── TestConfigurationProperties.java # Test config properties
    │       ├── interceptors/          # Interceptor tests
    │       ├── security/              # Security component tests
    │       └── util/                  # Test utilities
    │           └── JwtTestUtils.java              # JWT token generation utilities
    ├── resources/                     # Test resources
    │   ├── application-test.yaml      # Test-specific Spring Boot configuration
    │   ├── dstu3/                     # FHIR DSTU3 test data
    │   └── r4/                        # FHIR R4 test data
    └── smoketest/                     # Smoke test configuration
```

## Package Structure - Fire Arrow vs Base HAPI

### Fire Arrow Custom Code (`com.evoleen.hapi.faserver.*`)
**All new custom development MUST use this package hierarchy:**

- `com.evoleen.hapi.faserver.auth.*` - Authentication providers and configuration
- `com.evoleen.hapi.faserver.security.*` - Security infrastructure and JWT handling  
- `com.evoleen.hapi.faserver.interceptors.*` - HAPI FHIR interceptors
- `com.evoleen.hapi.faserver.config.*` - Spring configuration classes

### Base HAPI FHIR Code (`ca.uhn.fhir.jpa.starter.*`)
**Never modify - upstream synchronization required:**

- `ca.uhn.fhir.jpa.starter.*` - Original HAPI FHIR starter code
- All base FHIR server functionality
- HAPI FHIR framework integration points
- Original configuration and bootstrap classes

### Package Separation Benefits

1. **Upstream Compatibility**: Clean separation enables seamless HAPI FHIR updates
2. **Merge Conflict Prevention**: No conflicts when syncing with upstream repository
3. **Clear Ownership**: Fire Arrow features clearly distinguished from base functionality
4. **Testing Isolation**: Custom tests separate from base HAPI FHIR tests
5. **Development Focus**: All custom work confined to well-defined package space

## Key Module Organization

### Core Application Modules
- **Application.java**: Main Spring Boot entry point
- **AppProperties.java**: Configuration properties binding
- **FhirServerConfig\***: FHIR version-specific configurations (DSTU2, DSTU3, R4, R4B, R5)

### Feature Modules
- **cdshooks/**: Clinical Decision Support Hooks implementation
- **cr/**: Clinical Reasoning (CQL) module integration
- **ig/**: Implementation Guide runtime loading and management
- **mdm/**: Master Data Management for patient record linking
- **terminology/**: Terminology server capabilities

### Infrastructure Modules
- **common/**: Shared configurations and utilities
- **validation/**: FHIR resource validation interceptors
- **web/**: Web layer configurations and controllers

### Fire Arrow Authentication Modules
- **auth/**: OAuth provider architecture and management
- **security/**: JWT validation, Spring Security, and access control
- **interceptors/**: HAPI FHIR server integration points
- **config/**: Spring Boot auto-configuration for authentication

## File Naming Conventions

### Java Classes
- **Config classes**: `*Config.java` (e.g., `StarterJpaConfig.java`)
- **Properties classes**: `*Properties.java` (e.g., `CrProperties.java`)
- **Condition classes**: `*Condition.java` (e.g., `CrConfigCondition.java`)
- **Test classes**: `*IT.java` for integration tests, `*Test.java` for unit tests

### Configuration Files
- **YAML configs**: `*.yaml` or `*.yml`
- **Properties**: `*.properties`
- **JSON configs**: `*.json` (e.g., `mdm-rules.json`)

### Web Resources
- **Templates**: Located in `src/main/webapp/WEB-INF/templates/`
- **Static assets**: Images in `img/`, JavaScript in `js/`
- **Custom branding**: Stored in `custom/` directory

## Deployment Structure

### Container Deployment
- **Dockerfile**: Multi-stage build with optimized layers
- **docker-compose.yml**: Development environment with PostgreSQL
- **build-docker-image.sh**: Automated image building

### Kubernetes Deployment
- **charts/hapi-fhir-jpaserver/**: Complete Helm chart
- **templates/**: Kubernetes resource templates
- **values.yaml**: Default configuration values
- **ci/**: CI/CD specific value files

## Build Artifacts

### Maven Output (target/)
- **ROOT.war**: Deployable WAR file
- **classes/**: Compiled Java classes
- **test-classes/**: Compiled test classes
- **Generated resources**: Configuration files processed by Maven

### Key Build Files
- **pom.xml**: Maven project definition with dependencies
- **profiles**: Boot, Jetty, and cloud-specific profiles

## Development Standards

### Smoke Testing Requirements
All changes must pass complete smoke tests:
1. Full test suite execution
2. Maven build validation  
3. Server startup monitoring (minimum 40 seconds)
4. Production command: `mvn clean package spring-boot:repackage -DskipTests=true -Pboot && java -jar target/ROOT.war`

### Code Organization Rules
- **NEW CODE**: Always use `com.evoleen.hapi.faserver.*` packages
- **BASE CODE**: Never modify `ca.uhn.fhir.jpa.starter.*` packages
- **TESTING**: Mirror package structure in test directory
- **SEPARATION**: Maintain strict separation for upstream compatibility

## Update History
- 2025-08-30T07:36:48Z: Added comprehensive test infrastructure files - BaseAuthenticationTest, TestConfigurationProperties, JwtTestUtils, application-test.yaml. Updated timeout requirements to 40 seconds.