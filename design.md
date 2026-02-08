# Database Design Document

## User Service (MySQL)

### Base Entity (Extended by all entities)
```
Superclass: BaseEntity
Fields:
  - id: Long (Primary Key, Auto-increment)
  - createdAt: LocalDateTime (Timestamp when created)
  - updatedAt: LocalDateTime (Timestamp when last updated)
  - version: Integer (For optimistic locking)
```

### User Entity
```
Table: users
Extends: BaseEntity
Fields:
  - email: String (Unique, Not Null)
  - password: String (Encoded, Not Null)
  - firstName: String (Not Null)
  - lastName: String (Not Null)
  - role: Enum (GUEST, HOST, ADMIN) (Not Null)
  - phoneNumber: String (Nullable)
  - profileImageUrl: String (Nullable)
  - isVerified: Boolean (Default: false)
  - isActive: Boolean (Default: true)
  
Relationships:
  - One-to-Many: User → Bookings (user makes bookings)
  - One-to-Many: User → Listings (host creates listings)
  
Indexes:
  - email (Unique)
  - isActive (for filtering active users)

Constraints:
  - email must be unique
  - email format validation
  - password minimum length (8 chars)
  - role cannot be null
```

### UserAuditLog Entity
```
Table: user_audit_logs
Extends: BaseEntity
Purpose: Track user login attempts and sensitive actions

Fields:
  - userId: Long (Foreign Key to User)
  - action: String (LOGIN, LOGOUT, PASSWORD_CHANGE, EMAIL_CHANGE)
  - ipAddress: String
  - userAgent: String
  - isSuccessful: Boolean
  - failureReason: String (Nullable, if login failed)

Relationships:
  - Many-to-One: UserAuditLog → User

Indexes:
  - userId
  - createdAt (for time-range queries)
```

### JWT Token Blacklist Entity
```
Table: jwt_blacklist
Purpose: Store revoked/blacklisted tokens (for logout functionality)

Fields:
  - id: Long (Primary Key)
  - token: String (The JWT token)
  - userId: Long (Foreign Key to User)
  - expiryTime: LocalDateTime (When token expires)
  - createdAt: LocalDateTime (When blacklisted)

Relationships:
  - Many-to-One: JwtBlacklist → User

Indexes:
  - token (Unique)
  - userId
  - expiryTime (for cleanup queries)
```

---

## Entity Class Structure

### BaseEntity (Abstract)
```java
Location: common-lib/src/main/java/com/airbnb/common/entity/

Fields:
  - id: Long
  - createdAt: LocalDateTime
  - updatedAt: LocalDateTime
  - version: Integer

Methods:
  - getId()
  - getCreatedAt()
  - getUpdatedAt()
  - getVersion()
  - setUpdatedAt(LocalDateTime)
```

### User Entity
```java
Location: user-service/src/main/java/com/airbnb/user/entity/

Extends: BaseEntity

Fields:
  - email: String
  - password: String
  - firstName: String
  - lastName: String
  - role: UserRole (Enum)
  - phoneNumber: String
  - profileImageUrl: String
  - isVerified: Boolean
  - isActive: Boolean

Methods:
  - getters/setters (use Lombok @Getter, @Setter)
  - equals() and hashCode() (use Lombok @EqualsAndHashCode)
  - toString() (use Lombok @ToString)
```

### UserAuditLog Entity
```java
Location: user-service/src/main/java/com/airbnb/user/entity/

Extends: BaseEntity

Fields:
  - userId: Long
  - action: String
  - ipAddress: String
  - userAgent: String
  - isSuccessful: Boolean
  - failureReason: String

Methods:
  - getters/setters
```

### JwtBlacklist Entity
```java
Location: user-service/src/main/java/com/airbnb/user/entity/

Fields:
  - token: String
  - userId: Long
  - expiryTime: LocalDateTime

Methods:
  - getters/setters
```

---

## Enums

### UserRole Enum
```java
Location: user-service/src/main/java/com/airbnb/user/enums/

Values:
  - GUEST (Can book listings)
  - HOST (Can create listings)
  - ADMIN (System administration)
```

---

## Database Initialization

### MySQL User Service Database
```sql
CREATE DATABASE airbnb_user_service;
USE airbnb_user_service;

-- Base tables will be auto-created by Hibernate/JPA
-- Tables created: users, user_audit_logs, jwt_blacklist
```

### Connection Config
```
URL: jdbc:mysql://localhost:3306/airbnb_user_service
Username: root
Password: password
Driver: MySQL 8.0 (com.mysql.cj.jdbc.Driver)
Dialect: org.hibernate.dialect.MySQL8Dialect
```

---

## Entity Relationships Diagram (Text)

```
User (1) ──── (Many) UserAuditLog
  ↓
  └──── (Many) JwtBlacklist
  
User (1) ──── (Many) Bookings (in Booking Service - FK reference)
User (1) ──── (Many) Listings (in Listing Service - FK reference)
```

---

## Notes for Implementation

- All entities use Lombok annotations to reduce boilerplate
- BaseEntity will be in common-lib, imported by user-service
- use `@Version` for optimistic locking on User entity
- use `@CreationTimestamp` and `@UpdateTimestamp` for audit fields
- All date fields use `LocalDateTime` (not `java.util.Date`)
- Passwords stored as bcrypt hashes (encoding done in service layer)
- JWT tokens stored in blacklist table for revocation/logout
