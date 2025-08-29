package com.evoleen.hapi.faserver.security;

import java.util.*;

/**
 * Represents user identity information extracted from JWT token claims.
 * Contains both standard OAuth claims and FHIR-specific claims.
 */
public class UserIdentity {
    
    private final String userId;
    private final String fhirUserRoleResourceType;
    private final String fhirId;
    private final Set<String> roles;
    private final String email;
    private final String name;
    private final String issuer;
    private final String subject;
    private final List<String> audience;
    private final Date expirationTime;
    private final Date issuedAt;
    
    private UserIdentity(Builder builder) {
        this.userId = builder.userId;
        this.fhirUserRoleResourceType = builder.fhirUserRoleResourceType;
        this.fhirId = builder.fhirId;
        this.roles = Collections.unmodifiableSet(new HashSet<>(builder.roles));
        this.email = builder.email;
        this.name = builder.name;
        this.issuer = builder.issuer;
        this.subject = builder.subject;
        this.audience = builder.audience != null ? 
                Collections.unmodifiableList(new ArrayList<>(builder.audience)) : null;
        this.expirationTime = builder.expirationTime;
        this.issuedAt = builder.issuedAt;
    }
    
    // Getters
    
    /**
     * Returns the user ID (typically from 'sub' claim)
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Returns the FHIR user role resource type (e.g., "Practitioner", "Patient")
     */
    public String getFhirUserRoleResourceType() {
        return fhirUserRoleResourceType;
    }
    
    /**
     * Returns the FHIR ID (optional)
     */
    public String getFhirId() {
        return fhirId;
    }
    
    /**
     * Returns the user roles/permissions
     */
    public Set<String> getRoles() {
        return roles;
    }
    
    /**
     * Returns the user email
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Returns the user name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the token issuer
     */
    public String getIssuer() {
        return issuer;
    }
    
    /**
     * Returns the token subject
     */
    public String getSubject() {
        return subject;
    }
    
    /**
     * Returns the token audience
     */
    public List<String> getAudience() {
        return audience;
    }
    
    /**
     * Returns the token expiration time
     */
    public Date getExpirationTime() {
        return expirationTime;
    }
    
    /**
     * Returns the token issued at time
     */
    public Date getIssuedAt() {
        return issuedAt;
    }
    
    /**
     * Checks if the user has a specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Checks if the user has any of the specified roles
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }
    
    /**
     * Checks if the user has all of the specified roles
     */
    public boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        return Arrays.stream(roles).allMatch(this::hasRole);
    }
    
    /**
     * Checks if this represents a FHIR Practitioner
     */
    public boolean isPractitioner() {
        return "Practitioner".equalsIgnoreCase(fhirUserRoleResourceType);
    }
    
    /**
     * Checks if this represents a FHIR Patient
     */
    public boolean isPatient() {
        return "Patient".equalsIgnoreCase(fhirUserRoleResourceType);
    }
    
    /**
     * Checks if this represents a FHIR Organization
     */
    public boolean isOrganization() {
        return "Organization".equalsIgnoreCase(fhirUserRoleResourceType);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserIdentity that = (UserIdentity) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(fhirUserRoleResourceType, that.fhirUserRoleResourceType) &&
                Objects.equals(fhirId, that.fhirId) &&
                Objects.equals(roles, that.roles) &&
                Objects.equals(email, that.email) &&
                Objects.equals(name, that.name) &&
                Objects.equals(issuer, that.issuer) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(audience, that.audience) &&
                Objects.equals(expirationTime, that.expirationTime) &&
                Objects.equals(issuedAt, that.issuedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, fhirUserRoleResourceType, fhirId, roles, email, name, 
                issuer, subject, audience, expirationTime, issuedAt);
    }
    
    @Override
    public String toString() {
        return "UserIdentity{" +
                "userId='" + userId + '\'' +
                ", fhirUserRoleResourceType='" + fhirUserRoleResourceType + '\'' +
                ", fhirId='" + fhirId + '\'' +
                ", roles=" + roles +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", issuer='" + issuer + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
    
    /**
     * Builder for UserIdentity
     */
    public static class Builder {
        private String userId;
        private String fhirUserRoleResourceType;
        private String fhirId;
        private Set<String> roles = new HashSet<>();
        private String email;
        private String name;
        private String issuer;
        private String subject;
        private List<String> audience;
        private Date expirationTime;
        private Date issuedAt;
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder fhirUserRoleResourceType(String fhirUserRoleResourceType) {
            this.fhirUserRoleResourceType = fhirUserRoleResourceType;
            return this;
        }
        
        public Builder fhirId(String fhirId) {
            this.fhirId = fhirId;
            return this;
        }
        
        public Builder roles(Set<String> roles) {
            this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
            return this;
        }
        
        public Builder role(String role) {
            if (role != null) {
                this.roles.add(role);
            }
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }
        
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }
        
        public Builder audience(List<String> audience) {
            this.audience = audience;
            return this;
        }
        
        public Builder expirationTime(Date expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }
        
        public Builder issuedAt(Date issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        public UserIdentity build() {
            return new UserIdentity(this);
        }
    }
}
