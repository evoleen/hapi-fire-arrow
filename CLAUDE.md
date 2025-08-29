# CLAUDE.md

## PROJECT LOCATION AND CONSTRAINTS

**Project Directory**: `/Users/till/Development/hapi-fire-arrow`

**Critical Constraint**: ALL file edits and operations must be performed within this directory or its subdirectories. No edits outside this path are permitted.

> Think carefully and implement the most concise solution that changes as little code as possible.

## FILE OPERATIONS REQUIREMENT

**CRITICAL**: ALL file read/write activities MUST be performed via the mcp-text-editor MCP server. This is a mandatory requirement that overrides any other file operation methods.

### File Operation Rules
- Use mcp-text-editor for ALL file reads, writes, and edits
- No exceptions to this rule - all file I/O goes through mcp-text-editor
- This ensures proper file handling, permissions, and consistency
- The mcp-text-editor provides optimized file operations for this environment

## USE SUB-AGENTS FOR CONTEXT OPTIMIZATION

### 1. Always use the file-analyzer sub-agent when asked to read files.
The file-analyzer agent is an expert in extracting and summarizing critical information from files, particularly log files and verbose outputs. It provides concise, actionable summaries that preserve essential information while dramatically reducing context usage.

### 2. Always use the code-analyzer sub-agent when asked to search code, analyze code, research bugs, or trace logic flow.

The code-analyzer agent is an expert in code analysis, logic tracing, and vulnerability detection. It provides concise, actionable summaries that preserve essential information while dramatically reducing context usage.

### 3. Always use the test-runner sub-agent to run tests and analyze the test results.

Using the test-runner agent ensures:

- Full test output is captured for debugging
- Main conversation stays clean and focused
- Context usage is optimized
- All issues are properly surfaced
- No approval dialogs interrupt the workflow

## Philosophy

### Error Handling

- **Fail fast** for critical configuration (missing text model)
- **Log and continue** for optional features (extraction model)
- **Graceful degradation** when external services unavailable
- **User-friendly messages** through resilience layer

### Testing

- Always use the test-runner agent to execute tests.
- Do not use mock services for anything ever.
- Do not move on to the next test until the current test is complete.
- If the test fails, consider checking if the test is structured correctly before deciding we need to refactor the codebase.
- Tests to be verbose so we can use them for debugging.


## Tone and Behavior

- Criticism is welcome. Please tell me when I am wrong or mistaken, or even when you think I might be wrong or mistaken.
- Please tell me if there is a better approach than the one I am taking.
- Please tell me if there is a relevant standard or convention that I appear to be unaware of.
- Be skeptical.
- Be concise.
- Short summaries are OK, but don't give an extended breakdown unless we are working through the details of a plan.
- Do not flatter, and do not give compliments unless I am specifically asking for your judgement.
- Occasional pleasantries are fine.
- Feel free to ask many questions. If you are in doubt of my intent, don't guess. Ask.

## PROJECT PACKAGE STRUCTURE

### Custom Evoleen Code Location
All custom Evoleen authentication and security code is organized under the `com.evoleen.hapi.faserver` package structure to maintain clear separation from upstream HAPI FHIR code:

**com.evoleen.hapi.faserver.auth**
- AuthConfigurationProperties.java - OAuth provider configurations  
- AuthProvider.java - Base interface for auth providers
- AuthProviderManager.java - Manages multiple auth providers
- OAuthProvider.java - Standard OAuth/OIDC implementation
- AzureIdentityProvider.java - Azure Identity SDK implementation
- AuthProviderConfig.java - Configuration classes and claim mapping

**com.evoleen.hapi.faserver.security**  
- UserIdentity.java - User identity with FHIR-specific claims
- JwtValidationResult.java - JWT validation result container
- JwtTokenValidator.java - JWT validation logic with caching
- TokenClaimExtractor.java - Extracts user claims from JWT
- JwtValidationException.java - Custom validation exception
- SecurityConfig.java - Spring Security configuration
- OAuth2ResourceServerConfig.java - OAuth2 resource server setup
- AzureIdentityConfiguration.java - Azure-specific configuration
- CustomAuthenticationEntryPoint.java - Custom auth entry points
- JwtAccessDeniedHandler.java - JWT access denied handling
- JwtAuthenticationEntryPoint.java - JWT authentication entry point

**com.evoleen.hapi.faserver.interceptors**
- AuthenticationInterceptor.java - HAPI FHIR authentication interceptor
- AuthorizationInterceptor.java - HAPI FHIR authorization interceptor  
- InterceptorConfig.java - Registers interceptors with FHIR server

**com.evoleen.hapi.faserver.config**
- JwtConfiguration.java - JWT-related Spring configuration

### Base HAPI FHIR Code Location
Original HAPI FHIR starter code remains in `ca.uhn.fhir.jpa.starter` packages and should not be modified to prevent merge conflicts with upstream updates.

### Package Separation Benefits
- Prevents merge conflicts when updating HAPI FHIR dependencies
- Clear separation between custom Evoleen features and base FHIR functionality  
- Easier maintenance and future development
- Follows proper Java package naming conventions

## ABSOLUTE RULES:

- NO PARTIAL IMPLEMENTATION
- NO SIMPLIFICATION : no "//This is simplified stuff for now, complete implementation would blablabla"
- NO CODE DUPLICATION : check existing codebase to reuse functions and constants Read files before writing new functions. Use common sense function name to find them easily.
- NO DEAD CODE : either use or delete from codebase completely
- IMPLEMENT TEST FOR EVERY FUNCTIONS
- NO CHEATER TESTS : test must be accurate, reflect real usage and be designed to reveal flaws. No useless tests! Design tests to be verbose so we can use them for debuging.
- NO INCONSISTENT NAMING - read existing codebase naming patterns.
- NO OVER-ENGINEERING - Don't add unnecessary abstractions, factory patterns, or middleware when simple functions would work. Don't think "enterprise" when you need "working"
- NO MIXED CONCERNS - Don't put validation logic inside API handlers, database queries inside UI components, etc. instead of proper separation
- NO RESOURCE LEAKS - Don't forget to close database connections, clear timeouts, remove event listeners, or clean up file handles