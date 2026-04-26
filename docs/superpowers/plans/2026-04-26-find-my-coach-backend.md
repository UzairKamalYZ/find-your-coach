# Find-My-Coach Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot 3 REST API in `backend/` that replaces the frontend's mock auth and localStorage with a real PostgreSQL-backed service.

**Architecture:** Feature-slice packages (auth/user/trainer/venue/booking/common). Two Spring Security filter chains: one for the Google OAuth2 browser flow, one stateless JWT chain for the API. Testcontainers + MockMvc for integration tests; Mockito for unit tests.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Maven, Spring Security 6, Google OAuth2 OIDC, JWT RS256, Spring Data JPA, PostgreSQL 15, Testcontainers, JaCoCo.

---

## File Map

```
backend/
├── pom.xml
├── src/main/java/com/findmycoach/
│   ├── FindMyCoachApplication.java
│   ├── auth/
│   │   ├── AuthController.java          POST /auth/refresh, /auth/logout, /auth/complete-profile
│   │   ├── AuthService.java             refresh + logout + complete-profile logic
│   │   ├── TokenService.java            generate/validate access + refresh tokens
│   │   ├── RefreshToken.java            entity
│   │   ├── RefreshTokenRepository.java
│   │   └── dto/
│   │       ├── AuthResponse.java        { accessToken, refreshToken, user? }
│   │       ├── RefreshRequest.java      { refreshToken }
│   │       └── CompleteProfileRequest.java  { city }
│   ├── user/
│   │   ├── User.java                    entity
│   │   ├── UserRepository.java
│   │   ├── UserController.java          GET/PATCH /me
│   │   ├── UserService.java
│   │   └── dto/
│   │       ├── UserResponse.java
│   │       └── UpdateProfileRequest.java
│   ├── trainer/
│   │   ├── Trainer.java                 entity
│   │   ├── TrainerRepository.java
│   │   ├── TrainerController.java       GET /trainers, /trainers/{id}, /trainers/{id}/availability
│   │   ├── TrainerService.java          availability slot logic
│   │   └── dto/
│   │       ├── TrainerResponse.java
│   │       └── DayAvailability.java     { date, slots[] }
│   ├── venue/
│   │   ├── Venue.java
│   │   ├── VenueRepository.java
│   │   ├── VenueController.java         GET /venues
│   │   ├── VenueService.java
│   │   └── dto/VenueResponse.java
│   ├── booking/
│   │   ├── Booking.java
│   │   ├── BookingRepository.java
│   │   ├── BookingController.java       POST/GET/DELETE /bookings
│   │   ├── BookingService.java          conflict detection, cancel rules
│   │   └── dto/
│   │       ├── CreateBookingRequest.java
│   │       └── BookingResponse.java
│   └── common/
│       ├── security/
│       │   ├── SecurityConfig.java      two SecurityFilterChain beans
│       │   ├── JwtConfig.java           @ConfigurationProperties app.jwt.*
│       │   ├── CustomOidcUserService.java   create/update User after Google login
│       │   ├── OAuth2SuccessHandler.java    issue JWT, redirect to frontend
│       │   └── ProfileCompleteFilter.java   enforce profileComplete claim
│       ├── exception/
│       │   ├── AppException.java        base RuntimeException with HTTP status
│       │   ├── ErrorResponse.java       { error, details }
│       │   └── GlobalExceptionHandler.java  @RestControllerAdvice
│       └── seed/
│           └── DataSeeder.java          seeds trainers + venues; creates partial unique index
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/com/findmycoach/
    │   ├── support/
    │   │   ├── BaseIntegrationTest.java     Testcontainers + MockMvc + @Transactional
    │   │   └── TestSecurityConfig.java      in-memory RSA keys, overrides JWT beans
    │   ├── auth/
    │   │   ├── TokenServiceTest.java        unit
    │   │   └── AuthControllerTest.java      integration
    │   ├── user/UserControllerTest.java     integration
    │   ├── trainer/
    │   │   ├── TrainerServiceTest.java      unit
    │   │   └── TrainerControllerTest.java   integration
    │   ├── venue/VenueControllerTest.java   integration
    │   └── booking/
    │       ├── BookingServiceTest.java      unit
    │       └── BookingControllerTest.java   integration
    └── resources/
        └── application-test.yml
```

---

## Task 1: Bootstrap Project

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/findmycoach/FindMyCoachApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
    <relativePath/>
  </parent>

  <groupId>com.findmycoach</groupId>
  <artifactId>find-my-coach-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>find-my-coach-backend</name>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>1.20.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.12</version>
        <executions>
          <execution>
            <goals><goal>prepare-agent</goal></goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
          </execution>
          <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
              <rules>
                <rule>
                  <element>PACKAGE</element>
                  <includes>
                    <include>com.findmycoach.*.service</include>
                    <include>com.findmycoach.*.controller</include>
                  </includes>
                  <limits>
                    <limit>
                      <counter>LINE</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.80</minimum>
                    </limit>
                  </limits>
                </rule>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Create `FindMyCoachApplication.java`**

```java
package com.findmycoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FindMyCoachApplication {
    public static void main(String[] args) {
        SpringApplication.run(FindMyCoachApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `src/main/resources/application.yml`**

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/findmycoach}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    open-in-view: false
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email
  threads:
    virtual:
      enabled: true

app:
  frontend-url: ${APP_FRONTEND_URL:http://localhost:5173}
  jwt:
    private-key: ${APP_JWT_PRIVATE_KEY:}
    public-key: ${APP_JWT_PUBLIC_KEY:}
    access-token-ttl: ${APP_JWT_ACCESS_TOKEN_TTL:900}
    refresh-token-ttl: ${APP_JWT_REFRESH_TOKEN_TTL:2592000}

server:
  port: ${PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 4: Create `src/test/resources/application-test.yml`**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-client-id
            client-secret: test-client-secret
            scope: openid, profile, email

app:
  frontend-url: http://localhost:5173
  jwt:
    private-key: ""
    public-key: ""
    access-token-ttl: 900
    refresh-token-ttl: 2592000
```

- [ ] **Step 5: Verify compile**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "feat: bootstrap Spring Boot 3.3 project"
```

---

## Task 2: Exception Infrastructure

**Files:**
- Create: `backend/src/main/java/com/findmycoach/common/exception/AppException.java`
- Create: `backend/src/main/java/com/findmycoach/common/exception/ErrorResponse.java`
- Create: `backend/src/main/java/com/findmycoach/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/findmycoach/common/exception/GlobalExceptionHandlerTest.java`:

```java
package com.findmycoach.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAppException_returnsCorrectStatusAndBody() {
        AppException ex = new AppException("Slot already booked", HttpStatus.CONFLICT);
        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Slot already booked");
    }
}
```

- [ ] **Step 2: Run to confirm it fails**

```bash
cd backend && mvn test -pl . -Dtest=GlobalExceptionHandlerTest -q 2>&1 | tail -5
```

Expected: COMPILATION ERROR (classes don't exist yet)

- [ ] **Step 3: Create `AppException.java`**

```java
package com.findmycoach.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static AppException notFound(String message) {
        return new AppException(message, HttpStatus.NOT_FOUND);
    }

    public static AppException conflict(String message) {
        return new AppException(message, HttpStatus.CONFLICT);
    }

    public static AppException forbidden(String message) {
        return new AppException(message, HttpStatus.FORBIDDEN);
    }

    public static AppException badRequest(String message) {
        return new AppException(message, HttpStatus.BAD_REQUEST);
    }

    public static AppException unprocessable(String message) {
        return new AppException(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static AppException unauthorized(String message) {
        return new AppException(message, HttpStatus.UNAUTHORIZED);
    }
}
```

- [ ] **Step 4: Create `ErrorResponse.java`**

```java
package com.findmycoach.common.exception;

import java.util.Map;

public record ErrorResponse(String error, Map<String, String> details) {
    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }
}
```

- [ ] **Step 5: Create `GlobalExceptionHandler.java`**

```java
package com.findmycoach.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                                     (a, b) -> a));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Validation failed", details));
    }
}
```

- [ ] **Step 6: Run test to confirm it passes**

```bash
cd backend && mvn test -Dtest=GlobalExceptionHandlerTest -q
```

Expected: BUILD SUCCESS, Tests run: 1, Failures: 0

- [ ] **Step 7: Commit**

```bash
git add backend/src/
git commit -m "feat: add exception infrastructure"
```

---

## Task 3: Entities & Repositories

**Files:**
- Create: `user/User.java`, `user/UserRepository.java`
- Create: `auth/RefreshToken.java`, `auth/RefreshTokenRepository.java`
- Create: `trainer/Trainer.java`, `trainer/TrainerRepository.java`
- Create: `venue/Venue.java`, `venue/VenueRepository.java`
- Create: `booking/Booking.java`, `booking/BookingRepository.java`

- [ ] **Step 1: Create `user/User.java`**

```java
package com.findmycoach.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String city;
    private String avatar;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 2: Create `user/UserRepository.java`**

```java
package com.findmycoach.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
}
```

- [ ] **Step 3: Create `auth/RefreshToken.java`**

```java
package com.findmycoach.auth;

import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;
}
```

- [ ] **Step 4: Create `auth/RefreshTokenRepository.java`**

```java
package com.findmycoach.auth;

import com.findmycoach.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(User user);
}
```

- [ ] **Step 5: Create `trainer/Trainer.java`**

```java
package com.findmycoach.trainer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "trainers")
@Getter @Setter @NoArgsConstructor
public class Trainer {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private BigDecimal rating;

    @Column(nullable = false)
    private int reviews;

    @Column(nullable = false)
    private int hourlyRate;

    @Column(nullable = false)
    private int experience;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String specialties;
}
```

- [ ] **Step 6: Create `trainer/TrainerRepository.java`**

```java
package com.findmycoach.trainer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainerRepository extends JpaRepository<Trainer, String> {

    @Query("""
        SELECT t FROM Trainer t
        WHERE (:sport IS NULL OR t.sport = :sport)
        AND (:city IS NULL OR LOWER(t.city) = LOWER(:city))
        AND (:q IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(t.city) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Trainer> search(@Param("sport") String sport,
                         @Param("city") String city,
                         @Param("q") String q,
                         Pageable pageable);
}
```

- [ ] **Step 7: Create `venue/Venue.java`**

```java
package com.findmycoach.venue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "venues")
@Getter @Setter @NoArgsConstructor
public class Venue {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private int pricePerHour;

    @Column(nullable = false)
    private BigDecimal rating;

    private String imageUrl;
}
```

- [ ] **Step 8: Create `venue/VenueRepository.java`**

```java
package com.findmycoach.venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, String> {

    @Query("""
        SELECT v FROM Venue v
        WHERE (:sport IS NULL OR v.sport = :sport)
        AND (:city IS NULL OR LOWER(v.city) = LOWER(:city))
        """)
    Page<Venue> search(@Param("sport") String sport,
                       @Param("city") String city,
                       Pageable pageable);
}
```

- [ ] **Step 9: Create `booking/Booking.java`**

```java
package com.findmycoach.booking;

import com.findmycoach.trainer.Trainer;
import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    @Column(nullable = false)
    private String trainerName;

    private String trainerImageUrl;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private String slotTime;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String status = "CONFIRMED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 10: Create `booking/BookingRepository.java`**

```java
package com.findmycoach.booking;

import com.findmycoach.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByBookingDateAscSlotTimeAsc(User user);

    List<Booking> findByUserAndBookingDateGreaterThanEqualOrderByBookingDateAscSlotTimeAsc(
        User user, LocalDate from);

    List<Booking> findByUserAndBookingDateBeforeOrderByBookingDateAscSlotTimeAsc(
        User user, LocalDate before);

    boolean existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
        String trainerId, LocalDate date, String slotTime, String status);

    List<Booking> findByTrainerIdAndBookingDateBetweenAndStatus(
        String trainerId, LocalDate from, LocalDate to, String status);
}
```

- [ ] **Step 11: Verify compile**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add backend/src/
git commit -m "feat: add domain entities and repositories"
```

---

## Task 4: JWT Config & Token Service

**Files:**
- Create: `common/security/JwtConfig.java`
- Create: `auth/TokenService.java`
- Create: `support/TestSecurityConfig.java`
- Create: `auth/TokenServiceTest.java`

- [ ] **Step 1: Write the failing unit test**

Create `src/test/java/com/findmycoach/auth/TokenServiceTest.java`:

```java
package com.findmycoach.auth;

import com.findmycoach.common.exception.AppException;
import com.findmycoach.support.TestSecurityConfig;
import com.findmycoach.user.User;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        RSAKey rsaKey = TestSecurityConfig.TEST_RSA_KEY;
        var jwtEncoder = TestSecurityConfig.buildEncoder(rsaKey);
        var jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenService = new TokenService(jwtEncoder, jwtDecoder, refreshTokenRepository, 900L, 2592000L);
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        User user = testUser(true);
        String token = tokenService.generateAccessToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_savesToken() {
        User user = testUser(true);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String token = tokenService.generateRefreshToken(user);
        assertThat(token).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void validateRefreshToken_revokedToken_throwsUnauthorized() {
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(true);
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setUser(testUser(true));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(java.util.Optional.of(rt));

        assertThatThrownBy(() -> tokenService.validateRefreshToken("tok"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("revoked");
    }

    @Test
    void validateRefreshToken_expiredToken_throwsUnauthorized() {
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        rt.setExpiresAt(Instant.now().minusSeconds(1));
        rt.setUser(testUser(true));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(java.util.Optional.of(rt));

        assertThatThrownBy(() -> tokenService.validateRefreshToken("tok"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("expired");
    }

    private User testUser(boolean withCity) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setGoogleId("g123");
        u.setEmail("test@example.com");
        u.setName("Test User");
        if (withCity) u.setCity("London");
        return u;
    }
}
```

- [ ] **Step 2: Run to confirm it fails**

```bash
cd backend && mvn test -Dtest=TokenServiceTest -q 2>&1 | tail -5
```

Expected: COMPILATION ERROR

- [ ] **Step 3: Create `TestSecurityConfig.java`**

Create `src/test/java/com/findmycoach/support/TestSecurityConfig.java`:

```java
package com.findmycoach.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jwa.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPublicKey;

public class TestSecurityConfig {

    public static final RSAKey TEST_RSA_KEY;

    static {
        try {
            TEST_RSA_KEY = new RSAKeyGenerator(2048).generate();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate test RSA key", e);
        }
    }

    public static JwtEncoder buildEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        public JwtEncoder jwtEncoder() {
            return buildEncoder(TEST_RSA_KEY);
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() throws JOSEException {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TEST_RSA_KEY.toPublicKey()).build();
        }
    }
}
```

- [ ] **Step 4: Create `common/security/JwtConfig.java`**

```java
package com.findmycoach.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter @Setter
public class JwtConfig {

    private String privateKey = "";
    private String publicKey = "";
    private long accessTokenTtl = 900;
    private long refreshTokenTtl = 2592000;

    @Bean
    @Profile("!test")
    public JwtEncoder jwtEncoder() throws Exception {
        RSAKey rsaKey = new RSAKey.Builder(getRsaPublicKey())
            .privateKey(getRsaPrivateKey())
            .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    @Profile("!test")
    public JwtDecoder jwtDecoder() throws Exception {
        return NimbusJwtDecoder.withPublicKey(getRsaPublicKey()).build();
    }

    public RSAPrivateKey getRsaPrivateKey() throws Exception {
        String stripped = privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public RSAPublicKey getRsaPublicKey() throws Exception {
        String stripped = publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
    }
}
```

- [ ] **Step 5: Create `auth/TokenService.java`**

```java
package com.findmycoach.auth;

import com.findmycoach.common.exception.AppException;
import com.findmycoach.common.security.JwtConfig;
import com.findmycoach.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long accessTokenTtl;
    private final long refreshTokenTtl;

    public TokenService(JwtEncoder jwtEncoder,
                        JwtDecoder jwtDecoder,
                        RefreshTokenRepository refreshTokenRepository,
                        @Value("${app.jwt.access-token-ttl:900}") long accessTokenTtl,
                        @Value("${app.jwt.refresh-token-ttl:2592000}") long refreshTokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("profileComplete", user.getCity() != null)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(accessTokenTtl))
            .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Transactional
    public String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(token);
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTokenTtl));
        refreshTokenRepository.save(rt);
        return token;
    }

    @Transactional
    public User validateRefreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));
        if (rt.isRevoked()) {
            throw AppException.unauthorized("Refresh token revoked");
        }
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw AppException.unauthorized("Refresh token expired");
        }
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return rt.getUser();
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
```

- [ ] **Step 6: Run test to confirm it passes**

```bash
cd backend && mvn test -Dtest=TokenServiceTest -q
```

Expected: Tests run: 4, Failures: 0

- [ ] **Step 7: Commit**

```bash
git add backend/src/
git commit -m "feat: add JWT config and TokenService"
```

---

## Task 5: Security Config & OAuth2 Components

**Files:**
- Create: `common/security/SecurityConfig.java`
- Create: `common/security/CustomOidcUserService.java`
- Create: `common/security/OAuth2SuccessHandler.java`
- Create: `common/security/ProfileCompleteFilter.java`

- [ ] **Step 1: Create `ProfileCompleteFilter.java`**

```java
package com.findmycoach.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmycoach.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfileCompleteFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PROFILE_REQUIRED = List.of(
        "/api/v1/me", "/api/v1/bookings", "/api/v1/bookings/**"
    );
    private static final List<String> EXEMPT = List.of(
        "/api/v1/auth/complete-profile", "/api/v1/auth/logout",
        "/api/v1/auth/refresh"
    );

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean requiresProfile = PROFILE_REQUIRED.stream()
            .anyMatch(p -> MATCHER.match(p, path));
        boolean exempt = EXEMPT.stream().anyMatch(p -> MATCHER.match(p, path));

        if (requiresProfile && !exempt) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Boolean profileComplete = jwtAuth.getToken().getClaim("profileComplete");
                if (!Boolean.TRUE.equals(profileComplete)) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getWriter(),
                        ErrorResponse.of("PROFILE_INCOMPLETE"));
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Create `CustomOidcUserService.java`**

```java
package com.findmycoach.common.security;

import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : email;
        String picture = oidcUser.getPicture();

        userRepository.findByGoogleId(googleId).ifPresentOrElse(user -> {
            boolean changed = !Objects.equals(user.getName(), name)
                || !Objects.equals(user.getAvatar(), picture);
            if (changed) {
                user.setName(name);
                user.setAvatar(picture);
                userRepository.save(user);
            }
        }, () -> {
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setAvatar(picture);
            userRepository.save(newUser);
        });

        return oidcUser;
    }
}
```

- [ ] **Step 3: Create `OAuth2SuccessHandler.java`**

```java
package com.findmycoach.common.security;

import com.findmycoach.auth.TokenService;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        User user = userRepository.findByGoogleId(oidcUser.getSubject()).orElseThrow();

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        String redirectUrl = UriComponentsBuilder
            .fromUriString(frontendUrl + "/auth/callback")
            .queryParam("token", accessToken)
            .queryParam("refresh", refreshToken)
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

- [ ] **Step 4: Create `SecurityConfig.java`**

```java
package com.findmycoach.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtDecoder jwtDecoder;
    private final ProfileCompleteFilter profileCompleteFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2LoginChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/auth/google", "/login/oauth2/**", "/oauth2/**")
            .csrf(AbstractHttpConfigurer::disable)
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ae -> ae.baseUri("/api/v1/auth"))
                .userInfoEndpoint(info -> info.oidcUserService(customOidcUserService))
                .successHandler(oauth2SuccessHandler)
            );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/**", "/actuator/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/trainers/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/venues/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.decoder(jwtDecoder)))
            .addFilterAfter(profileCompleteFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 5: Verify compile**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add Spring Security config and OAuth2 components"
```

---

## Task 6: Auth Endpoints

**Files:**
- Create: `auth/dto/AuthResponse.java`, `auth/dto/RefreshRequest.java`, `auth/dto/CompleteProfileRequest.java`
- Create: `auth/AuthService.java`
- Create: `auth/AuthController.java`
- Create: `support/BaseIntegrationTest.java`
- Create: `auth/AuthControllerTest.java`

- [ ] **Step 1: Create DTOs**

`auth/dto/AuthResponse.java`:
```java
package com.findmycoach.auth.dto;

import com.findmycoach.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}
```

`auth/dto/RefreshRequest.java`:
```java
package com.findmycoach.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

`auth/dto/CompleteProfileRequest.java`:
```java
package com.findmycoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteProfileRequest(
    @NotBlank @Size(min = 1, max = 100) String city
) {}
```

- [ ] **Step 2: Create `user/dto/UserResponse.java`** (needed by AuthResponse)

```java
package com.findmycoach.user.dto;

import com.findmycoach.user.User;

public record UserResponse(String id, String email, String name, String city, String avatar) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId().toString(),
            user.getEmail(),
            user.getName(),
            user.getCity(),
            user.getAvatar()
        );
    }
}
```

- [ ] **Step 3: Create `auth/AuthService.java`**

```java
package com.findmycoach.auth;

import com.findmycoach.auth.dto.AuthResponse;
import com.findmycoach.auth.dto.CompleteProfileRequest;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import com.findmycoach.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse refresh(String oldRefreshToken) {
        User user = tokenService.validateRefreshToken(oldRefreshToken);
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, null);
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    @Transactional
    public AuthResponse completeProfile(String userId, CompleteProfileRequest request) {
        User user = userRepository.findById(java.util.UUID.fromString(userId)).orElseThrow();
        user.setCity(request.city());
        userRepository.save(user);
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}
```

- [ ] **Step 4: Create `auth/AuthController.java`**

```java
package com.findmycoach.auth;

import com.findmycoach.auth.dto.AuthResponse;
import com.findmycoach.auth.dto.CompleteProfileRequest;
import com.findmycoach.auth.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complete-profile")
    public AuthResponse completeProfile(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody CompleteProfileRequest request) {
        return authService.completeProfile(jwt.getSubject(), request);
    }
}
```

- [ ] **Step 5: Create `BaseIntegrationTest.java`**

Create `src/test/java/com/findmycoach/support/BaseIntegrationTest.java`:

```java
package com.findmycoach.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmycoach.auth.TokenService;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Testcontainers
@Import(TestSecurityConfig.Config.class)
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("findmycoach_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected TokenService tokenService;

    protected User createUser(String googleId, String email, String city) {
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName("Test User");
        user.setCity(city);
        return userRepository.save(user);
    }

    protected String bearerToken(User user) {
        return "Bearer " + tokenService.generateAccessToken(user);
    }

    protected String incompleteToken(User user) {
        user.setCity(null);
        return "Bearer " + tokenService.generateAccessToken(user);
    }
}
```

- [ ] **Step 6: Write failing integration tests**

Create `src/test/java/com/findmycoach/auth/AuthControllerTest.java`:

```java
package com.findmycoach.auth;

import com.findmycoach.auth.dto.CompleteProfileRequest;
import com.findmycoach.auth.dto.RefreshRequest;
import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Test
    void refresh_validToken_returnsNewTokenPair() throws Exception {
        User user = createUser("g1", "a@test.com", "London");
        String refreshToken = tokenService.generateRefreshToken(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_revokedToken_returns401() throws Exception {
        User user = createUser("g2", "b@test.com", "London");
        String refreshToken = tokenService.generateRefreshToken(user);
        tokenService.revokeRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void completeProfile_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/complete-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteProfileRequest("London"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void completeProfile_blankCity_returns400() throws Exception {
        User user = createUser("g3", "c@test.com", null);
        mockMvc.perform(post("/api/v1/auth/complete-profile")
                .header("Authorization", incompleteToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteProfileRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.city").exists());
    }

    @Test
    void completeProfile_valid_returnsTokensAndUser() throws Exception {
        User user = createUser("g4", "d@test.com", null);
        mockMvc.perform(post("/api/v1/auth/complete-profile")
                .header("Authorization", incompleteToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteProfileRequest("London"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.city").value("London"));
    }
}
```

- [ ] **Step 7: Run tests**

```bash
cd backend && mvn test -Dtest=AuthControllerTest -q
```

Expected: Tests run: 4, Failures: 0

- [ ] **Step 8: Commit**

```bash
git add backend/src/
git commit -m "feat: add auth endpoints (refresh, logout, complete-profile)"
```

---

## Task 7: User Endpoints

**Files:**
- Create: `user/dto/UpdateProfileRequest.java`
- Create: `user/UserService.java`
- Create: `user/UserController.java`
- Create: `user/UserControllerTest.java`

- [ ] **Step 1: Write failing test**

Create `src/test/java/com/findmycoach/user/UserControllerTest.java`:

```java
package com.findmycoach.user;

import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseIntegrationTest {

    @Test
    void getMe_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_incompleteProfile_returns403() throws Exception {
        User user = createUser("g1", "a@test.com", null);
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", incompleteToken(user)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getMe_validJwt_returnsUser() throws Exception {
        User user = createUser("g2", "b@test.com", "London");
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("b@test.com"))
            .andExpect(jsonPath("$.city").value("London"));
    }

    @Test
    void patchMe_updatesNameAndCity() throws Exception {
        User user = createUser("g3", "c@test.com", "London");
        mockMvc.perform(patch("/api/v1/me")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateProfileRequest("New Name", "Manchester", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.city").value("Manchester"));
    }
}
```

- [ ] **Step 2: Run to confirm it fails**

```bash
cd backend && mvn test -Dtest=UserControllerTest -q 2>&1 | tail -5
```

Expected: COMPILATION ERROR

- [ ] **Step 3: Create `user/dto/UpdateProfileRequest.java`**

```java
package com.findmycoach.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 1, max = 100) String name,
    @Size(min = 1, max = 100) String city,
    @Size(max = 500) String avatar
) {}
```

- [ ] **Step 4: Create `user/UserService.java`**

```java
package com.findmycoach.user;

import com.findmycoach.common.exception.AppException;
import com.findmycoach.user.dto.UpdateProfileRequest;
import com.findmycoach.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUser(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> AppException.notFound("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> AppException.notFound("User not found"));
        if (request.name() != null) user.setName(request.name());
        if (request.city() != null) user.setCity(request.city());
        if (request.avatar() != null) user.setAvatar(request.avatar());
        return UserResponse.from(userRepository.save(user));
    }
}
```

- [ ] **Step 5: Create `user/UserController.java`**

```java
package com.findmycoach.user;

import com.findmycoach.user.dto.UpdateProfileRequest;
import com.findmycoach.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(jwt.getSubject());
    }

    @PatchMapping
    public UserResponse patchMe(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateUser(jwt.getSubject(), request);
    }
}
```

- [ ] **Step 6: Run tests**

```bash
cd backend && mvn test -Dtest=UserControllerTest -q
```

Expected: Tests run: 4, Failures: 0

- [ ] **Step 7: Commit**

```bash
git add backend/src/
git commit -m "feat: add user profile endpoints (GET/PATCH /me)"
```

---

## Task 8: Trainer Endpoints

**Files:**
- Create: `trainer/dto/TrainerResponse.java`, `trainer/dto/DayAvailability.java`
- Create: `trainer/TrainerService.java`
- Create: `trainer/TrainerController.java`
- Create: `trainer/TrainerServiceTest.java`
- Create: `trainer/TrainerControllerTest.java`

- [ ] **Step 1: Create DTOs**

`trainer/dto/TrainerResponse.java`:
```java
package com.findmycoach.trainer.dto;

import com.findmycoach.trainer.Trainer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public record TrainerResponse(
    String id, String name, String sport, String city,
    double rating, int reviews, double hourlyRate,
    int experience, String bio, String imageUrl,
    List<String> specialties
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TrainerResponse from(Trainer t) {
        List<String> specs;
        try {
            specs = MAPPER.readValue(t.getSpecialties(), new TypeReference<>() {});
        } catch (Exception e) {
            specs = List.of();
        }
        return new TrainerResponse(
            t.getId(), t.getName(), t.getSport(), t.getCity(),
            t.getRating().doubleValue(), t.getReviews(),
            t.getHourlyRate() / 100.0,
            t.getExperience(), t.getBio(), t.getImageUrl(), specs
        );
    }
}
```

`trainer/dto/DayAvailability.java`:
```java
package com.findmycoach.trainer.dto;

import java.util.List;

public record DayAvailability(String date, List<String> slots) {}
```

- [ ] **Step 2: Write failing unit test**

Create `src/test/java/com/findmycoach/trainer/TrainerServiceTest.java`:

```java
package com.findmycoach.trainer;

import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.dto.DayAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TrainerServiceTest {

    private TrainerRepository trainerRepository;
    private BookingRepository bookingRepository;
    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        trainerRepository = mock(TrainerRepository.class);
        bookingRepository = mock(BookingRepository.class);
        trainerService = new TrainerService(trainerRepository, bookingRepository);
    }

    @Test
    void getAvailability_rangeExceeds14Days_throwsBadRequest() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(30);
        assertThatThrownBy(() -> trainerService.getAvailability("t1", from, to))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getAvailability_trainerNotFound_throwsNotFound() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(3);
        when(trainerRepository.findById("t1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainerService.getAvailability("t1", from, to))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAvailability_excludesBookedSlots() {
        LocalDate date = LocalDate.now();
        Trainer trainer = new Trainer();
        trainer.setId("t1");
        when(trainerRepository.findById("t1")).thenReturn(Optional.of(trainer));

        Booking booking = new Booking();
        booking.setBookingDate(date);
        booking.setSlotTime("08:00");
        when(bookingRepository.findByTrainerIdAndBookingDateBetweenAndStatus(
            "t1", date, date, "CONFIRMED")).thenReturn(List.of(booking));

        List<DayAvailability> result = trainerService.getAvailability("t1", date, date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slots()).doesNotContain("08:00");
        assertThat(result.get(0).slots()).contains("09:00");
    }
}
```

- [ ] **Step 3: Run to confirm it fails**

```bash
cd backend && mvn test -Dtest=TrainerServiceTest -q 2>&1 | tail -5
```

Expected: COMPILATION ERROR

- [ ] **Step 4: Create `trainer/TrainerService.java`**

```java
package com.findmycoach.trainer;

import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.dto.DayAvailability;
import com.findmycoach.trainer.dto.TrainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private static final List<String> ALL_SLOTS =
        List.of("08:00", "09:00", "10:00", "12:00", "14:00", "15:00", "17:00", "18:00", "19:00");

    private final TrainerRepository trainerRepository;
    private final BookingRepository bookingRepository;

    public Page<TrainerResponse> search(String sport, String city, String q, Pageable pageable) {
        return trainerRepository.search(
            (sport != null && !sport.isBlank()) ? sport : null,
            (city != null && !city.isBlank()) ? city : null,
            (q != null && !q.isBlank()) ? q : null,
            pageable
        ).map(TrainerResponse::from);
    }

    public TrainerResponse getById(String id) {
        return trainerRepository.findById(id)
            .map(TrainerResponse::from)
            .orElseThrow(() -> AppException.notFound("Trainer not found"));
    }

    public List<DayAvailability> getAvailability(String trainerId, LocalDate from, LocalDate to) {
        if (ChronoUnit.DAYS.between(from, to) > 14) {
            throw AppException.badRequest("Date range cannot exceed 14 days");
        }
        trainerRepository.findById(trainerId)
            .orElseThrow(() -> AppException.notFound("Trainer not found"));

        List<Booking> booked = bookingRepository
            .findByTrainerIdAndBookingDateBetweenAndStatus(trainerId, from, to, "CONFIRMED");

        Set<String> bookedKeys = booked.stream()
            .map(b -> b.getBookingDate() + ":" + b.getSlotTime())
            .collect(Collectors.toSet());

        List<DayAvailability> result = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            final LocalDate day = current;
            List<String> available = ALL_SLOTS.stream()
                .filter(slot -> !bookedKeys.contains(day + ":" + slot))
                .toList();
            result.add(new DayAvailability(day.toString(), available));
            current = current.plusDays(1);
        }
        return result;
    }
}
```

- [ ] **Step 5: Create `trainer/TrainerController.java`**

```java
package com.findmycoach.trainer;

import com.findmycoach.trainer.dto.DayAvailability;
import com.findmycoach.trainer.dto.TrainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trainers")
@RequiredArgsConstructor
public class TrainerController {

    private final TrainerService trainerService;

    @GetMapping
    public Page<TrainerResponse> list(
        @RequestParam(required = false) String sport,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return trainerService.search(sport, city, q, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public TrainerResponse getById(@PathVariable String id) {
        return trainerService.getById(id);
    }

    @GetMapping("/{id}/availability")
    public List<DayAvailability> availability(
        @PathVariable String id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return trainerService.getAvailability(id, from, to);
    }
}
```

- [ ] **Step 6: Write integration test**

Create `src/test/java/com/findmycoach/trainer/TrainerControllerTest.java`:

```java
package com.findmycoach.trainer;

import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrainerControllerTest extends BaseIntegrationTest {

    @Autowired
    private TrainerRepository trainerRepository;

    private Trainer seedTrainer(String id, String sport, String city) {
        Trainer t = new Trainer();
        t.setId(id);
        t.setName("Coach " + id);
        t.setSport(sport);
        t.setCity(city);
        t.setRating(java.math.BigDecimal.valueOf(4.5));
        t.setReviews(10);
        t.setHourlyRate(5000);
        t.setExperience(5);
        t.setSpecialties("[]");
        return trainerRepository.save(t);
    }

    @Test
    void listTrainers_filterBySport_returnsOnlyMatchingSport() throws Exception {
        seedTrainer("t1", "Tennis", "London");
        seedTrainer("t2", "Football", "London");

        mockMvc.perform(get("/api/v1/trainers").param("sport", "Tennis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].sport").value(org.hamcrest.Matchers.everyItem(
                org.hamcrest.Matchers.is("Tennis"))));
    }

    @Test
    void getTrainer_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/trainers/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void availability_rangeOver14Days_returns400() throws Exception {
        seedTrainer("t3", "Tennis", "London");
        String from = LocalDate.now().toString();
        String to = LocalDate.now().plusDays(30).toString();

        mockMvc.perform(get("/api/v1/trainers/t3/availability")
                .param("from", from).param("to", to))
            .andExpect(status().isBadRequest());
    }

    @Test
    void availability_returns14DaysOfSlots() throws Exception {
        seedTrainer("t4", "Tennis", "London");
        String from = LocalDate.now().toString();
        String to = LocalDate.now().plusDays(5).toString();

        mockMvc.perform(get("/api/v1/trainers/t4/availability")
                .param("from", from).param("to", to))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(6));
    }
}
```

- [ ] **Step 7: Run all trainer tests**

```bash
cd backend && mvn test -Dtest="TrainerServiceTest,TrainerControllerTest" -q
```

Expected: Tests run: 7, Failures: 0

- [ ] **Step 8: Commit**

```bash
git add backend/src/
git commit -m "feat: add trainer endpoints with availability logic"
```

---

## Task 9: Venue Endpoints

**Files:**
- Create: `venue/dto/VenueResponse.java`
- Create: `venue/VenueService.java`
- Create: `venue/VenueController.java`
- Create: `venue/VenueControllerTest.java`

- [ ] **Step 1: Create DTO, Service, Controller**

`venue/dto/VenueResponse.java`:
```java
package com.findmycoach.venue.dto;

import com.findmycoach.venue.Venue;

public record VenueResponse(
    String id, String name, String type, String sport,
    String city, double pricePerHour, double rating, String imageUrl
) {
    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getType(), v.getSport(),
            v.getCity(), v.getPricePerHour() / 100.0, v.getRating().doubleValue(), v.getImageUrl());
    }
}
```

`venue/VenueService.java`:
```java
package com.findmycoach.venue;

import com.findmycoach.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    public Page<VenueResponse> search(String sport, String city, Pageable pageable) {
        return venueRepository.search(
            (sport != null && !sport.isBlank()) ? sport : null,
            (city != null && !city.isBlank()) ? city : null,
            pageable
        ).map(VenueResponse::from);
    }
}
```

`venue/VenueController.java`:
```java
package com.findmycoach.venue;

import com.findmycoach.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public Page<VenueResponse> list(
        @RequestParam(required = false) String sport,
        @RequestParam(required = false) String city,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return venueService.search(sport, city, PageRequest.of(page, Math.min(size, 100)));
    }
}
```

- [ ] **Step 2: Write and run integration test**

Create `src/test/java/com/findmycoach/venue/VenueControllerTest.java`:

```java
package com.findmycoach.venue;

import com.findmycoach.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VenueControllerTest extends BaseIntegrationTest {

    @Autowired
    private VenueRepository venueRepository;

    private void seedVenue(String id, String sport, String city) {
        Venue v = new Venue();
        v.setId(id); v.setName("Venue " + id);
        v.setType("Court"); v.setSport(sport); v.setCity(city);
        v.setPricePerHour(3000); v.setRating(BigDecimal.valueOf(4.5));
        venueRepository.save(v);
    }

    @Test
    void listVenues_filterBySport_returnsOnlyMatchingSport() throws Exception {
        seedVenue("v1", "Tennis", "London");
        seedVenue("v2", "Football", "Manchester");

        mockMvc.perform(get("/api/v1/venues").param("sport", "Tennis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].sport")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Tennis"))));
    }

    @Test
    void listVenues_noFilter_returnsAll() throws Exception {
        seedVenue("v3", "Cricket", "Birmingham");
        seedVenue("v4", "Basketball", "London");

        mockMvc.perform(get("/api/v1/venues"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
```

```bash
cd backend && mvn test -Dtest=VenueControllerTest -q
```

Expected: Tests run: 2, Failures: 0

- [ ] **Step 3: Commit**

```bash
git add backend/src/
git commit -m "feat: add venue list endpoint"
```

---

## Task 10: Booking Endpoints

**Files:**
- Create: `booking/dto/CreateBookingRequest.java`, `booking/dto/BookingResponse.java`
- Create: `booking/BookingService.java`
- Create: `booking/BookingController.java`
- Create: `booking/BookingServiceTest.java`
- Create: `booking/BookingControllerTest.java`

- [ ] **Step 1: Create DTOs**

`booking/dto/CreateBookingRequest.java`:
```java
package com.findmycoach.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateBookingRequest(
    @NotBlank String trainerId,
    @NotNull LocalDate date,
    @NotBlank @Pattern(regexp = "^(08|09|10|12|14|15|17|18|19):00$",
                       message = "must be a valid slot time") String slot
) {}
```

`booking/dto/BookingResponse.java`:
```java
package com.findmycoach.booking.dto;

import com.findmycoach.booking.Booking;

public record BookingResponse(
    String id, String trainerId, String trainerName, String trainerImageUrl,
    String sport, String date, String slot, double price, String status, String createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
            b.getId().toString(),
            b.getTrainer().getId(),
            b.getTrainerName(),
            b.getTrainerImageUrl(),
            b.getSport(),
            b.getBookingDate().toString(),
            b.getSlotTime(),
            b.getPrice() / 100.0,
            b.getStatus(),
            b.getCreatedAt().toString()
        );
    }
}
```

- [ ] **Step 2: Write failing unit test**

Create `src/test/java/com/findmycoach/booking/BookingServiceTest.java`:

```java
package com.findmycoach.booking;

import com.findmycoach.booking.dto.CreateBookingRequest;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private TrainerRepository trainerRepository;
    private UserRepository userRepository;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        trainerRepository = mock(TrainerRepository.class);
        userRepository = mock(UserRepository.class);
        bookingService = new BookingService(bookingRepository, trainerRepository, userRepository);
    }

    @Test
    void createBooking_slotAlreadyTaken_throwsConflict() {
        String userId = UUID.randomUUID().toString();
        User user = new User(); user.setId(UUID.fromString(userId));
        Trainer trainer = trainer("t1", 6500);
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(trainerRepository.findById("t1")).thenReturn(Optional.of(trainer));
        when(bookingRepository.existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
            "t1", LocalDate.now(), "08:00", "CONFIRMED")).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(userId,
            new CreateBookingRequest("t1", LocalDate.now(), "08:00")))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createBooking_snapshotsHourlyRateAsPrice() {
        String userId = UUID.randomUUID().toString();
        User user = new User(); user.setId(UUID.fromString(userId));
        Trainer trainer = trainer("t1", 6500);
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(trainerRepository.findById("t1")).thenReturn(Optional.of(trainer));
        when(bookingRepository.existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
            any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.create(userId, new CreateBookingRequest("t1", LocalDate.now(), "09:00"));

        verify(bookingRepository).save(argThat(b -> b.getPrice() == 6500));
    }

    @Test
    void cancelBooking_pastBooking_throwsUnprocessable() {
        Booking booking = new Booking();
        booking.setBookingDate(LocalDate.now().minusDays(1));
        String ownerId = UUID.randomUUID().toString();
        User owner = new User(); owner.setId(UUID.fromString(ownerId));
        booking.setUser(owner);
        booking.setStatus("CONFIRMED");
        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(UUID.randomUUID().toString(), ownerId))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cancelBooking_notOwner_throwsForbidden() {
        Booking booking = new Booking();
        booking.setBookingDate(LocalDate.now().plusDays(1));
        User owner = new User(); owner.setId(UUID.randomUUID());
        booking.setUser(owner);
        booking.setStatus("CONFIRMED");
        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
            bookingService.cancel(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Trainer trainer(String id, int hourlyRate) {
        Trainer t = new Trainer();
        t.setId(id); t.setName("Coach"); t.setSport("Tennis");
        t.setCity("London"); t.setRating(BigDecimal.valueOf(4.5));
        t.setHourlyRate(hourlyRate); t.setSpecialties("[]");
        return t;
    }
}
```

- [ ] **Step 3: Run to confirm it fails**

```bash
cd backend && mvn test -Dtest=BookingServiceTest -q 2>&1 | tail -5
```

Expected: COMPILATION ERROR

- [ ] **Step 4: Create `booking/BookingService.java`**

```java
package com.findmycoach.booking;

import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.booking.dto.CreateBookingRequest;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse create(String userId, CreateBookingRequest request) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        Trainer trainer = trainerRepository.findById(request.trainerId())
            .orElseThrow(() -> AppException.notFound("Trainer not found"));

        boolean exists = bookingRepository.existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
            request.trainerId(), request.date(), request.slot(), "CONFIRMED");
        if (exists) throw AppException.conflict("Slot already booked");

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrainer(trainer);
        booking.setTrainerName(trainer.getName());
        booking.setTrainerImageUrl(trainer.getImageUrl());
        booking.setSport(trainer.getSport());
        booking.setBookingDate(request.date());
        booking.setSlotTime(request.slot());
        booking.setPrice(trainer.getHourlyRate());

        try {
            return BookingResponse.from(bookingRepository.save(booking));
        } catch (DataIntegrityViolationException e) {
            throw AppException.conflict("Slot already booked");
        }
    }

    public List<BookingResponse> list(String userId, String status) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        LocalDate today = LocalDate.now();
        return switch (status) {
            case "upcoming" -> bookingRepository
                .findByUserAndBookingDateGreaterThanEqualOrderByBookingDateAscSlotTimeAsc(user, today)
                .stream().map(BookingResponse::from).toList();
            case "past" -> bookingRepository
                .findByUserAndBookingDateBeforeOrderByBookingDateAscSlotTimeAsc(user, today)
                .stream().map(BookingResponse::from).toList();
            default -> bookingRepository
                .findByUserOrderByBookingDateAscSlotTimeAsc(user)
                .stream().map(BookingResponse::from).toList();
        };
    }

    @Transactional
    public void cancel(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(UUID.fromString(bookingId))
            .orElseThrow(() -> AppException.notFound("Booking not found"));

        if (!booking.getUser().getId().toString().equals(userId)) {
            throw AppException.forbidden("Not your booking");
        }
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw AppException.unprocessable("Cannot cancel a past booking");
        }
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}
```

- [ ] **Step 5: Create `booking/BookingController.java`**

```java
package com.findmycoach.booking;

import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(jwt.getSubject(), request);
    }

    @GetMapping
    public List<BookingResponse> list(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(defaultValue = "all") String status) {
        return bookingService.list(jwt.getSubject(), status);
    }

    @DeleteMapping("/{id}")
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        bookingService.cancel(id, jwt.getSubject());
    }
}
```

- [ ] **Step 6: Write integration test**

Create `src/test/java/com/findmycoach/booking/BookingControllerTest.java`:

```java
package com.findmycoach.booking;

import com.findmycoach.booking.dto.CreateBookingRequest;
import com.findmycoach.support.BaseIntegrationTest;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerTest extends BaseIntegrationTest {

    @Autowired TrainerRepository trainerRepository;
    @Autowired BookingRepository bookingRepository;

    private Trainer seedTrainer() {
        Trainer t = new Trainer();
        t.setId("t-test"); t.setName("Coach Test"); t.setSport("Tennis");
        t.setCity("London"); t.setRating(BigDecimal.valueOf(4.5));
        t.setReviews(10); t.setHourlyRate(6500); t.setExperience(5); t.setSpecialties("[]");
        return trainerRepository.save(t);
    }

    @Test
    void createBooking_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(1), "08:00"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createBooking_incompleteProfile_returns403() throws Exception {
        seedTrainer();
        User user = createUser("g-inc", "inc@test.com", null);
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", incompleteToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(1), "08:00"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createBooking_valid_returns201WithSnapshot() throws Exception {
        seedTrainer();
        User user = createUser("g1", "u1@test.com", "London");
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(1), "09:00"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.price").value(65.0))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void createBooking_sameSlotTwice_returns409() throws Exception {
        seedTrainer();
        User user = createUser("g2", "u2@test.com", "London");
        var body = objectMapper.writeValueAsString(
            new CreateBookingRequest("t-test", LocalDate.now().plusDays(2), "10:00"));

        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        User user2 = createUser("g3", "u3@test.com", "London");
        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(user2))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void deleteBooking_notOwner_returns403() throws Exception {
        seedTrainer();
        User owner = createUser("g4", "owner@test.com", "London");
        User other = createUser("g5", "other@test.com", "London");

        var createBody = objectMapper.writeValueAsString(
            new CreateBookingRequest("t-test", LocalDate.now().plusDays(3), "12:00"));
        String responseBody = mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearerToken(owner))
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(responseBody).get("id").asText();
        mockMvc.perform(delete("/api/v1/bookings/" + bookingId)
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getBookings_statusUpcoming_returnsOnlyFutureBookings() throws Exception {
        seedTrainer();
        User user = createUser("g6", "u6@test.com", "London");
        String auth = bearerToken(user);

        mockMvc.perform(post("/api/v1/bookings")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateBookingRequest("t-test", LocalDate.now().plusDays(4), "14:00"))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bookings").param("status", "upcoming")
                .header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
```

- [ ] **Step 7: Run all booking tests**

```bash
cd backend && mvn test -Dtest="BookingServiceTest,BookingControllerTest" -q
```

Expected: Tests run: 9, Failures: 0

- [ ] **Step 8: Commit**

```bash
git add backend/src/
git commit -m "feat: add booking endpoints with conflict detection"
```

---

## Task 11: Data Seeder

**Files:**
- Create: `common/seed/DataSeeder.java`

- [ ] **Step 1: Create `DataSeeder.java`**

```java
package com.findmycoach.common.seed;

import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.venue.Venue;
import com.findmycoach.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final TrainerRepository trainerRepository;
    private final VenueRepository venueRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createPartialUniqueIndex();
        if (trainerRepository.count() == 0) {
            seedTrainers();
            seedVenues();
            log.info("Seeded trainers and venues");
        }
    }

    private void createPartialUniqueIndex() {
        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uq_trainer_slot_confirmed
            ON bookings (trainer_id, booking_date, slot_time)
            WHERE status = 'CONFIRMED'
            """);
    }

    private void seedTrainers() {
        trainerRepository.saveAll(List.of(
            trainer("marcus-reed", "Marcus Reed", "Tennis", "London",
                BigDecimal.valueOf(4.9), 184, 6500, 12,
                "Former ATP qualifier turned coach. I help juniors and adults sharpen technique, footwork and match strategy.",
                "[\"Serve & Volley\",\"Junior Development\",\"Match Play\"]"),
            trainer("sofia-alvarez", "Sofia Alvarez", "Football", "Manchester",
                BigDecimal.valueOf(4.8), 142, 5500, 9,
                "UEFA B licensed coach. Focused on positional play, finishing and game intelligence for ages 8 to adult.",
                "[\"Finishing\",\"1v1 Defending\",\"Position Play\"]"),
            trainer("arjun-mehta", "Arjun Mehta", "Cricket", "Birmingham",
                BigDecimal.valueOf(4.9), 98, 5000, 14,
                "Ex-county batsman. Specializing in batting fundamentals, spin play, and high-pressure scenario training.",
                "[\"Batting\",\"Spin Play\",\"Fielding Drills\"]"),
            trainer("leah-okafor", "Leah Okafor", "Basketball", "London",
                BigDecimal.valueOf(4.7), 121, 6000, 8,
                "Pro shooting coach. Build a deadly jumper, tighten your handle, and develop pro-level court IQ.",
                "[\"Shooting\",\"Ball Handling\",\"Conditioning\"]")
        ));
    }

    private void seedVenues() {
        venueRepository.saveAll(List.of(
            venue("v1", "Riverside Tennis Club", "Court", "Tennis", "London", 3000, BigDecimal.valueOf(4.7)),
            venue("v2", "Northgate Football Arena", "Pitch", "Football", "Manchester", 8000, BigDecimal.valueOf(4.8)),
            venue("v3", "Hardwood Hoops Center", "Court", "Basketball", "London", 4500, BigDecimal.valueOf(4.6)),
            venue("v4", "Royal Cricket Ground", "Stadium", "Cricket", "Birmingham", 12000, BigDecimal.valueOf(4.9))
        ));
    }

    private Trainer trainer(String id, String name, String sport, String city,
                            BigDecimal rating, int reviews, int hourlyRate,
                            int experience, String bio, String specialties) {
        Trainer t = new Trainer();
        t.setId(id); t.setName(name); t.setSport(sport); t.setCity(city);
        t.setRating(rating); t.setReviews(reviews); t.setHourlyRate(hourlyRate);
        t.setExperience(experience); t.setBio(bio); t.setSpecialties(specialties);
        return t;
    }

    private Venue venue(String id, String name, String type, String sport,
                        String city, int pricePerHour, BigDecimal rating) {
        Venue v = new Venue();
        v.setId(id); v.setName(name); v.setType(type); v.setSport(sport);
        v.setCity(city); v.setPricePerHour(pricePerHour); v.setRating(rating);
        return v;
    }
}
```

- [ ] **Step 2: Run full test suite to verify seeder doesn't break tests**

```bash
cd backend && mvn test -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/
git commit -m "feat: add data seeder and partial unique index for booking slots"
```

---

## Task 12: Final Verification

- [ ] **Step 1: Run full test suite with coverage**

```bash
cd backend && mvn verify -q
```

Expected: BUILD SUCCESS. JaCoCo report in `backend/target/site/jacoco/index.html`.

- [ ] **Step 2: Check coverage threshold**

If `mvn verify` fails with coverage below 80%, open `target/site/jacoco/index.html` and find which service/controller classes need more tests. Add them to the relevant test file and re-run.

- [ ] **Step 3: Verify app starts against a real PostgreSQL**

Ensure a local PostgreSQL instance is running and set environment variables, then:

```bash
cd backend && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/findmycoach \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=postgres \
  GOOGLE_CLIENT_ID=placeholder \
  GOOGLE_CLIENT_SECRET=placeholder \
  APP_JWT_PRIVATE_KEY="$(cat private.pem)" \
  APP_JWT_PUBLIC_KEY="$(cat public.pem)" \
  APP_FRONTEND_URL=http://localhost:5173 \
  mvn spring-boot:run
```

Generate RSA keys if not yet done:
```bash
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_pkcs8.pem -nocrypt
openssl rsa -in private.pem -pubout -out public.pem
```

Expected: App starts on port 8080, `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

- [ ] **Step 4: Final commit**

```bash
git add backend/
git commit -m "feat: complete backend implementation with full test coverage"
```
