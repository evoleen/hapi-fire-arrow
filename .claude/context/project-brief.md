---
created: 2025-08-28T15:58:42Z
last_updated: 2025-08-28T16:54:05Z
version: 1.1
author: Claude Code PM System
---

# Project Brief

## Project Identity

**Project Name**: Fire Arrow Server  
**Repository**: https://github.com/evoleen/hapi-fire-arrow  
**Base Project**: HAPI FHIR JPA Server Starter  
**Current Version**: 8.4.0-2  
**Server Identity**: Fire Arrow Server (branded HAPI FHIR with integrated dynamic RBAC)  

## What It Is

Fire Arrow Server is a unified FHIR server that integrates dynamic Role-Based Access Control (RBAC) capabilities directly into the HAPI FHIR JPA starter project. This eliminates the need for separate facade services, improving performance and simplifying deployment while providing sophisticated healthcare data access controls through OAuth 2.0 authentication and compartment-based authorization.

## Why It Exists

### Primary Purpose
Healthcare systems worldwide struggle with data interoperability - the ability to exchange and use clinical data across different systems, organizations, and platforms. HAPI Fire Arrow solves this by providing:

1. **Standards Compliance**: Full implementation of FHIR R4/R5 specifications
2. **Enterprise Ready**: Production-grade server with scalability and security features
3. **Open Source Foundation**: Cost-effective alternative to proprietary solutions
4. **Comprehensive Features**: Beyond basic FHIR, includes clinical reasoning, quality measures, and advanced data management

### Business Problem Solved
- **Performance Overhead**: Eliminates separate facade service calls for access control
- **Deployment Complexity**: Reduces from multiple services to unified server architecture  
- **Code Maintenance**: Single repository vs. multiple service codebases
- **Single Points of Failure**: Eliminates service chain dependencies
- **Multi-tenant Access Control**: Sophisticated RBAC for SaaS healthcare deployments
- **Healthcare Compliance**: Fine-grained access controls meeting regulatory requirements

## Project Scope

### In Scope

#### Core FHIR Server Functionality
- Complete FHIR REST API implementation (Create, Read, Update, Delete, Search)
- Support for FHIR versions: DSTU2, DSTU3, R4, R4B, R5
- Resource validation and terminology services
- Subscription system for real-time notifications
- Bulk data operations and export capabilities

#### Advanced Healthcare Features
- **Clinical Reasoning (CQL)**: Clinical Quality Language execution
- **CDS Hooks**: Clinical Decision Support integration
- **Master Data Management**: Patient identity resolution and linking  
- **Quality Measures**: Automated clinical quality reporting
- **Implementation Guides**: Runtime loading and validation

#### Enterprise Capabilities
- Scalable multi-instance deployment
- Database support (H2, PostgreSQL, SQL Server)
- Container and Kubernetes deployment
- Monitoring and observability (Prometheus, OpenTelemetry)
- Security framework and audit logging

### Out of Scope
- **Authentication/Authorization Implementation**: Framework provided, specific implementation not included
- **Enterprise Audit Logging**: Basic audit provided, enterprise-grade logging requires additional configuration
- **Multi-instance Shared Caching**: Default implementations are in-memory only
- **Frontend Applications**: Server-side only, UI applications built separately
- **Custom Clinical Workflows**: Generic FHIR server, workflow-specific logic implemented by users

## Key Objectives

### Short-term Goals (Current Development)
1. **Server Branding**: Complete customization and branding features
2. **Claude Integration**: Finalize AI-assisted development workflow
3. **Quality Assurance**: Comprehensive testing and validation
4. **Documentation**: Complete setup and deployment guides

### Medium-term Goals (3-6 months)
1. **Performance Optimization**: Enhanced throughput and response times
2. **Security Hardening**: Production-grade security implementations
3. **Advanced Features**: Extended clinical reasoning capabilities
4. **Integration Examples**: Reference implementations for common use cases

### Long-term Goals (6-12 months)
1. **Cloud-Native Deployment**: Optimized for cloud platforms
2. **Microservices Architecture**: Modular deployment options
3. **Extended Interoperability**: Additional healthcare standards support
4. **Community Ecosystem**: Plugin architecture and developer tools

## Success Criteria

### Technical Success Metrics
- **Performance**: Handle 1000+ concurrent requests with <500ms response time
- **Reliability**: Achieve 99.9% uptime in production deployments
- **Compliance**: Pass all FHIR Connectathon tests and official validation
- **Scalability**: Linear performance scaling across multiple instances

### Business Success Metrics
- **Adoption**: 50+ healthcare organizations using in production
- **Integration**: 100+ successful EHR and system integrations
- **Community**: Active contributor community with regular contributions
- **Documentation**: 90%+ user satisfaction with documentation and support

### User Experience Success Metrics
- **Developer Experience**: <2 hours from download to running server
- **Integration Time**: <1 week for typical EHR integration project
- **Support Quality**: <24 hour average response time for critical issues
- **Feature Completeness**: 95%+ of FHIR specification implemented

## Key Stakeholders

### Development Team
- **Maintainers**: Core HAPI FHIR development team
- **Contributors**: Open source community contributors  
- **Evoleen Team**: Custom enhancements and branding
- **Claude AI**: AI-assisted development and optimization

### End Users
- **Healthcare IT Teams**: Primary implementers and operators
- **Integration Engineers**: System integration specialists
- **Clinical Teams**: End users of integrated systems
- **Compliance Officers**: Healthcare regulation compliance teams

### Business Stakeholders
- **Healthcare Organizations**: Hospitals, clinics, health systems
- **EHR Vendors**: Electronic Health Record system providers
- **HIE Organizations**: Health Information Exchanges
- **Research Institutions**: Clinical research and population health organizations

## Project Constraints

### Technical Constraints
- **Java Platform**: JVM-based, requires Java 17+
- **Database Requirements**: SQL database required for persistence
- **Memory Usage**: Significant memory requirements for large datasets
- **FHIR Compliance**: Must maintain strict FHIR specification compliance

### Business Constraints
- **Open Source License**: Apache 2.0 license requirements
- **Backward Compatibility**: Must support existing HAPI FHIR installations
- **Community Governance**: Changes must align with HAPI FHIR community standards
- **Healthcare Regulations**: Must support HIPAA, GDPR, and other healthcare compliance requirements

### Resource Constraints
- **Development Team Size**: Limited core development team
- **Documentation Resources**: Community-driven documentation efforts
- **Testing Infrastructure**: Relies on community testing and feedback
- **Support Model**: Community-based support with no guaranteed SLAs

## Current Status

**Phase**: Active Development - Server Branding  
**Branch**: `8-server-branding`  
**Last Update**: Recent commits include Claude workflow integration and analysis improvements  
**Next Milestone**: Complete branding features and merge to master branch  

The project is in a stable, production-ready state with ongoing enhancements for customization and developer experience improvements.