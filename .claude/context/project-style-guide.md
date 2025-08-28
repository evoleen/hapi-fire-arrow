---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-28T15:58:42Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Code Style Standards

### Java Coding Standards

#### General Principles
- **Follow Oracle Java Conventions**: Use standard Java naming and style conventions
- **Consistency**: Maintain consistent style throughout the codebase
- **Readability**: Prioritize code readability and maintainability
- **HAPI FHIR Standards**: Follow established HAPI FHIR project conventions

#### Naming Conventions

**Classes:**
- **Configuration Classes**: `*Config.java` (e.g., `StarterJpaConfig.java`)
- **Properties Classes**: `*Properties.java` (e.g., `AppProperties.java`, `CrProperties.java`)
- **Condition Classes**: `*Condition.java` (e.g., `CrConfigCondition.java`)
- **Interceptor Classes**: `*Interceptor.java` (e.g., `ValidationInterceptor.java`)
- **Provider Classes**: `*Provider.java` (e.g., `PatientProvider.java`)

**Methods:**
- **Camel Case**: `getUserById()`, `validateResource()`
- **Boolean Methods**: Start with `is`, `has`, `can` (e.g., `isEnabled()`, `hasPermission()`)
- **Configuration Methods**: `configure*()` (e.g., `configureValidation()`)

**Variables:**
- **Camel Case**: `patientResource`, `validationResult`
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_PAGE_SIZE`, `MAX_RETRY_COUNT`)
- **Spring Beans**: Match class name with lowercase first letter

**Packages:**
- **Base Package**: `ca.uhn.fhir.jpa.starter`
- **Feature Packages**: `ca.uhn.fhir.jpa.starter.{feature}` (e.g., `cdshooks`, `cr`, `mdm`)
- **Common Utilities**: `ca.uhn.fhir.jpa.starter.common`
- **Web Controllers**: `ca.uhn.fhir.jpa.starter.web`

#### Code Structure

**Class Organization:**
1. Static variables
2. Instance variables
3. Constructors
4. Public methods
5. Protected methods  
6. Private methods
7. Static methods

**Method Structure:**
- **Single Responsibility**: Each method should have one clear purpose
- **Parameter Validation**: Validate inputs at method entry
- **Error Handling**: Use appropriate exception types with clear messages
- **Documentation**: JavaDoc for public methods and complex logic

### Spring Framework Conventions

#### Configuration Classes
```java
@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir", name = "feature_enabled")
public class FeatureConfig {
    
    @Bean
    @ConditionalOnMissingBean
    public FeatureService featureService() {
        return new DefaultFeatureService();
    }
}
```

#### Properties Classes
```java
@ConfigurationProperties(prefix = "hapi.fhir.feature")
@Data
public class FeatureProperties {
    private boolean enabled = false;
    private String endpoint = "http://localhost:8080";
    private Duration timeout = Duration.ofSeconds(30);
}
```

#### Service Classes
```java
@Service
public class PatientService {
    
    private final IGenericClient fhirClient;
    
    public PatientService(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }
    
    @Transactional
    public Patient createPatient(Patient patient) {
        // Implementation
    }
}
```

### FHIR-Specific Conventions

#### Resource Handling
- **Use HAPI FHIR Types**: Prefer HAPI FHIR resource types over custom objects
- **Validation**: Always validate FHIR resources before processing
- **Version Handling**: Explicitly handle FHIR version differences
- **Error Responses**: Use appropriate FHIR OperationOutcome for errors

#### Interceptor Implementation
```java
@Interceptor
@Component
public class CustomInterceptor {
    
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public void preProcess(ServletRequestDetails theRequest, HttpServletRequest theServletRequest) {
        // Implementation
    }
}
```

## Configuration Standards

### Application Configuration

#### YAML Structure
```yaml
# Group related settings
hapi:
  fhir:
    # Core server settings
    server_name: "HAPI Fire Arrow"
    server_version: "8.4.0-2"
    
    # Feature toggles
    mdm_enabled: false
    cr_enabled: true
    subscription:
      enabled: true
      resthook_enabled: true
      
    # Database configuration
    datasource:
      url: "jdbc:h2:mem:test_mem"
      driver_class_name: "org.h2.Driver"
```

#### Property Naming
- **Lowercase with underscores**: `server_name`, `max_page_size`
- **Hierarchical structure**: Use nested properties for related settings
- **Boolean properties**: Use `_enabled` suffix consistently
- **Duration properties**: Specify units clearly (e.g., `timeout_seconds`)

### Environment-Specific Configuration
- **application.yaml**: Default/development configuration
- **application-prod.yaml**: Production overrides
- **application-test.yaml**: Test-specific settings
- **Environment Variables**: Override via `HAPI_FHIR_*` environment variables

## Testing Standards

### Test Structure

#### Test Class Organization
```java
class PatientServiceTest {
    
    @Mock
    private IGenericClient mockClient;
    
    @InjectMocks
    private PatientService patientService;
    
    @BeforeEach
    void setUp() {
        // Test setup
    }
    
    @Test
    void shouldCreatePatient_whenValidInput() {
        // Given
        Patient patient = new Patient();
        
        // When
        Patient result = patientService.createPatient(patient);
        
        // Then
        assertThat(result).isNotNull();
    }
}
```

#### Test Naming
- **Test Classes**: `*Test.java` for unit tests, `*IT.java` for integration tests
- **Test Methods**: `should{ExpectedResult}_when{Condition}` format
- **Descriptive Names**: Clear description of what is being tested

#### Test Categories
- **Unit Tests**: Test individual classes in isolation
- **Integration Tests**: Test complete workflows with real dependencies  
- **Smoke Tests**: Basic functionality validation
- **Performance Tests**: Load and performance validation

### Test Data Management
- **FHIR Resources**: Use standard FHIR test data from `src/test/resources/`
- **Test Builders**: Create builder classes for complex test objects
- **Data Isolation**: Each test should be independent and idempotent
- **Realistic Data**: Use clinically realistic test data when possible

## Documentation Standards

### Code Documentation

#### JavaDoc Standards
```java
/**
 * Service for managing patient resources and operations.
 * 
 * <p>This service provides CRUD operations for FHIR Patient resources
 * and handles patient identity resolution via MDM integration.</p>
 * 
 * @author HAPI FHIR Team
 * @since 8.4.0
 */
public class PatientService {
    
    /**
     * Creates a new patient resource with validation.
     * 
     * @param patient the patient resource to create
     * @return the created patient with server-assigned ID
     * @throws FhirException if validation fails
     */
    public Patient createPatient(Patient patient) {
        // Implementation
    }
}
```

#### Inline Comments
- **Complex Logic**: Explain non-obvious business logic
- **FHIR Specifics**: Document FHIR specification requirements
- **TODOs**: Use `TODO:` prefix for future improvements
- **Workarounds**: Document temporary solutions with explanation

### Configuration Documentation
- **Property Comments**: Document each configuration property
- **Examples**: Provide usage examples for complex configurations
- **Defaults**: Document default values and their rationale
- **Dependencies**: Note when properties depend on other settings

## File Organization Standards

### Directory Structure
```
src/main/java/ca/uhn/fhir/jpa/starter/
├── annotations/          # Custom annotations
├── common/              # Shared configurations and utilities
├── {feature}/           # Feature-specific packages (cdshooks, cr, mdm)
├── web/                 # Web controllers and configuration
├── AppProperties.java   # Main application properties
└── Application.java     # Spring Boot application entry point
```

### File Naming
- **Configuration**: `*Config.java`
- **Properties**: `*Properties.java`  
- **Tests**: `*Test.java` or `*IT.java`
- **Resources**: Descriptive names with appropriate extensions
- **Templates**: `*.html` for Thymeleaf templates

### Resource Organization
```
src/main/resources/
├── application.yaml     # Main configuration
├── logback.xml         # Logging configuration
├── mdm-rules.json      # MDM matching rules
└── static/             # Static web resources
```

## Version Control Standards

### Commit Message Format
```
feat: add CDS Hooks integration for clinical decision support

- Implement CDS Hooks servlet and configuration
- Add support for order-sign and patient-view hooks
- Include comprehensive integration tests
- Update documentation with configuration examples

Fixes #123
```

### Commit Types
- **feat**: New features
- **fix**: Bug fixes
- **docs**: Documentation changes
- **style**: Code style changes
- **refactor**: Code refactoring
- **test**: Test additions or changes
- **chore**: Build or auxiliary tool changes

### Branch Naming
- **Feature branches**: `feature/description` or `{issue-number}-description`
- **Bug fixes**: `fix/description` or `hotfix/description`
- **Releases**: `release/version-number`
- **Hotfixes**: `hotfix/version-number`

### Code Review Standards
- **Small Changes**: Keep pull requests focused and reviewable
- **Test Coverage**: Include tests for new functionality
- **Documentation**: Update relevant documentation
- **Breaking Changes**: Clearly document any breaking changes
- **Security Review**: Flag security-sensitive changes for additional review

## Performance Guidelines

### Database Operations
- **Batch Operations**: Use batch processing for bulk operations
- **Query Optimization**: Optimize database queries and indexes
- **Connection Pooling**: Configure appropriate connection pool sizes
- **Transaction Boundaries**: Keep transactions as short as possible

### Memory Management
- **Resource Cleanup**: Properly close resources and clear references
- **Caching Strategy**: Use appropriate caching for frequently accessed data
- **Lazy Loading**: Load data on-demand when possible
- **Memory Profiling**: Regular memory usage monitoring

### API Design
- **Pagination**: Implement pagination for large result sets
- **Response Size**: Minimize response payload size
- **Caching Headers**: Use appropriate HTTP caching headers
- **Async Processing**: Use asynchronous processing for long-running operations