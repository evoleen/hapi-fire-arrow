---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-28T16:51:16Z
version: 1.1
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
│   ├── java/ca/uhn/fhir/jpa/starter/  # Main application code
│   │   ├── annotations/                # Custom annotations
│   │   ├── cdshooks/                  # CDS Hooks implementation
│   │   ├── common/                    # Common configurations
│   │   ├── cr/                        # Clinical Reasoning module
│   │   ├── ig/                        # Implementation Guide support
│   │   ├── ips/                       # International Patient Summary
│   │   ├── mdm/                       # Master Data Management
│   │   ├── terminology/               # Terminology services
│   │   ├── util/                      # Utility classes
│   │   ├── web/                       # Web controllers and configuration
│   │   ├── AppProperties.java         # Application properties
│   │   └── Application.java           # Main Spring Boot application
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
    ├── java/                          # Java test classes
    ├── resources/                     # Test resources
    │   ├── dstu3/                     # FHIR DSTU3 test data
    │   └── r4/                        # FHIR R4 test data
    └── smoketest/                     # Smoke test configuration
```

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