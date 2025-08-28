---
issue: 8
title: Server Foundation and Branding
analyzed: 2025-08-28T15:32:40Z
estimated_hours: 6
parallelization_factor: 2.5
---

# Parallel Work Analysis: Issue #8

## Overview
Establish foundational server infrastructure and implement custom branding for the HAPI FHIR starter application. This involves modifying server configuration, removing/updating tester options, implementing custom branding elements, and ensuring proper server initialization.

## Parallel Streams

### Stream A: Configuration and Server Foundation
**Scope**: Server configuration, tester configuration removal, and health endpoints setup
**Files**:
- `src/main/resources/application.yaml`
- `src/main/java/ca/uhn/fhir/jpa/starter/common/FhirTesterConfig.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/Application.java`
- `src/main/resources/logback.xml`
**Agent Type**: backend-specialist
**Can Start**: immediately
**Estimated Hours**: 3
**Dependencies**: none

### Stream B: Web Interface and Branding
**Scope**: Custom branding, UI updates, asset integration, and template modifications
**Files**:
- `src/main/webapp/WEB-INF/templates/tmpl-home-welcome.html`
- `src/main/webapp/WEB-INF/templates/tmpl-banner.html`
- `src/main/webapp/WEB-INF/templates/tmpl-footer.html`
- `src/main/webapp/img/favicon.ico`
- `src/main/webapp/img/sample-logo.jpg`
- `custom/welcome.html`
- `custom/logo.jpg`
- `custom/about.html`
**Agent Type**: frontend-specialist
**Can Start**: immediately
**Estimated Hours**: 2.5
**Dependencies**: none

### Stream C: Integration and Testing
**Scope**: Integration testing, health check validation, and cross-browser compatibility
**Files**:
- Test files validation
- Server startup verification
- Health endpoint testing
**Agent Type**: fullstack-specialist
**Can Start**: after Stream A and B are 80% complete
**Estimated Hours**: 1.5
**Dependencies**: Stream A (for server config), Stream B (for UI validation)

## Coordination Points

### Shared Files
None - streams work on completely different file sets

### Sequential Requirements
1. Server configuration must be completed before integration testing
2. Branding assets must be in place before UI validation
3. Both server foundation and branding must be ready for final integration testing

## Conflict Risk Assessment
- **Low Risk**: Streams work on completely different directories and file types
- **No Overlapping Files**: Configuration (Stream A) and web interface (Stream B) are entirely separate
- **Minimal Coordination Needed**: Only timing coordination for final integration testing

## Parallelization Strategy

**Recommended Approach**: parallel

Launch Streams A and B simultaneously as they have no dependencies and work on completely separate file sets. Start Stream C when both A and B are approximately 80% complete to perform integration testing and validation.

## Expected Timeline

With parallel execution:
- Wall time: 3 hours (max of Stream A)
- Total work: 7 hours
- Efficiency gain: 57%

Without parallel execution:
- Wall time: 7 hours

## Notes

**Key Implementation Details:**
- Stream A focuses on removing "global tester" and making "local tester" default while hiding it from UI
- Stream A must update application.yaml to change "tester" references to "fire_arrow_server"
- Stream B implements custom branding elements and integrates existing custom assets
- Stream C validates that server starts properly, health endpoints work, and branding appears correctly

**Specific Configuration Changes:**
- Remove `tester.global` configuration from application.yaml
- Update server name/identifier from "tester" to "fire_arrow_server" 
- Ensure `tester.home.name: "Local Tester"` is set as default but hidden from UI
- Implement custom favicon, logos, and application title updates

**Testing Priorities:**
- Server startup with custom configuration
- Health endpoints functionality (/actuator/health)
- Custom branding visibility in web interface
- Absence of global tester option in UI
- Local tester working but not visible to users