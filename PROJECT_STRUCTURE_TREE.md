# Complete Project Structure Tree

```
airbnb-booking-platform/
│
├── pom.xml (parent)
├── docker-compose.yml
│
├── common-lib/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/airbnb/common/
│       │   ├── dto/ (5 DTOs)
│       │   ├── events/ (5 Kafka Events)
│       │   ├── constants/ (3 Classes)
│       │   ├── exception/ (4 Exception Classes)
│       │   ├── util/ (Utilities)
│       │   └── config/ (ObservabilityConfiguration)
│       └── test/java/
│
├── user-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/user/
│       │   │   ├── controller/        → REST endpoints (@RestController)
│       │   │   ├── service/           → Business logic (IUserService interface + impl)
│       │   │   ├── repository/        → Data access (UserRepository extends JpaRepository)
│       │   │   ├── entity/            → JPA entities (User extends BaseEntity)
│       │   │   ├── security/          → JWT, Spring Security config
│       │   │   ├── config/            → Spring configurations
│       │   │   └── UserServiceApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8001, MySQL: localhost:3306/airbnb_user_service)
│       └── test/java/com/airbnb/user/
│
├── listing-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/listing/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── entity/
│       │   │   ├── config/
│       │   │   └── ListingServiceApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8002, PostgreSQL: localhost:5432/airbnb_listing_service)
│       └── test/java/com/airbnb/listing/
│
├── availability-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/availability/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── entity/
│       │   │   ├── config/
│       │   │   └── AvailabilityServiceApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8003, PostgreSQL + Redis: localhost:6379)
│       └── test/java/com/airbnb/availability/
│
├── booking-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/booking/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── entity/
│       │   │   ├── config/
│       │   │   └── BookingServiceApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8004, MySQL + Kafka)
│       └── test/java/com/airbnb/booking/
│
├── payment-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/payment/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── entity/
│       │   │   ├── config/
│       │   │   └── PaymentServiceApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8005, MySQL + Kafka + Resilience4j)
│       └── test/java/com/airbnb/payment/
│
├── notification-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/notification/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── config/
│       │   │   └── NotificationServiceApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8006, Kafka)
│       └── test/java/com/airbnb/notification/
│
├── api-gateway/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/airbnb/gateway/
│       │   │   ├── config/        → Routes & Spring AI config
│       │   │   ├── filter/        → Custom filters & interceptors
│       │   │   ├── controller/    → ChatBot endpoint
│       │   │   ├── service/       → Spring AI service
│       │   │   └── ApiGatewayApplication.java
│       │   └── resources/
│       │       └── application.yml (Port 8000, Spring Cloud Gateway routing)
│       └── test/java/com/airbnb/gateway/
│
├── .github/
│   └── workflows/
│       └── ci.yml (GitHub Actions: Unit tests on PR, Docker builds on main)
│
└── Documentation Files/
    ├── README.md
    ├── planning.md
    ├── QUICKSTART.md
    ├── GIT_SETUP.md
    ├── DOCUMENTATION_INDEX.md
    ├── FILE_INVENTORY.md
    ├── PHASE0_COMPLETE.md
    ├── PHASE0_EXECUTION_SUMMARY.md
    ├── PHASE0_FINAL_SUMMARY.md
    ├── PHASE0_MANIFEST.md
    └── PROJECT_STRUCTURE_SUMMARY.md
```

## Service Port Mapping

| Service | Port | Database | Features |
|---------|------|----------|----------|
| API Gateway | 8000 | None | Spring Cloud Gateway, routing, Spring AI |
| User Service | 8001 | MySQL | Auth, JWT, Spring Security |
| Listing Service | 8002 | PostgreSQL | Search, pagination, full-text |
| Availability Service | 8003 | PostgreSQL | Calendar, Redis locks, concurrency |
| Booking Service | 8004 | MySQL | Saga pattern, event orchestration |
| Payment Service | 8005 | MySQL | Stripe, Resilience4j, idempotency |
| Notification Service | 8006 | None | Kafka consumer, async notifications |

## Database Connections

**MySQL Services:**
- Host: localhost, Port: 3306
- Username: root, Password: password
- Databases:
  - `airbnb_user_service` (User Service)
  - `airbnb_booking_service` (Booking Service)
  - `airbnb_payment_service` (Payment Service)

**PostgreSQL Services:**
- Host: localhost, Port: 5432
- Username: postgres, Password: password
- Databases:
  - `airbnb_listing_service` (Listing Service)
  - `airbnb_availability_service` (Availability Service)

**Redis:**
- Host: localhost, Port: 6379
- Used by: Availability Service (distributed locking)

**Kafka:**
- Bootstrap Servers: localhost:9092
- Zookeeper: localhost:2181
- Used by: Booking, Payment, Notification services

## Key Files Ready for Phase 1

✅ **Application Classes**
- 7 Spring Boot main classes with proper annotations
- Located in each service's `src/main/java/com/airbnb/{service}/` directory

✅ **Configuration Files**
- 7 application.yml files with service-specific settings
- Located in each service's `src/main/resources/` directory

✅ **Package Structures**
- controller/ - For REST endpoints
- service/ - For business logic interfaces and implementations
- repository/ - For Spring Data JPA repositories
- entity/ - For JPA entity classes
- config/ - For Spring configurations
- security/ (User & API Gateway) - For security configurations
- filter/ (API Gateway) - For custom filters
- test/ - For unit and integration tests

## Ready for Implementation

This structure is **100% ready** for Phase 1 development:

```bash
# Each service can be independently built
mvn clean package -pl user-service

# Run individual service
mvn spring-boot:run -pl user-service

# Run all tests
mvn test

# Build Docker image (after Dockerfile creation in Phase 1)
docker build -t airbnb/user-service:latest user-service/
```

---

**Total Directories Created:** 40+ packages
**Total Configuration Files:** 7 application.yml
**Total Application Classes:** 7 main classes
**Status:** Ready for Phase 1 implementation
