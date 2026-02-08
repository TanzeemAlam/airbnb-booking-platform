# Airbnb-Like Booking Platform - Execution Plan

## Project Overview
A distributed microservices-based booking platform built with Java 17, Spring Boot, and enterprise patterns. This document outlines the iterative development approach with emphasis on architectural patterns, testing strategies, and interview-ready implementation.

---

## Tech Stack (Locked)
- **Language:** Java 17 (core stability) + Java 21 features (selective adoption)
  - Virtual Threads for async consumers (Booking, Payment, Notification services)
  - Record Patterns for event/DTO processing
  - Pattern Matching (switch expressions) for saga state handling
- **Framework:** Spring Boot 3.x
- **Security:** Spring Security + JWT
- **Database:** MySQL (User, Booking, Notification) | PostgreSQL (Listing, Availability)
- **Messaging:** Kafka with Zookeeper
- **Caching:** Redis
- **Resilience:** Resilience4j (Circuit Breaker, Retry)
- **Observability:** Spring Boot Actuator + Micrometer (auto-config tracing), Zipkin UI (minimal, read-only), Structured Logging
- **Payment:** Stripe Sandbox (test mode, free) with Mock fallback
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Containerization:** Docker + Docker Compose
- **CI/CD:** GitHub Actions
- **API Documentation:** Swagger/OpenAPI
- **AI Feature:** Spring AI (embedded in API Gateway for chatbot)

---

## Java Design Patterns & OOP Principles (Throughout Project)

### Design Patterns (GoF & Microservices)
______________________________________________________________________________________________________________________________________________
| Pattern                 | Category      | Usage                                    | Example                                               |
|-------------------------|---------------|------------------------------------------|-------------------------------------------------------|
| **Microservices**       | Architectural | Service per domain (User, Listing, etc.) | Independent services with separate databases          |
| **Saga**                | Behavioral    | Multi-step distributed transactions      | Booking → Payment → Confirmation with compensation    |
| **CQRS**                | Architectural | Command Query Responsibility Separation  | BookingService writes events, separate read models    |
| **Event Sourcing**      | Behavioral    | Immutable event log for audit trail      | Kafka topics as event store, replay for recovery      |
| **Repository**          | Structural    | Abstract data access layer               | Spring Data JPA repositories                          |
| **DTO**                 | Structural    | Data transfer objects using Records      | `record BookingDTO(Long id, LocalDate checkIn) {}`    |
| **Circuit Breaker**     | Behavioral    | Fault tolerance for external calls       | Resilience4j for Stripe payment integration           |
| **Retry**               | Behavioral    | Transient failure handling               | 3 attempts with exponential backoff                   |
| **Timeout**             | Behavioral    | Prevent hanging requests                 | 5-second timeout for API calls                        |
| **Bulkhead**            | Behavioral    | Thread pool isolation                    | Separate pools for payment vs notification            |
| **Observer**            | Behavioral    | Event listener pattern                   | Kafka consumers for async events                      |
| **Strategy**            | Behavioral    | Switchable algorithms                    | PaymentGateway (Stripe, Mock implementations)         |
| **Adapter**             | Structural    | Interface translation                    | MockPaymentGateway adapts to PaymentGateway interface |
| **Decorator**           | Structural    | Add behavior dynamically                 | Resilience4j decorators wrapping service calls        |
| **Factory**             | Creational    | Object creation abstraction              | BookingDTOFactory for complex DTO creation            |
| **Builder**             | Creational    | Complex object construction              | Fluent API for building queries or requests           |
| **Template Method**     | Behavioral    | Skeleton algorithm definition            | BaseService with common transaction logic             |
| **State**               | Behavioral    | State-dependent behavior                 | Booking (PENDING, CONFIRMED, CANCELLED)               |
| **Distributed Locking** | Concurrency   | Atomicity across instances               | Redis NX/EX for availability reservation              |
| **Optimistic Locking**  | Concurrency   | Version-based conflict detection         | DB version field in Availability table                |

### OOP Principles Applied

**1. Encapsulation**
```java
// Service hides implementation details
public class BookingService {
    private final AvailabilityService availabilityService;  // private dependency
    private final PaymentService paymentService;             // injected, not accessible
    
    public BookingDTO createBooking(BookingRequest request) {  // public contract
        // Complex internal logic hidden
        return new BookingDTO(...);
    }
}
```

**2. Abstraction**
```java
// Define contract, hide implementation
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
}

public class StripePaymentGateway implements PaymentGateway { }
public class MockPaymentGateway implements PaymentGateway { }

// Client depends on abstraction, not implementation
@Autowired private PaymentGateway paymentGateway;  // can inject either
```

**3. Inheritance**
```java
// Share common behavior through base class
public abstract class BaseEntity {
    @Id private Long id;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Version private Integer version;  // for optimistic locking
}

public class Booking extends BaseEntity {
    private Long userId;
    private Long listingId;
    // inherits id, createdAt, updatedAt, version
}
```

**4. Polymorphism**
```java
// Multiple implementations, single interface
List<NotificationHandler> handlers = List.of(
    new EmailNotificationHandler(),
    new SMSNotificationHandler(),
    new LogNotificationHandler()
);

for (NotificationHandler handler : handlers) {
    handler.send(notification);  // polymorphic call
}

// Java 21 Pattern Matching
switch (notification) {
    case EmailNotification e -> sendEmail(e.getAddress(), e.getBody());
    case SMSNotification s -> sendSMS(s.getPhoneNumber(), s.getMessage());
    default -> log.warn("Unknown notification type");
}
```

### Exception Handling Strategy

```java
// 1. Custom Exception Hierarchy
public class AirbnbException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> context;
    
    public AirbnbException(ErrorCode code, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = code;
        this.context = context;
    }
}

public class BookingException extends AirbnbException { }
public class PaymentException extends AirbnbException { }
public class AvailabilityException extends AirbnbException { }

// 2. Usage in Service
public void createBooking(BookingRequest request) {
    return availabilityService.reserve(request.getListingId(), request.getCheckInDate())
        .orElseThrow(() -> new AvailabilityException(
            ErrorCode.UNAVAILABLE,
            "Listing not available for selected dates",
            Map.of("listingId", request.getListingId(), "checkIn", request.getCheckInDate())
        ));
}

// 3. Global Error Handler
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ErrorResponse> handleBookingException(BookingException ex) {
        return ResponseEntity
            .status(ex.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex.getContext()
            ));
    }
}
```

### Optional & Null Safety

```java
// 1. Use Optional instead of null checks
public Optional<Booking> findBooking(Long id) {
    return bookingRepository.findById(id)
        .filter(b -> !b.isCancelled());  // fluent chaining
}

// 2. Consuming Optional
bookingRepository.findById(id)
    .ifPresent(booking -> sendConfirmationEmail(booking.getUserId()))
    .orElse(() -> log.warn("Booking not found"));

// 3. Chaining operations
var userEmail = bookingRepository.findById(id)
    .map(Booking::getUserId)
    .flatMap(userId -> userRepository.findById(userId))
    .map(User::getEmail)
    .orElseThrow(() -> new UserNotFoundException("User not found"));

// 4. Stream with Optional
bookings.stream()
    .filter(b -> !b.isCancelled())
    .map(b -> userRepository.findById(b.getUserId()))
    .flatMap(Optional::stream)  // Java 9+
    .forEach(user -> sendNotification(user));
```

### Stream API & Functional Programming

```java
// 1. Filtering and mapping
List<ListingDTO> availableListings = listings.stream()
    .filter(l -> l.getPrice() >= minPrice && l.getPrice() <= maxPrice)
    .filter(l -> !l.isCancelled())
    .map(l -> new ListingDTO(l.getId(), l.getTitle(), l.getPrice()))
    .collect(Collectors.toList());

// 2. Grouping
Map<String, List<Booking>> bookingsByStatus = bookings.stream()
    .collect(Collectors.groupingBy(
        booking -> booking.getStatus().name(),
        Collectors.toList()
    ));

// 3. Reducing for aggregation
BigDecimal totalRevenue = bookings.stream()
    .map(Booking::getPrice)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// 4. Lazy evaluation (no intermediate collection)
boolean hasFailedPayments = payments.stream()
    .filter(p -> p.getStatus() == PaymentStatus.FAILED)
    .anyMatch(p -> p.getRetries() >= MAX_RETRIES);

// 5. Parallel streams for performance
List<Listing> results = listings.parallelStream()
    .filter(l -> expensiveCalculation(l))  // distribute across cores
    .collect(Collectors.toList());
```

### Data Structure Selection
____________________________________________________________________________________________________________________________
| Use Case                         | Data Structure                             | Why                                      |
|----------------------------------|--------------------------------------------|------------------------------------------|
| **Cache with concurrent access** | `ConcurrentHashMap`                        | Thread-safe, no locking overhead         |
| **Session tokens**               | `ConcurrentHashMap<String, TokenMetadata>` | Fast lookup, concurrent put/get          |
| **Lock tracking**                | `ConcurrentHashMap<String, ReservationId>` | Atomic operations (putIfAbsent)          |
| **Ordered reservations**         | `LinkedHashMap` or `LinkedList`            | FIFO order preservation                  |
| **Unique values**                | `HashSet` or `TreeSet`                     | No duplicates, O(1) contains check       |
| **Sorted operations**            | `TreeMap` or `TreeSet`                     | Ordered access, range queries            |
| **Large result sets**            | `Stream<T>`                                | Lazy evaluation, memory efficient        |
| **Pending notifications**        | `Queue<Notification>`                      | FIFO processing guarantee                |
| **Multi-key lookups**            | `Map<String, Map<String, Value>>`          | Or custom key class with equals/hashCode |

### Java Memory Model & GC Optimization

**1. Immutable Objects (GC-friendly)**
```java
// Java 21 Records - automatically final and immutable
public record BookingCreatedEvent(
    Long bookingId,
    Long listingId,
    LocalDate checkIn,
    BigDecimal totalPrice
) {}

// No need for setters, no accidental mutations
// Reduces GC pressure: objects move to old generation, not constantly modified
```

**2. Volatile & Happens-Before**
```java
// Shared state in concurrent context
public class PaymentState {
    private volatile PaymentStatus status = PaymentStatus.PENDING;  // visibility
    
    public synchronized void updateStatus(PaymentStatus newStatus) {
        // happens-before: all writes before lock release visible after lock acquire
        this.status = newStatus;
    }
    
    public PaymentStatus getStatus() {
        return status;  // volatile read returns latest value
    }
}
```

**3. Weak References for Caches**
```java
// Temp cache without GC interference
Map<String, WeakReference<CachedData>> cache = new WeakHashMap<>();

// If memory is needed, WeakReference is cleared by GC
cache.put("key", new WeakReference<>(expensiveComputation()));
```

**4. Stream API - Lazy Evaluation**
```java
// NOT executed until terminal operation
Stream<Booking> bookings = allBookings.stream()
    .filter(b -> b.getPrice() > 100)  // lazy
    .sorted(Comparator.comparing(Booking::getCreatedAt))  // lazy
    .limit(10);  // lazy

// Executed here (terminal operation)
List<Booking> result = bookings.collect(Collectors.toList());
// Only 10 items in memory, not entire collection
```

**5. Cleanup Strategies**
```java
// Background job: cleanup expired locks/reservations
@Scheduled(fixedDelay = 60000)  // every 60 seconds
public void cleanupExpiredLocks() {
    locks.entrySet().stream()
        .filter(e -> e.getValue().isExpired())
        .map(Map.Entry::getKey)
        .forEach(locks::remove);  // prevent memory leak
}

// Database: cleanup old events/logs
@Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
public void archiveOldEvents() {
    eventRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusMonths(6));
}
```

### Java 21 Modern Language Constructs

**1. Records (Immutable Data Carriers)**
```java
// Before (11 lines of boilerplate)
public class BookingDTO {
    private final Long id;
    private final Long userId;
    public BookingDTO(Long id, Long userId) { ... }
    @Override public boolean equals(Object o) { ... }
    @Override public int hashCode() { ... }
    @Override public String toString() { ... }
}

// After (1 line)
public record BookingDTO(Long id, Long userId) {}
// Automatic equals, hashCode, toString, and all final fields
```

**2. Pattern Matching (Switch Expressions)**
```java
// Before (many branches)
String message;
if (event instanceof BookingCreatedEvent) {
    BookingCreatedEvent bce = (BookingCreatedEvent) event;
    message = "Booking " + bce.bookingId() + " created";
} else if (event instanceof PaymentSuccessEvent) { ... }

// After (exhaustiveness checking)
String message = switch (event) {
    case BookingCreatedEvent(var bookingId, var userId, var amount) -> 
        "Booking %d created for user %d".formatted(bookingId, userId);
    case PaymentSuccessEvent(var paymentId, var txId) -> 
        "Payment %d processed (txId: %s)".formatted(paymentId, txId);
    case BookingCancelledEvent bce -> 
        "Booking cancelled with refund: %s".formatted(bce.refundAmount());
    default -> "Unknown event";
};
```

**3. Virtual Threads (Lightweight Threads)**
```java
// Before: limited by OS thread count (~1000)
@Scheduled(fixedDelay = 1000)
public void consumeNotifications() {
    // Only 10 threads can process notifications concurrently
    ExecutorService executor = Executors.newFixedThreadPool(10);
}

// After: can create 100K+ virtual threads
@Bean
public Executor virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

@KafkaListener(topics = "booking.confirmed")
@Async("virtualThreadExecutor")
public void handleBookingConfirmed(BookingConfirmedEvent event) {
    // Each listener runs on virtual thread
    // Can handle 100K concurrent events without blocking
    sendNotification(event.userId(), "Your booking is confirmed");
}
```

---

### Microservices
1. **User Service** (Port 8001) - MySQL
   - Registration, Login, JWT issuance, Role management (HOST/GUEST)
   
2. **Listing Service** (Port 8002) - PostgreSQL
   - Create/View listings, Search with pagination, Filtering
   
3. **Availability Service** (Port 8003) - PostgreSQL
   - Calendar management, Concurrency handling, Redis locks, Prevent double booking
   
4. **Booking Service** (Port 8004) - MySQL
   - Saga orchestration, Booking lifecycle, Event publishing
   
5. **Payment Service** (Port 8005) - MySQL
   - Stripe integration, Idempotency keys, Circuit breaker, Retry logic
   
6. **Notification Service** (Port 8006) - No DB
   - Kafka event consumer, Email/SMS simulation, Logging

### Infrastructure Services
7. **API Gateway** (Port 8000) - Spring Cloud Gateway
   - Routing, Load balancing, Spring AI chatbot embedded
   - Automatic distributed tracing (Micrometer)

### Supporting Infrastructure
- **MySQL** - Port 3306 (User, Booking, Notification services)
- **PostgreSQL** - Port 5432 (Listing, Availability services)
- **Kafka** - Port 9092 (Event streaming)
- **Zookeeper** - Port 2181 (Kafka coordination)
- **Redis** - Port 6379 (Caching, distributed locks)
- **Zipkin** - Port 9411 (Trace UI - read-only visualization of Micrometer traces)

---

## Project Structure (Maven Multi-Module)

```
airbnb-booking-platform/
├── pom.xml (parent)
├── common-lib/
│   ├── src/main/java/com/airbnb/common/
│   │   ├── dto/           (Shared DTOs)
│   │   ├── events/        (Kafka event schemas)
│   │   ├── constants/     (Shared constants)
│   │   └── util/          (Common utilities, Zipkin setup)
│   └── pom.xml
├── user-service/
│   ├── src/main/java/com/airbnb/user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── security/
│   │   └── config/
│   ├── src/test/
│   ├── Dockerfile
│   └── pom.xml
├── listing-service/
│   ├── src/main/java/com/airbnb/listing/
│   └── ... (similar structure)
├── availability-service/
│   ├── src/main/java/com/airbnb/availability/
│   └── ... (similar structure)
├── booking-service/
│   ├── src/main/java/com/airbnb/booking/
│   └── ... (similar structure)
├── payment-service/
│   ├── src/main/java/com/airbnb/payment/
│   └── ... (similar structure)
├── notification-service/
│   ├── src/main/java/com/airbnb/notification/
│   └── ... (similar structure)
├── api-gateway/
│   ├── src/main/java/com/airbnb/gateway/
│   │   ├── config/        (Routes, AI config)
│   │   ├── filter/        (Zipkin propagation)
│   │   ├── controller/    (AI chatbot endpoint)
│   │   └── service/       (Spring AI service)
│   ├── Dockerfile
│   └── pom.xml
├── service-discovery/
│   ├── pom.xml
│   └── Dockerfile
├── config-server/
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml     (All infrastructure)
└── README.md

```

---

## Development Phases (Iterative - ~10 hrs/day effort)

### Phase 0: Foundation & Infrastructure Setup
**Objective:** Establish project skeleton, shared libraries, and local development environment with Java 17 as foundation.

**Deliverables:**
- ✅ Maven multi-module parent pom with version management
- ✅ `common-lib` module with:
  - **Shared DTOs** (Java Records for immutability and memory efficiency)
    - `record UserDTO(Long id, String email, String role) {}`
    - `record ListingDTO(Long id, String title, BigDecimal price) {}`
    - `record BookingDTO(Long id, Long listingId, LocalDate checkIn, LocalDate checkOut) {}`
    - `record PaymentDTO(Long id, BigDecimal amount, String status) {}`
    - `record NotificationDTO(Long id, String message, String type) {}`
  - **Kafka Event Schemas** (Records with pattern matching support)
    - `record BookingCreatedEvent(Long bookingId, Long listingId, Long userId, BigDecimal totalPrice) {}`
    - `record PaymentSuccessEvent(Long paymentId, Long bookingId, String stripeTransactionId) {}`
    - `record PaymentFailedEvent(Long paymentId, Long bookingId, String reason) {}`
    - `record BookingConfirmedEvent(Long bookingId, Long userId, LocalDate checkIn) {}`
    - `record BookingCancelledEvent(Long bookingId, String reason, BigDecimal refundAmount) {}`
  - Constants (Role, BookingStatus, PaymentStatus)
  - Utility classes for exception handling, Optional usage
  - Spring Boot auto-config for distributed tracing (Micrometer)
- ✅ `docker-compose.yml` with MySQL, PostgreSQL, Kafka, Zookeeper, Redis (no separate Zipkin container)
- ✅ GitHub Actions CI/CD skeleton (.github/workflows/ci.yml)
- ✅ README skeleton with architecture diagram and quick start

**Key Activities:**
1. Create parent pom.xml with Spring Boot 3.x BOM, dependency management
2. Initialize all service modules (User, Listing, Availability, Booking, Payment, Notification, API Gateway)
3. Set up common-lib with:
   - **Design Patterns:** Factory pattern for DTO creation, Builder pattern for complex objects
   - **OOP Principles:** Encapsulation (Records), Abstraction (interfaces for services), Inheritance (base entity classes), Polymorphism (event processing)
   - **Exception Handling:** Custom exceptions (BookingException, PaymentException), try-catch with proper logging, Optional for null safety
   - **Data Structures:** Choose ConcurrentHashMap for caches, ArrayList for ordered collections, Set for unique operations
   - **Java Memory Model:** Immutable Records to reduce GC pressure, proper object lifecycle management
4. Configure docker-compose with health checks and non-root user constraints
5. Document service communication contract with API design patterns (REST: GET/POST/PUT/DELETE)

**Non-Root Docker User Setup:**
```dockerfile
# All Dockerfiles to include:
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
```

**Java 17 & 21 Feature Notes:**
- Java 17 for core services: stability, long-term support
- Java 21 Records for DTOs: automatic equals(), hashCode(), toString() → memory efficient
- Java 21 Pattern Matching (preview) for event processing: cleaner switch expressions
- Virtual Threads: for async Kafka consumers (non-blocking I/O)

---

### Phase 1: User Service + Security + API Gateway Foundation
**Objective:** Implement authentication, authorization, and API routing backbone with security best practices.

**Deliverables:**
- ✅ User Service:
  - User registration endpoint (validation, password encoding with BCrypt)
  - Login endpoint (JWT token generation)
  - JWT token validation filter
  - Role-based access control (HOST / GUEST) using Spring Security
  - Swagger API docs
  - Automatic distributed tracing (Spring Boot Actuator + Micrometer)
  
- ✅ API Gateway:
  - Routes to all downstream services
  - Spring Cloud Gateway with routing configuration
  - Automatic trace ID propagation (Micrometer, W3C Trace Context)
  - Spring AI chatbot endpoint embedded (/api/ai/chat)
  - Global error handling with custom exception mappers

- ✅ Testing:
  - Unit tests: Password encoding, JWT generation, token validation
  - Integration tests: /register, /login, secured endpoint access (Testcontainers for MySQL)
  - CI on PR: Unit tests only

**Database:** MySQL (User Service)

**Design Patterns & OOP Implementation:**
- **Microservices Pattern:** Service per domain (User Service), clear boundaries
- **REST API Pattern:** 
  - POST /api/users/register (create resource)
  - POST /api/users/login (action endpoint)
  - GET /api/users/{id} (retrieve resource, secured)
- **Security Pattern:** 
  - JWT as Bearer token in Authorization header
  - Spring Security filter chain for stateless authentication
  - Role-based authorization using @PreAuthorize annotations
- **OOP:**
  - Encapsulation: UserService interface, UserServiceImpl implementation
  - Polymorphism: AuthenticationProvider implementations (JwtAuthProvider)
  - Abstraction: UserRepository (JPA interface)
  - Inheritance: BaseEntity with common fields (id, createdAt, updatedAt)
- **Exception Handling:**
  - Custom exceptions: UserAlreadyExistsException, InvalidCredentialsException
  - Global @ControllerAdvice for centralized error handling
  - Optional<User> for null-safe user lookups
- **Data Structures:**
  - ConcurrentHashMap for token blacklist (logout tokens)
  - ArrayList for role hierarchy
- **Java Memory Model:**
  - Immutable User entity after creation
  - Use Optional instead of null to reduce null-pointer exceptions
  - Stream API for filtering user roles

**Key Patterns:**
- JWT with RS256 (asymmetric signing for scalability)
- Role hierarchy (ADMIN > HOST > GUEST)
- Stateless authentication
- Spring Security with custom UserDetailsService

---

### Phase 2: Listing Service + Search + PostgreSQL Integration
**Objective:** Build listing creation and discovery with pagination and full-text search.

**Deliverables:**
- ✅ Listing Service:
  - Create listing endpoint (host only, input validation)
  - View all listings with pagination
  - Search/filter by location, price range, amenities (PostgreSQL full-text search)
  - Image URL storage (actual image handling post-MVP)
  - Swagger API docs
  - Automatic distributed tracing

- ✅ Testing:
  - Unit tests: Validation rules, search logic, pagination
  - Integration tests: REST API tests with Testcontainers (PostgreSQL)

**Database:** PostgreSQL (Listing Service)

**Design Patterns & OOP Implementation:**
- **REST API Pattern:**
  - POST /api/listings (create, Host role only)
  - GET /api/listings (list with pagination)
  - GET /api/listings/search?location=NYC&priceMin=100&priceMax=500 (search)
  - GET /api/listings/{id} (retrieve single)
  - PUT /api/listings/{id} (update, host only)
- **Microservices Pattern:** Independent database (PostgreSQL), clear API contract
- **OOP:**
  - Encapsulation: ListingService handles business logic
  - Polymorphism: ListingRepository with custom query methods
  - Abstraction: IListingService interface
  - Inheritance: BaseEntity (id, createdAt, updatedAt)
- **Exception Handling:**
  - Custom exceptions: ListingNotFoundException, UnauthorizedListingModificationException
  - Optional<Listing> for safe lookups
  - Validation using @Valid and custom validators
- **Data Structures:**
  - List<Listing> with Stream API for filtering
  - Page<Listing> for pagination (Spring Data)
  - Set<String> for amenities (unique values)
- **Java Memory Model:**
  - Lazy-load listing images (avoid loading unnecessary data)
  - Use projections for list queries (only required fields)
  - Stream API for efficient filtering: `listings.stream().filter(...).collect(Collectors.toList())`
- **Design Notes (Document in README):**
  - Listings are static metadata; availability is separate (why listings ≠ availability)
  - Search can be eventually consistent (cached in Redis, updated async)
  - Full-text search uses PostgreSQL's native capabilities (GIN/GIST index)
  - Pagination prevents memory bloat from large result sets

---

### Phase 3: Availability Service + Concurrency + Redis
**Objective:** Build booking calendar with double-booking prevention using distributed locking (interview-winning pattern).

**Deliverables:**
- ✅ Availability Service:
  - Calendar endpoint (date ranges, pricing)
  - Redis distributed locks for concurrency control
  - Prevent double booking with optimistic locking strategy
  - Time-boxed reservations (30-min hold)
  - Automatic distributed tracing
  - Pattern matching for availability state transitions

- ✅ Testing:
  - Unit tests: Availability logic, lock acquisition/release
  - Integration tests (Testcontainers):
    - Simulate 5+ concurrent booking attempts on same slot
    - Validate only one succeeds
    - Verify lock timeout and release

**Database:** PostgreSQL (Availability Service, shares schema with Listing for JOINs)

**Design Patterns & OOP Implementation:**
- **Microservices Pattern:** Independent database, async availability updates via Kafka
- **Concurrency Pattern:**
  - Distributed lock using Redis: `SET availability:{listingId}:{date} {reservationId} NX EX 1800`
  - Optimistic locking in database: version field on Availability table
  - Compare-and-swap (CAS) semantics for atomicity
- **REST API Pattern:**
  - GET /api/availability/{listingId}?checkIn=2026-02-15&checkOut=2026-02-20 (retrieve calendar)
  - POST /api/availability/reserve (acquire lock, initiate booking)
  - DELETE /api/availability/release/{reservationId} (release lock on timeout)
- **OOP:**
  - Encapsulation: AvailabilityLockService handles lock logic
  - Abstraction: IAvailabilityService interface, ILockProvider (Redis implementation)
  - Polymorphism: Multiple lock strategies (Redis, Database)
  - Inheritance: BaseEntity with version field for optimistic locking
- **Exception Handling:**
  - Custom exceptions: LockAcquisitionFailedException, AvailabilityAlreadyBookedException
  - Try-catch for timeout handling, Optional for lock state
  - Graceful degradation on Redis unavailability (fallback to DB lock)
- **Java 21 Features (Selective Adoption):**
  - Pattern matching for lock state: `if (lockStatus instanceof LockedBy(String reservationId, LocalDateTime expiresAt)) {...}`
  - Record Patterns for availability events: `case BookingReservedEvent(var listingId, var date, var reservationId) -> {...}`
- **Data Structures:**
  - ConcurrentHashMap for in-memory lock metadata cache
  - LinkedHashMap for FIFO queue of pending reservations
  - LocalDate/LocalDateTime for accurate date handling
- **Java Memory Model:**
  - Volatile keyword for lock status visibility across threads
  - Weak references for temporary reservation metadata
  - Stream API: `reservations.stream().filter(r -> r.isExpired()).forEach(r -> releaseLock(r))`
  - Immutable AvailabilitySlot records to prevent concurrent modification

**Concurrency Pattern (Detailed):**
```
1. Client POST /api/availability/reserve
2. AvailabilityService attempts: SET availability:{listingId}:{date} {reservationId} NX EX 1800
3. Redis Response:
   - OK → Lock acquired, reserve in DB with version=1
   - nil → Lock held by another, return 409 Conflict immediately
4. Background task: Release lock after 30 min or on PAYMENT_FAILED event
5. Async event: AVAILABILITY_RESERVED published (async with virtual thread)
```

**Interview Talking Points:**
- "Implemented distributed locking with Redis to prevent race conditions in high-concurrency booking scenarios"
- "Used optimistic locking with version fields as secondary safeguard for atomicity"
- "Tested with Testcontainers simulating 5+ concurrent requests to validate only 1 succeeds under contention"
- "Leveraged Java 21 virtual threads for async lock release without blocking thread pools"

---

### Phase 4: Booking Service + Saga Pattern + Kafka
**Objective:** Orchestrate multi-step booking flow with event-driven compensation using Saga pattern.

**Deliverables:**
- ✅ Booking Service:
  - Booking creation endpoint (initiates saga)
  - Saga orchestration with event-driven choreography
  - Event publishing to Kafka with automatic trace propagation:
    - BOOKING_CREATED → triggers Payment Service
    - PAYMENT_SUCCESS → confirms booking
    - PAYMENT_FAILED → compensates (releases availability)
    - BOOKING_CONFIRMED → notifies user
    - BOOKING_CANCELLED → cleanup
  - Swagger API docs
  - Automatic distributed tracing with async event propagation
  - Java 21 pattern matching for saga state transitions

- ✅ Testing:
  - Unit tests: Saga orchestration, compensation logic, state transitions
  - Integration tests (Testcontainers):
    - Kafka consumer/producer integration
    - End-to-end booking flow (success path)
    - Failure compensation scenarios

**Database:** MySQL (Booking Service)

**Design Patterns & OOP Implementation:**
- **Saga Pattern (Choreography-based):**
  - Each service consumes events and publishes compensation events
  - No central orchestrator (decoupled, scalable)
  - Clear event contracts for service communication
- **Microservices Pattern:** 
  - CQRS (Command Query Responsibility Segregation): booking command → events published
  - Event sourcing: immutable event log for auditing
  - Service-to-service communication via events (eventual consistency)
- **REST API Pattern:**
  - POST /api/bookings (initiate booking, starts saga)
  - GET /api/bookings/{id} (retrieve booking status)
  - GET /api/bookings/user/{userId} (user's booking history)
  - DELETE /api/bookings/{id} (cancel booking, triggers compensation)
- **OOP:**
  - Encapsulation: BookingOrchestrationService handles saga logic
  - Polymorphism: EventHandler interface for different event types
  - Abstraction: SagaOrchestrator (abstract base for multi-step processes)
  - Inheritance: BookingSaga extends AbstractSaga
- **Java 21 Features (Selective Adoption):**
  - **Pattern Matching for Saga State:**
    ```java
    switch (sagaEvent) {
        case PaymentSuccessEvent(Long paymentId, String txId) -> confirmBooking(paymentId, txId);
        case PaymentFailedEvent(Long paymentId, String reason) -> compensateAvailability(paymentId, reason);
        case BookingConfirmedEvent confirmed -> sendNotification(confirmed.userId());
        default -> logUnexpectedEvent(sagaEvent);
    }
    ```
  - **Virtual Threads for Event Handlers:** `@Async(value = "virtualThreadExecutor")` for non-blocking event processing
  - **Records for Events:** Automatic equals/hashCode for event comparison
- **Exception Handling:**
  - Custom exceptions: BookingInitiationFailedException, CompensationFailedException
  - Retry logic with exponential backoff (Resilience4j)
  - Dead-letter queue (DLQ) for failed compensation
  - Optional for nullable saga steps
- **Data Structures:**
  - ConcurrentHashMap for in-flight saga states
  - LinkedList for event ordering
  - Queue<SagaEvent> for event processing buffer
- **Java Memory Model:**
  - Immutable Event records (prevent accidental mutations)
  - Proper volatile access for saga state transitions
  - Stream API for filtering events: `events.stream().filter(e -> e.isCompensation()).forEach(...)`
  - Cleanup expired saga states to prevent memory leaks

**Saga Pattern (Choreography Flow):**
```
Booking Service                  Payment Service             Availability Service
        |                               |                             |
        +---BOOKING_CREATED────────────>|                             |
        |  (reserve availability)       |                             |
        |  (create booking record)      |                             |
        |                               |                             |
        |                    [Process Payment]                        |
        |                               |                             |
        |<──────PAYMENT_SUCCESS────────+                             |
        |  (update booking status)      |                             |
        |                               |                             |
        +────BOOKING_CONFIRMED─────────────────────────────────────>|
        |  (persist reservation)   (confirm reservation)             |
        |                               |                             |
        |                         [If Payment Fails]                 |
        |<──────PAYMENT_FAILED────────+                             |
        |  (update saga state)          |                             |
        |                               |                             |
        +──AVAILABILITY_RELEASE────────────────────────────────────>|
        |                               |                             |
        +──BOOKING_CANCELLED────────────────────────────────────────>|
        |  (cancel booking)             |           (release hold)   |
```

**Interview Talking Points:**
- "Implemented Saga pattern with event-driven choreography for multi-step booking orchestration"
- "Designed for eventual consistency with automatic compensation on payment failures"
- "Used Java 21 pattern matching for clean, readable saga state transitions"
- "Virtual threads enable non-blocking event handlers without thread pool exhaustion"

---

### Phase 5: Payment Service + Resilience + Idempotency
**Objective:** Integrate Stripe sandbox with fault tolerance, idempotency, and circuit breaker pattern.

**Deliverables:**
- ✅ Payment Service:
  - Stripe sandbox integration (test mode, free)
  - Mock payment gateway fallback
  - Idempotency key handling (prevent duplicate charges on retries)
  - Circuit breaker with Resilience4j:
    - Failure threshold: 5 failures
    - Wait duration: 60 seconds
    - Fallback to mock gateway
  - Retry logic (3 attempts, exponential backoff)
  - Timeout handling (5-second timeout)
  - Swagger API docs
  - Automatic distributed tracing with async callbacks
  - Java 21 virtual threads for async payment processing

- ✅ Testing:
  - Unit tests: Success/failure paths, idempotency validation
  - Integration tests (Testcontainers):
    - Payment success scenario
    - Payment failure simulation → verify saga compensation
    - Idempotency: replay same request, validate single charge
    - Circuit breaker open/half-open states

**Database:** MySQL (Payment Service)

**Design Patterns & OOP Implementation:**
- **Resilience Pattern:**
  - Circuit Breaker: prevent cascading failures
  - Retry with exponential backoff: handle transient failures
  - Timeout: prevent hanging requests
  - Fallback: degrade gracefully (mock payment)
  - Bulkhead: isolate thread pools per operation
- **Microservices Pattern:**
  - Service-to-service via events (async calls)
  - External service integration with resilience
  - Compensation for failed payments
- **REST API Pattern:**
  - POST /api/payments/process (idempotency key in header: X-Idempotency-Key)
  - GET /api/payments/{id} (retrieve payment status)
  - POST /api/payments/{id}/refund (refund initiated by booking cancellation)
- **OOP:**
  - Encapsulation: PaymentProcessor handles Stripe integration
  - Polymorphism: PaymentGateway interface (StripePaymentGateway, MockPaymentGateway)
  - Abstraction: IPaymentService with multiple implementations
  - Inheritance: BasePaymentProcessor with retry/timeout logic
- **Exception Handling:**
  - Custom exceptions: StripeException, IdempotencyKeyViolatedException, PaymentTimeoutException
  - Global error handler maps exceptions to proper HTTP responses
  - Optional for nullable payment responses
  - Try-with-resources for Stripe client management
- **Java 21 Features (Selective Adoption):**
  - **Virtual Threads for Async Callbacks:** 
    ```java
    @Async
    public CompletableFuture<PaymentResult> processPaymentAsync(PaymentRequest req) {
        return CompletableFuture.supplyAsync(
            () -> stripeClient.charge(req),
            virtualThreadExecutor
        );
    }
    ```
  - **Record Patterns for Payment Events:**
    ```java
    record PaymentRequest(Long bookingId, BigDecimal amount, String currency) {}
    record PaymentResult(Long transactionId, String status, LocalDateTime timestamp) {}
    ```
- **Data Structures:**
  - ConcurrentHashMap for idempotency key tracking
  - LinkedHashMap for transaction history
  - Queue for payment processing order
  - Set for duplicate detection
- **Java Memory Model:**
  - AtomicReference for thread-safe payment state
  - Volatile for circuit breaker status visibility
  - Stream API: `transactionHistory.stream().filter(t -> t.isFailed()).map(...)`
  - Cleanup old idempotency keys after 24 hours to prevent memory bloat

**Stripe Sandbox Details:**
- Free test account (no credit card required for test mode)
- Test card: 4242 4242 4242 4242 (success) | 4000 0000 0000 0002 (decline)
- Fallback: Mock stripe client for local testing without internet
- Idempotency: Same request with same key returns same response (Stripe-native feature)

**Resilience4j Config:**
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60000
        permitted-number-of-calls-in-half-open-state: 3
    instances:
      stripePaymentClient:
        registerHealthIndicator: true
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1000
        retry-exceptions:
          - java.io.IOException
          - com.stripe.exception.StripeException
  timelimiter:
    configs:
      default:
        timeout-duration: 5s
    instances:
      stripePaymentClient:
        timeout-duration: 5s
  bulkhead:
    configs:
      default:
        max-concurrent-calls: 100
        max-wait-duration: 10ms
```

**Interview Talking Points:**
- "Implemented Circuit Breaker pattern using Resilience4j for Stripe payment integration with exponential backoff and fallback to mock gateway"
- "Ensured idempotency through unique keys preventing duplicate charges on retry scenarios"
- "Used Java 21 virtual threads for non-blocking async payment callbacks without thread pool exhaustion"

---

### Phase 6: Notification Service + Observability Enhancement
**Objective:** Consume Kafka events with virtual threads, simulate notifications, and implement distributed tracing.

**Deliverables:**
- ✅ Notification Service:
  - Kafka consumer for all booking events (async with virtual threads)
  - Email/SMS simulation (log to file/console with structured logging)
  - Event handlers for:
    - BOOKING_CONFIRMED → "Your booking is confirmed"
    - PAYMENT_FAILED → "Payment failed, please retry"
    - BOOKING_CANCELLED → "Booking cancelled, refund initiated"
  - Automatic distributed tracing with async propagation
  - Swagger API docs (notification history endpoint)
  - Java 21 pattern matching for event type routing

- ✅ Observability Enhancement (All Services):
  - Structured logging (JSON format with trace ID, span ID, service name)
  - Spring Boot Actuator + Micrometer for automatic tracing
  - W3C Trace Context standard (traceparent header propagation)
  - Trace ID propagation:
    - HTTP headers: `traceparent` (W3C standard)
    - Kafka headers: automatic via Micrometer Tracing
  - Actuator endpoints: /actuator/metrics, /actuator/health

- ✅ Testing:
  - Unit tests: Event handler logic, message formatting
  - Integration tests (Testcontainers):
    - Kafka consumption from booking service
    - Verify notification logs for each event type
    - End-to-end trace validation

**Database:** None (Stateless consumer)

**Design Patterns & OOP Implementation:**
- **Microservices Pattern:**
  - Event-driven consumer: decoupled from event producers
  - Async processing with virtual threads: non-blocking I/O
  - Eventual consistency: guaranteed delivery (Kafka partitions)
- **Observer Pattern:**
  - NotificationService listens to Kafka topics
  - Multiple event handlers (polymorphic)
  - Loose coupling between services
- **REST API Pattern:**
  - GET /api/notifications/user/{userId} (retrieve notification history)
  - PUT /api/notifications/{id}/read (mark notification as read)
- **OOP:**
  - Encapsulation: NotificationProcessor handles notification logic
  - Polymorphism: NotificationHandler interface (EmailHandler, SMSHandler, LogHandler)
  - Abstraction: INotificationService
  - Inheritance: BaseNotificationHandler with common retry logic
- **Java 21 Features (Selective Adoption):**
  - **Virtual Threads for Event Consumers:** 
    ```java
    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @KafkaListener(topics = "booking.confirmed")
    @Async("virtualThreadExecutor")
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        sendNotification(event);
    }
    ```
  - **Pattern Matching for Event Routing:**
    ```java
    public void processEvent(DomainEvent event) {
        switch (event) {
            case BookingConfirmedEvent(Long bookingId, Long userId, LocalDate checkIn) -> 
                sendBookingConfirmationEmail(userId, bookingId, checkIn);
            case PaymentFailedEvent(Long paymentId, String reason) -> 
                sendPaymentFailureAlert(paymentId, reason);
            case BookingCancelledEvent(Long bookingId, BigDecimal refund) -> 
                sendCancellationEmail(bookingId, refund);
            default -> logUnexpectedEvent(event);
        }
    }
    ```
- **Exception Handling:**
  - Custom exceptions: NotificationSendFailedException, InvalidEventException
  - Retry logic for failed notifications (dead-letter queue)
  - Optional for nullable notification recipients
  - Try-catch for Kafka deserialization errors
- **Data Structures:**
  - Queue<Notification> for pending notifications (async processing)
  - ConcurrentHashMap for notification tracking
  - LinkedList for notification history
  - Set<String> for recipient deduplication
- **Java Memory Model:**
  - Immutable Notification records (thread-safe by default)
  - Volatile for event handler state
  - Stream API: `notifications.stream().filter(n -> !n.isRead()).forEach(...)`
  - Cleanup old notifications periodically to prevent unbounded memory growth

**Observability with Spring Boot Actuator + Micrometer:**
```java
// common-lib: ObservabilityConfiguration.java
@Configuration
public class ObservabilityConfiguration {
    
    @Bean
    public MicrometerTracingBeanPostProcessor micrometerTracingBeanPostProcessor(Tracer tracer) {
        return new MicrometerTracingBeanPostProcessor(tracer);
    }
    
    @Bean
    public JwtTokenProvider jwtTokenProvider(Tracer tracer) {
        return new JwtTokenProvider(tracer); // traces token generation
    }
}

// application.yml (all services)
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
  tracing:
    sampling:
      probability: 1.0 # sample all requests
```

**Structured Logging Example:**
```java
// All services use structured logging
@Slf4j
public class BookingEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(BookingEventHandler.class);
    
    public void handleEvent(BookingCreatedEvent event) {
        logger.info("Processing booking event", 
            "bookingId", event.bookingId(),
            "userId", event.userId(),
            "amount", event.totalPrice(),
            "traceId", MDC.get("traceId"),
            "spanId", MDC.get("spanId")
        );
    }
}
```

**Interview Talking Points:**
- "Implemented distributed tracing with Spring Boot Actuator + Micrometer across all services"
- "Used Java 21 virtual threads for non-blocking Kafka event processing without thread pool saturation"
- "Pattern matching simplified event type handling with exhaustiveness checking"
- "Structured JSON logging with trace/span IDs enables centralized debugging across services"

---

### Phase 7: Integration, Polish & Documentation
**Objective:** End-to-end validation, comprehensive documentation, and interview readiness.

**Deliverables:**
- ✅ End-to-End Testing:
  - Manual scenario: User registration → Search listing → Availability check → Book → Payment → Notification
  - Trace validation: Single trace ID across all services and async Kafka events
  - Failure scenarios: Payment decline, double booking attempt, service timeout, lock release
  - Concurrency test: 5+ simultaneous bookings on same listing

- ✅ GitHub Actions CI/CD (Full Pipeline):
  - Trigger: PR → run unit tests only (~3 min)
  - Trigger: Merge to main → run unit tests + build Docker images + push to Docker Hub
  - Docker image naming: `{dockerhub_username}/airbnb-{service-name}:latest`
  - Local deployment: Docker Compose pulls images from Docker Hub
  - Dockerfile standards: Non-root user (appuser), multi-stage builds, minimal image size, health checks

- ✅ Architecture Documentation:
  - High-level system diagram (services, databases, Kafka, Redis, load balancing)
  - Request flow diagrams (sync and async paths)
  - Saga compensation flow diagram
  - Concurrency handling flow (Redis lock + DB optimistic locking)
  - Trace propagation diagram (W3C Trace Context)

- ✅ API Documentation:
  - OpenAPI/Swagger specs for all services (auto-generated from code)
  - cURL examples:
    - User registration and login
    - Listing search and filtering
    - Availability check and reservation
    - Booking creation and payment
    - Notification history
  - Error response examples with HTTP status codes
  - Request/response payloads with descriptions

- ✅ Technical Deep Dives (Document in README):
  - **Concurrency Handling:** 
    - Redis distributed locks for atomicity
    - Optimistic locking with version fields as fallback
    - Testcontainers validation of race condition prevention
    - Java Memory Model implications (volatile, happens-before)
  - **Saga Pattern Explanation:**
    - Event-driven choreography (no central coordinator)
    - Compensation logic for failure scenarios
    - Idempotency considerations (at-least-once delivery)
    - Trade-offs vs. orchestration pattern
  - **CI/CD Overview:**
    - GitHub Actions workflow with stages
    - Build caching for faster pipelines
    - Multi-arch Docker builds (ARM64 support)
  - **Testing Strategy:**
    - Unit tests: Mockito for dependencies, ~70% coverage
    - Integration tests: Testcontainers for databases, Kafka, Redis (~30% coverage)
    - Total coverage target: 85%+
    - Test isolation and cleanup strategies
  - **Java 21 Feature Usage:**
    - Virtual threads for async event consumers (non-blocking I/O benefit)
    - Record patterns for event type routing (pattern exhaustiveness)
    - Pattern matching in switch for saga state transitions
    - Memory efficiency gains from Records (automatic equals/hashCode)
  - **Design Patterns Used:**
    - **Architectural:** Microservices, CQRS (Booking Service)
    - **Behavioral:** Observer (Kafka consumers), Strategy (PaymentGateway)
    - **Structural:** Adapter (MockPaymentGateway), Decorator (Resilience4j)
    - **Concurrency:** Double-checked locking (Redis + DB), Compare-and-swap
    - **Resilience:** Circuit Breaker, Retry, Bulkhead, Timeout
  - **OOP Principles Applied:**
    - **Encapsulation:** Services expose APIs, hide implementation
    - **Abstraction:** Interfaces for flexible implementations
    - **Inheritance:** Base classes (BaseEntity, BaseService)
    - **Polymorphism:** Multiple payment gateway implementations, event handlers
  - **Java Memory Model:**
    - Immutable Records reduce GC pressure
    - Proper volatile usage for shared state in concurrent classes
    - Stream API efficiency for large collections
    - Weak references for temporary caches
    - Cleanup strategies for expired data

- ✅ README Components:
  - Quick start guide (clone, build, docker-compose up)
  - Architecture diagram (Mermaid)
  - Service descriptions with responsibilities
  - Tech stack justification
  - Booking flow explanation with sequence diagram
  - Concurrency prevention mechanism
  - Saga compensation walkthrough
  - Distributed tracing visualization
  - Testing strategy overview
  - Java 21 feature highlights
  - Known limitations and trade-offs
  - Performance benchmarks (optional)
  - Deployment instructions (local, Docker)

- ✅ Interview Preparation:
  - Resume bullets (6-7 key achievements)
  - Common interview questions and answers
  - Trade-off discussions (consistency vs availability, sync vs async)
  - Failure scenario explanations
  - Scalability considerations

**Design Patterns & OOP Summary:**

| Category | Pattern | Usage |
|----------|---------|-------|
| **Architectural** | Microservices | 6 independent services |
| | CQRS | Booking command → event log |
| | Event Sourcing | Immutable event audit trail |
| **Resilience** | Circuit Breaker | Stripe payment integration |
| | Retry | Transient failure handling |
| | Bulkhead | Thread pool isolation |
| | Timeout | Prevent hanging requests |
| **Concurrency** | Distributed Locking | Redis for availability |
| | Optimistic Locking | DB version field |
| | Double-Checked Locking | State initialization |
| **Data** | Repository | Spring Data JPA |
| | DTO (Records) | Service boundaries |
| **Behavioral** | Observer | Kafka event consumers |
| | Strategy | PaymentGateway implementations |
| | State | Booking/Saga state machine |
| **Structural** | Adapter | MockPaymentGateway |
| | Decorator | Resilience4j wrappers |
| | Facade | API Gateway routing |

**OOP Principles Application:**

```java
// Encapsulation: Service hides details
public interface BookingService {
    BookingDTO createBooking(BookingRequest request);
}

// Abstraction: Interface contract
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
}

// Inheritance: Base class for common behavior
public abstract class BaseService<T, ID> {
    protected void logOperation(String operation) { }
}

// Polymorphism: Multiple implementations
public class StripePaymentGateway implements PaymentGateway { }
public class MockPaymentGateway implements PaymentGateway { }

// Java 21 Records: Immutable value objects
public record BookingCreatedEvent(
    Long bookingId,
    Long listingId,
    Long userId,
    BigDecimal totalPrice
) {}
```

**Exception Handling Strategy:**

```java
// Custom exceptions with context
public class BookingException extends RuntimeException {
    private final String bookingId;
    private final ErrorCode errorCode;
}

// Global error handler
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ErrorResponse> handleBookingException(BookingException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }
}

// Optional for null safety
public Optional<Booking> findBooking(Long id) {
    return bookingRepository.findById(id)
        .filter(b -> !b.isCancelled());
}
```

**Interview Talking Points:**
- "Designed a distributed microservices platform with clear separation of concerns using design patterns"
- "Implemented concurrency control through Redis distributed locking and DB optimistic locking"
- "Leveraged Java 21 features (virtual threads, records, pattern matching) for improved readability and performance"
- "Used comprehensive testing strategy with unit tests (Mockito) and integration tests (Testcontainers)"
- "Applied SOLID principles throughout: Single Responsibility (services), Open/Closed (interfaces), Liskov Substitution (implementations)"
- "Implemented robust error handling with custom exceptions and global error handlers"
- "Used Java Stream API and Optional for functional-style null-safe data processing"
- "Designed for memory efficiency: immutable records, proper GC behavior, no memory leaks"

---

## Testing Strategy

### Unit Tests (All Services)
- **Coverage:** Business logic, validation, calculations, null handling
- **Tools:** JUnit 5, Mockito, AssertJ
- **Isolation:** Mockito for all dependencies (services, repositories, external clients)
- **Examples:**
  - User password encoding validation (BCrypt)
  - JWT token generation, expiration, validation
  - Listing search filter logic with Stream API
  - Availability calendar logic with date calculations
  - Saga compensation logic with state transitions
  - Payment idempotency checks with Optional
  - Exception handling paths (custom exceptions)
  - OOP principles validation (polymorphism through mocks)

**Unit Test Best Practices:**
```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock private AvailabilityService availabilityService;
    @Mock private PaymentService paymentService;
    @InjectMocks private BookingService bookingService;
    
    @Test
    void shouldCreateBookingWhenAvailabilityExists() {
        // Arrange
        when(availabilityService.reserve(anyLong(), any()))
            .thenReturn(Optional.of(new Reservation(...)));
        
        // Act
        BookingDTO result = bookingService.createBooking(request);
        
        // Assert
        assertThat(result).isNotNull();
        verify(paymentService, times(1)).processPayment(any());
    }
}
```

### Integration Tests (With Testcontainers)
- **Coverage:** Database interactions, API endpoints, external dependencies, event streaming
- **Container Setup:**
  - User/Booking/Payment: MySQL Testcontainer
  - Listing/Availability: PostgreSQL Testcontainer
  - All services: Kafka + Zookeeper Testcontainers
  - Cache tests: Redis Testcontainer
  - Shared infrastructure via Docker Compose for complex scenarios

- **Examples:**
  - End-to-end booking flow (registration → search → book → pay → notify)
  - Concurrent booking attempts (5+ threads on same slot, validate only 1 succeeds)
  - Payment failure + saga compensation (verify availability released)
  - Kafka event consumption (verify listeners receive and process events)
  - Idempotency: replay payment request, validate single charge
  - Circuit breaker state transitions (open, half-open, closed)
  - Search with pagination (PostgreSQL full-text search)
  - Distributed tracing (verify trace ID propagation across async calls)

**Integration Test Best Practices:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @Test
    void shouldPreventDoubleBooingWhenConcurrentRequests() throws InterruptedException {
        // Launch 5 threads, attempt to book same slot
        // Verify only 1 succeeds with 200 OK
        // Verify 4 others get 409 Conflict
    }
}
```

### CI/CD Testing
- **PR Trigger:** Unit tests only (~3-5 min, fast feedback)
- **Main Branch:** Unit tests → Integration tests (optional, slower) → Docker build → Push
- **Local Testing:** Run all tests before final merge to main
- **Coverage Target:** 85%+ (70% unit, 30% integration)

**Test Naming Convention:**
- Unit: `should{ExpectedBehavior}When{Condition}`
- Integration: `shouldHandle{Scenario}With{Setup}`
- Example: `shouldCreateBookingWhenAvailabilityExistsAndPaymentSucceeds`

**Test Data Builders (Builder Pattern for test setup):**
```java
public class BookingBuilder {
    private Long userId = 1L;
    private Long listingId = 1L;
    private LocalDate checkIn = LocalDate.now().plusDays(1);
    private LocalDate checkOut = LocalDate.now().plusDays(3);
    
    public BookingBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }
    
    public Booking build() {
        return new Booking(userId, listingId, checkIn, checkOut);
    }
}
```

---

## Database Strategy

### MySQL (User, Booking, Payment Services)
- **User Service:** Users, Roles, JWT tokens (audit trail)
- **Booking Service:** Bookings, Booking history, Saga state
- **Payment Service:** Payment records, Idempotency keys, Stripe transaction logs

### PostgreSQL (Listing, Availability Services)
- **Listing Service:** Listings, Amenities, Reviews, Full-text search
- **Availability Service:** Availability calendar, Pricing tiers, Reservations
- **Shared Schema:** Listing ↔ Availability relationship (foreign keys)

### Redis
- **Purpose:** Distributed locking, caching, session management
- **Keys:**
  - `availability:{listingId}:{date}` → reservation ID (lock)
  - `listing:search:{hash}` → cached search results (TTL: 1 hour)
  - `user:session:{token}` → token metadata (TTL: 24 hours)

---

## Kafka Topics & Event Schema

| Topic | Producer | Consumer | Schema |
|-------|----------|----------|--------|
| `booking.created` | Booking Service | Payment Service | `{bookingId, listingId, userId, totalPrice, currency}` |
| `payment.success` | Payment Service | Booking Service, Notification Service | `{paymentId, bookingId, amount, stripeTransactionId}` |
| `payment.failed` | Payment Service | Booking Service, Availability Service | `{paymentId, bookingId, reason}` |
| `booking.confirmed` | Booking Service | Notification Service | `{bookingId, userId, checkInDate, checkOutDate}` |
| `booking.cancelled` | Booking Service | Notification Service, Availability Service | `{bookingId, reason, refundAmount}` |

---

## Spring AI Chatbot Integration (Embedded in API Gateway)

**Endpoint:** `POST /api/ai/chat`

**Purpose:** Help users discover API workflows and troubleshoot

**Examples:**
- **User:** "How do I search for listings?"
  - **AI:** "Use GET /api/listings/search?location=NYC&checkIn=2026-02-15"
  
- **User:** "I got a payment error, what should I do?"
  - **AI:** "Check if your card is valid (test: 4242...). If it fails, try again after 60 seconds (circuit breaker cooldown)."

**Implementation:**
- Spring AI client in API Gateway
- Context: Service documentation, API endpoints, error codes
- Async processing to avoid blocking requests
- Zipkin tracing for AI calls

---

## Interview Talking Points (Resume Bullets)

### 1. Architecture & System Design
- "Designed a distributed microservices-based booking platform using Java 17 for stability with selective adoption of Java 21 features (virtual threads, records, pattern matching) for improved concurrency and code clarity."
- "Implemented clear microservices boundaries (6 independent services) with event-driven communication via Kafka, enabling loose coupling and independent scaling."
- "Utilized both MySQL (transactional services) and PostgreSQL (analytics, full-text search) to optimize database selection per access patterns and consistency requirements."

### 2. Distributed Systems & Patterns
- "Implemented Saga pattern with event-driven choreography for multi-step booking orchestration, ensuring automatic compensation on payment failures without central coordinator."
- "Designed CQRS architecture in Booking Service: commands (create booking) trigger immutable event log for complete audit trail and temporal queries."
- "Achieved eventual consistency through event streaming, validating with integration tests that all services reach consistent state despite transient failures."

### 3. Concurrency & Resilience
- "Prevented double-booking through Redis-based distributed locking (SET with NX/EX flags) combined with database optimistic locking (version fields) for dual-layer atomicity."
- "Validated concurrency handling using Testcontainers to simulate 5+ concurrent booking attempts, ensuring only 1 succeeds and others receive 409 Conflict atomically."
- "Implemented Circuit Breaker pattern (Resilience4j) for Stripe payment integration: handles failures gracefully, exponential backoff, and fallback to mock gateway during outages."

### 4. Java Platform Excellence
- **Java 21 Features:**
  - "Used Java 21 virtual threads for Kafka event consumers (Booking, Payment, Notification services), eliminating thread pool bottlenecks and enabling massive concurrency (100K+ concurrent operations)."
  - "Implemented Java Records for DTOs and Kafka events, reducing boilerplate by ~30 lines per class while gaining automatic equals(), hashCode(), toString() and improved GC pressure."
  - "Applied pattern matching in switch expressions for saga state transitions and event routing, improving code readability and ensuring exhaustiveness checking at compile time."
- **Memory & Performance:**
  - "Optimized memory usage through immutable Records instead of mutable POJOs, reducing GC pause times by ~40% in async event processing."
  - "Used Stream API efficiently: lazy evaluation for filtering bookings/listings prevents loading entire collections into memory."
  - "Applied Java Memory Model: proper volatile usage for shared state in concurrent classes (PaymentState, SagaState), ensuring visibility across threads."

### 5. Testing & Quality Assurance
- "Achieved 85%+ test coverage with dual-layer approach: unit tests (70%) using Mockito for isolation, and integration tests (30%) with Testcontainers validating real dependencies."
- "Implemented end-to-end booking flow tests spanning all 6 services, validating complete transaction from registration through payment to notification."
- "Tested race condition prevention: Testcontainers-based concurrent booking scenario validates atomicity and correct lock semantics under contention."

### 6. Error Handling & Resilience
- "Designed comprehensive exception hierarchy: custom exceptions (BookingException, PaymentException) with context, global @ControllerAdvice for centralized error mapping to appropriate HTTP status codes."
- "Implemented null safety using Optional<T>: eliminates null-pointer exceptions in booking lookups (findBooking, findUser) through functional-style chaining."
- "Built retry logic with exponential backoff for transient failures (3 attempts, 1s wait with 2x multiplier), preventing cascading failures in distributed system."

### 7. Observability & Tracing
- "Implemented W3C Trace Context standard with Spring Boot Actuator + Micrometer for automatic distributed tracing across all services."
- "Propagated trace IDs through HTTP headers (traceparent) and Kafka messages, enabling end-to-end request visibility from user API call through async event processing."
- "Structured logging in JSON format with trace ID and span ID on every log statement, enabling correlation of logs across services for debugging production issues."

### 8. CI/CD & DevOps
- "Built GitHub Actions CI/CD pipeline: unit tests on PRs (~3 min for fast feedback), full pipeline on main merge (tests → Docker build → push to Docker Hub)."
- "Containerized all services with Dockerfile best practices: non-root user (appuser), multi-stage builds, minimal image layers, health checks for orchestration."
- "Designed for local development consistency: docker-compose.yml with all infrastructure (MySQL, PostgreSQL, Kafka, Redis) ensures reproducible environment across team."

### 9. Design Patterns & OOP Mastery
- **Architectural Patterns:** Microservices, CQRS (Booking Service), Event Sourcing (immutable event log), API Gateway
- **Behavioral Patterns:** Observer (Kafka consumers), Strategy (PaymentGateway implementations), State (Booking state machine)
- **Structural Patterns:** Adapter (MockPaymentGateway), Decorator (Resilience4j wrappers)
- **Concurrency Patterns:** Distributed locking, compare-and-swap, double-checked locking
- **OOP Principles:**
  - Encapsulation: Services hide implementation, expose clean APIs
  - Abstraction: Interfaces for flexibility (PaymentGateway, NotificationHandler)
  - Inheritance: Base classes for shared behavior (BaseEntity, BaseService)
  - Polymorphism: Multiple payment gateway and event handler implementations switchable at runtime

### 10. REST API & Microservices Design
- "Applied REST design principles: proper HTTP methods (POST/PUT/GET/DELETE), resource-oriented URLs, stateless operations, consistent error responses."
- "Implemented idempotency through unique keys (X-Idempotency-Key header) for payment processing, preventing duplicate charges on client retries."
- "Designed service contracts with clear request/response schemas using Records, versioning strategy for backward compatibility."

**Resume-Ready One-Liners (Pick 2-3 for each interview):**
1. "Designed a distributed Java 21 microservices booking platform with Saga pattern, Redis locking, and comprehensive testing."
2. "Implemented double-booking prevention using Redis distributed locking validated through Testcontainers concurrency testing."
3. "Built resilient payment service with Resilience4j circuit breaker, Stripe integration, and idempotency for production reliability."
4. "Applied Java 21 features (virtual threads, records, pattern matching) for modern concurrent systems without blocking threads."
5. "Achieved 85%+ test coverage with unit tests (Mockito) and integration tests (Testcontainers) spanning microservices."
6. "Implemented W3C Trace Context distributed tracing across 6 services for complete request observability."
7. "Designed CQRS architecture with event sourcing for immutable audit trail and temporal queries in booking system."
8. "Used proper exception handling, Optional, and Stream API demonstrating Java best practices and null safety."

---

## Timeline Overview

| Phase | Duration | Key Focus | Interview Value |
|-------|----------|-----------|-----------------|
| 0 | ~6 hrs | Infrastructure, Maven setup, docker-compose | Foundation solid |
| 1 | ~10 hrs | Auth, JWT, API Gateway | Security expertise |
| 2 | ~8 hrs | Listing, Search, PostgreSQL | Data patterns |
| 3 | ~10 hrs | Concurrency, Redis locks | **High interview impact** |
| 4 | ~10 hrs | Saga, Kafka events | **Distributed systems** |
| 5 | ~10 hrs | Payment, Resilience4j, Idempotency | Fault tolerance |
| 6 | ~8 hrs | Notifications, Zipkin, Observability | Production-ready |
| 7 | ~8 hrs | E2E testing, docs, polish | Maturity |

**Total:** ~70 hours (7 days @ ~10 hrs/day)

---

## Success Criteria

- ✅ All 6 microservices + 3 infrastructure services running in Docker Compose
- ✅ End-to-end booking flow validated (registration → listing → booking → payment → notification)
- ✅ Zipkin shows single trace ID across all services
- ✅ Concurrent booking attempts: only 1 succeeds, others get 409 Conflict
- ✅ Payment failure triggers saga compensation
- ✅ GitHub Actions CI/CD pushes images to Docker Hub
- ✅ Comprehensive README with diagrams and API examples
- ✅ 85%+ test coverage (unit + integration)

---

## Quick Start Commands

```bash
# Clone and setup
git clone https://github.com/TanzeemAlam/airbnb-booking-platform.git
cd airbnb-booking-platform

# Build all services
mvn clean package -DskipTests

# Start infrastructure
docker-compose up -d

# Wait for services to be healthy (Eureka, Kafka, Databases)
# Then access:
# - API Gateway: http://localhost:8000
# - Eureka: http://localhost:8761
# - Zipkin: http://localhost:9411
# - Swagger: http://localhost:8000/swagger-ui.html
```

---

## Final Checklist

### Code Quality & Standards
- [ ] All services use Java Records for DTOs and Kafka events
- [ ] Exception handling: custom exceptions with context + global @ControllerAdvice
- [ ] Optional<T> used for null-safe operations (no null checks)
- [ ] Stream API used for filtering and transformations
- [ ] OOP principles applied: inheritance (BaseEntity), polymorphism (interfaces), encapsulation (private fields)
- [ ] Immutable objects used where appropriate (Records, enums for constants)
- [ ] Proper data structure selection (ConcurrentHashMap, LinkedHashMap, Set)
- [ ] Java Memory Model: volatile for shared state, proper thread synchronization
- [ ] Java 21 features: virtual threads in async consumers, pattern matching in switch, records for DTOs

### Design Patterns
- [ ] Microservices pattern: independent services with clear boundaries
- [ ] Saga pattern: event-driven compensation
- [ ] CQRS: separation of command and query (Booking Service)
- [ ] Circuit Breaker: Resilience4j for external service calls
- [ ] Retry & Timeout: transient failure handling
- [ ] Observer: Kafka listeners for async events
- [ ] Strategy: PaymentGateway implementations
- [ ] Repository: Spring Data JPA for data access
- [ ] DTO: Records for inter-service communication
- [ ] API Gateway: routing and load balancing

### Infrastructure & Setup
- [ ] Parent pom.xml with dependency management
- [ ] All service modules created via Spring Initializer
- [ ] common-lib with shared DTOs, events, constants
- [ ] docker-compose.yml with MySQL, PostgreSQL, Kafka, Zookeeper, Redis
- [ ] Non-root user in all Dockerfiles
- [ ] Spring Boot auto-config for distributed tracing (Micrometer)
- [ ] Health checks in docker-compose for service readiness

### Testing
- [ ] Unit tests for all services (70% coverage target)
- [ ] Integration tests with Testcontainers (30% coverage target)
- [ ] Concurrent booking tests (5+ threads, validate atomicity)
- [ ] Payment failure + saga compensation tests
- [ ] Kafka event consumption tests
- [ ] Trace ID propagation validation tests
- [ ] Test naming convention: should{Behavior}When{Condition}
- [ ] Test data builders for clean setup

### API & Documentation
- [ ] Swagger/OpenAPI enabled for all services
- [ ] cURL examples for all endpoints
- [ ] Error response documentation with HTTP status codes
- [ ] API design patterns (REST): proper HTTP methods, resource URIs

### CI/CD
- [ ] GitHub Actions workflow created
- [ ] PR trigger: unit tests only
- [ ] Main merge trigger: full pipeline (tests → build → push)
- [ ] Docker images pushed to Docker Hub
- [ ] Dockerfile standards: multi-stage builds, minimal size

### README & Documentation
- [ ] Architecture diagram (Mermaid/ASCII)
- [ ] Service descriptions and responsibilities
- [ ] Quick start guide (clone, build, docker-compose up)
- [ ] Booking flow diagram with saga explanation
- [ ] Concurrency prevention mechanism explanation
- [ ] Distributed tracing walkthrough
- [ ] Testing strategy overview
- [ ] Java 21 features highlights
- [ ] Design patterns table
- [ ] OOP principles application examples
- [ ] Exception handling strategy
- [ ] Resume bullets (8-10 key achievements)
- [ ] Interview Q&A section
- [ ] Known limitations and trade-offs
- [ ] Performance considerations

### Code Style
- [ ] Consistent naming: services (BookingService), entities (Booking), DTOs (BookingDTO)
- [ ] Proper logging: structured JSON with trace ID/span ID
- [ ] Comments: explain "why" not "what"
- [ ] No code duplication: use abstract base classes
- [ ] Consistent error handling across all services

---

**Status:** Ready for execution. Begin with Phase 0.

