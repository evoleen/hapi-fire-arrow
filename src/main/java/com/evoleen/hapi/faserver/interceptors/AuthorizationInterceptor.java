package com.evoleen.hapi.faserver.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import com.evoleen.hapi.faserver.security.UserIdentity;
import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor.Verdict;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.PolicyEnum;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * HAPI FHIR Authorization Interceptor that implements claim-based authorization.
 * 
 * This interceptor hooks into the HAPI FHIR request processing pipeline to:
 * - Extract user identity information from authenticated requests
 * - Apply authorization rules based on JWT claims and user roles
 * - Enforce FHIR resource-level and operation-level access controls
 * - Support role-based access control (RBAC) patterns
 * 
 * Authorization is based on:
 * - User roles from JWT token claims
 * - FHIR resource types (Patient, Practitioner, Organization, etc.)
 * - FHIR operations (read, write, delete, etc.)
 * - Resource ownership and relationships
 */
@Component
@Interceptor
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class AuthorizationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
    
    private final AuthConfigurationProperties authConfig;
    
    public AuthorizationInterceptor(AuthConfigurationProperties authConfig) {
        this.authConfig = authConfig;
    }
    
    /**
     * Intercepts incoming HTTP requests to authorize FHIR operations.
     * 
     * This hook point is called after authentication but before the actual
     * FHIR operation is performed, allowing us to enforce access controls.
     */
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public boolean incomingRequestPostProcessed(
            RequestDetails theRequestDetails,
            RestOperationTypeEnum theOperation) {
        
        // Skip authorization for non-protected endpoints
        if (shouldSkipAuthorization(theRequestDetails, theOperation)) {
            logger.debug("Skipping authorization for operation: {} on path: {}", 
                    theOperation, theRequestDetails.getRequestPath());
            return true;
        }
        
        try {
            // Get authenticated user identity
            UserIdentity userIdentity = AuthenticationInterceptor.getCurrentUserIdentity(theRequestDetails);
            
            if (userIdentity == null) {
                logger.warn("No authenticated user identity found for authorization check");
                throw new ForbiddenOperationException("Access denied: User not authenticated");
            }
            
            // Apply authorization rules
            boolean isAuthorized = checkAuthorization(theRequestDetails, theOperation, userIdentity);
            
            if (!isAuthorized) {
                logger.warn("Authorization denied for user {} attempting {} on {}", 
                        userIdentity.getUserId(), theOperation, theRequestDetails.getRequestPath());
                throw new ForbiddenOperationException("Access denied: Insufficient permissions");
            }
            
            logger.debug("Authorization granted for user {} performing {} on {}", 
                    userIdentity.getUserId(), theOperation, theRequestDetails.getRequestPath());
            
            return true;
            
        } catch (ForbiddenOperationException e) {
            // Re-throw authorization exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during authorization: {}", e.getMessage(), e);
            throw new ForbiddenOperationException("Access denied: Authorization error");
        }
    }
    
    /**
     * Checks if the user is authorized to perform the requested operation
     */
    private boolean checkAuthorization(
            RequestDetails theRequestDetails, 
            RestOperationTypeEnum theOperation,
            UserIdentity userIdentity) {
        
        // Get the resource type being accessed
        String resourceType = theRequestDetails.getResourceName();
        
        // Apply role-based access control
        if (!hasRequiredRole(userIdentity, theOperation, resourceType)) {
            return false;
        }
        
        // Apply FHIR-specific authorization rules
        if (!checkFhirSpecificRules(theRequestDetails, theOperation, userIdentity)) {
            return false;
        }
        
        // Apply resource-level authorization
        if (!checkResourceLevelAuthorization(theRequestDetails, theOperation, userIdentity)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the user has the required role for the operation
     */
    private boolean hasRequiredRole(
            UserIdentity userIdentity, 
            RestOperationTypeEnum theOperation, 
            String resourceType) {
        
        Set<String> userRoles = userIdentity.getRoles();
        
        // Admin role has access to everything
        if (userRoles.contains("admin") || userRoles.contains("administrator")) {
            return true;
        }
        
        // Check operation-specific role requirements
        switch (theOperation) {
            case READ:
            case SEARCH_TYPE:
            case SEARCH_SYSTEM:
            case HISTORY_INSTANCE:
            case HISTORY_TYPE:
            case HISTORY_SYSTEM:
            case GET_PAGE:
                // Read operations - require read access
                return hasReadAccess(userRoles, resourceType, userIdentity);
                
            case CREATE:
            case UPDATE:
            case PATCH:
                // Write operations - require write access
                return hasWriteAccess(userRoles, resourceType, userIdentity);
                
            case DELETE:
                // Delete operations - require delete access
                return hasDeleteAccess(userRoles, resourceType, userIdentity);
                
            case TRANSACTION:
            case BATCH:
                // Batch operations - require special permissions
                return hasBatchAccess(userRoles, userIdentity);
                
            case VALIDATE:
            case META:
            case META_ADD:
            case META_DELETE:
                // Metadata operations - generally allowed for authenticated users
                return true;
                
            default:
                logger.warn("Unknown operation type: {}", theOperation);
                return false;
        }
    }
    
    /**
     * Checks if the user has read access to the resource type
     */
    private boolean hasReadAccess(Set<String> userRoles, String resourceType, UserIdentity userIdentity) {
        // Practitioners can read most clinical resources
        if (userIdentity.isPractitioner()) {
            return userRoles.contains("practitioner") || 
                   userRoles.contains("clinician") ||
                   userRoles.contains("nurse") ||
                   userRoles.contains("doctor");
        }
        
        // Patients can read their own resources
        if (userIdentity.isPatient()) {
            return userRoles.contains("patient");
        }
        
        // Organization users can read organization-related resources
        if (userIdentity.isOrganization()) {
            return userRoles.contains("organization") || userRoles.contains("org_admin");
        }
        
        // System users can read system resources
        return userRoles.contains("system") || userRoles.contains("service");
    }
    
    /**
     * Checks if the user has write access to the resource type
     */
    private boolean hasWriteAccess(Set<String> userRoles, String resourceType, UserIdentity userIdentity) {
        // More restrictive than read access
        
        // Practitioners can write clinical resources
        if (userIdentity.isPractitioner()) {
            return userRoles.contains("practitioner") || 
                   userRoles.contains("clinician") ||
                   userRoles.contains("doctor") ||
                   (userRoles.contains("nurse") && isNurseWritableResource(resourceType));
        }
        
        // Patients have limited write access
        if (userIdentity.isPatient()) {
            return userRoles.contains("patient") && isPatientWritableResource(resourceType);
        }
        
        // Organization administrators can manage organization resources
        if (userIdentity.isOrganization()) {
            return userRoles.contains("org_admin");
        }
        
        // System users have broad write access
        return userRoles.contains("system") && userRoles.contains("write");
    }
    
    /**
     * Checks if the user has delete access
     */
    private boolean hasDeleteAccess(Set<String> userRoles, String resourceType, UserIdentity userIdentity) {
        // Very restrictive - only admins and specific roles can delete
        
        if (userRoles.contains("admin") || userRoles.contains("administrator")) {
            return true;
        }
        
        // Practitioners can delete certain resources they created
        if (userIdentity.isPractitioner() && userRoles.contains("senior_practitioner")) {
            return isDeletableResource(resourceType);
        }
        
        // System services can delete with proper permissions
        return userRoles.contains("system") && userRoles.contains("delete");
    }
    
    /**
     * Checks if the user has batch operation access
     */
    private boolean hasBatchAccess(Set<String> userRoles, UserIdentity userIdentity) {
        // Batch operations require special permissions
        return userRoles.contains("admin") || 
               userRoles.contains("system") || 
               userRoles.contains("batch_processor");
    }
    
    /**
     * Applies FHIR-specific authorization rules
     */
    private boolean checkFhirSpecificRules(
            RequestDetails theRequestDetails, 
            RestOperationTypeEnum theOperation,
            UserIdentity userIdentity) {
        
        String resourceType = theRequestDetails.getResourceName();
        
        // Patient compartment rules
        if (userIdentity.isPatient() && StringUtils.hasText(userIdentity.getFhirId())) {
            return checkPatientCompartmentAccess(theRequestDetails, userIdentity);
        }
        
        // Practitioner rules
        if (userIdentity.isPractitioner()) {
            return checkPractitionerAccess(theRequestDetails, theOperation, userIdentity);
        }
        
        // Organization rules
        if (userIdentity.isOrganization()) {
            return checkOrganizationAccess(theRequestDetails, theOperation, userIdentity);
        }
        
        return true; // Default allow for system users
    }
    
    /**
     * Checks patient compartment access rules
     */
    private boolean checkPatientCompartmentAccess(RequestDetails theRequestDetails, UserIdentity userIdentity) {
        String resourceType = theRequestDetails.getResourceName();
        String patientId = userIdentity.getFhirId();
        
        // Patients can only access resources in their compartment
        if ("Patient".equals(resourceType)) {
            // For Patient resources, check if accessing own record
            IIdType resourceId = theRequestDetails.getId();
            return resourceId == null || patientId.equals(resourceId.getIdPart());
        }
        
        // For other resources, they must be linked to the patient
        // This would require more complex logic to check patient references
        return true; // Simplified for now - in production, implement proper compartment checking
    }
    
    /**
     * Checks practitioner-specific access rules
     */
    private boolean checkPractitionerAccess(
            RequestDetails theRequestDetails, 
            RestOperationTypeEnum theOperation,
            UserIdentity userIdentity) {
        
        // Practitioners have broad access to clinical resources
        String resourceType = theRequestDetails.getResourceName();
        
        // Restrict access to administrative resources
        if (isAdministrativeResource(resourceType) && 
            !userIdentity.hasRole("admin") && 
            !userIdentity.hasRole("org_admin")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks organization-specific access rules
     */
    private boolean checkOrganizationAccess(
            RequestDetails theRequestDetails, 
            RestOperationTypeEnum theOperation,
            UserIdentity userIdentity) {
        
        // Organization users can access organization-related resources
        String resourceType = theRequestDetails.getResourceName();
        
        if ("Organization".equals(resourceType)) {
            // Can access own organization record
            IIdType resourceId = theRequestDetails.getId();
            String orgId = userIdentity.getFhirId();
            return resourceId == null || orgId == null || orgId.equals(resourceId.getIdPart());
        }
        
        return true;
    }
    
    /**
     * Checks resource-level authorization (e.g., ownership, references)
     */
    private boolean checkResourceLevelAuthorization(
            RequestDetails theRequestDetails, 
            RestOperationTypeEnum theOperation,
            UserIdentity userIdentity) {
        
        // This would implement fine-grained resource-level checks
        // For example:
        // - Check if user owns/created the resource
        // - Check if resource is linked to user's organization
        // - Check if user has specific permissions on the resource
        
        // For now, return true - implement specific logic as needed
        return true;
    }
    
    /**
     * Determines if authorization should be skipped for this request
     */
    private boolean shouldSkipAuthorization(RequestDetails theRequestDetails, RestOperationTypeEnum theOperation) {
        String requestPath = theRequestDetails.getRequestPath();
        
        if (!StringUtils.hasText(requestPath)) {
            return false;
        }
        
        // Skip authorization for metadata and system endpoints
        if (requestPath.equals("/metadata") || 
            requestPath.equals("/fhir/metadata") ||
            requestPath.startsWith("/actuator/") ||
            requestPath.startsWith("/swagger") ||
            requestPath.startsWith("/api-docs")) {
            return true;
        }
        
        // Skip for METADATA operations
        if (theOperation == RestOperationTypeEnum.METADATA) {
            return true;
        }
        
        // Check if authorization is globally disabled
        return !authConfig.isEnabled();
    }
    
    // Helper methods for resource type checking
    
    private boolean isNurseWritableResource(String resourceType) {
        // Nurses can write certain clinical resources
        return "Observation".equals(resourceType) ||
               "CarePlan".equals(resourceType) ||
               "MedicationAdministration".equals(resourceType) ||
               "Procedure".equals(resourceType);
    }
    
    private boolean isPatientWritableResource(String resourceType) {
        // Patients can write limited resources
        return "Observation".equals(resourceType) || // e.g., patient-reported outcomes
               "QuestionnaireResponse".equals(resourceType) ||
               "Communication".equals(resourceType);
    }
    
    private boolean isDeletableResource(String resourceType) {
        // Only certain resources can be deleted
        return !"Patient".equals(resourceType) &&
               !"Practitioner".equals(resourceType) &&
               !"Organization".equals(resourceType);
    }
    
    private boolean isAdministrativeResource(String resourceType) {
        // Administrative resources require special permissions
        return "Organization".equals(resourceType) ||
               "Location".equals(resourceType) ||
               "HealthcareService".equals(resourceType) ||
               "Endpoint".equals(resourceType);
    }
}
