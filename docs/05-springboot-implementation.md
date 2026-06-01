# EduLearn Platform — Spring Boot Implementation Guide

## 1. Project Setup — pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
  </parent>

  <groupId>com.edulearn</groupId>
  <artifactId>edulearn-platform</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <name>EduLearn Platform</name>
  <description>Education platform with approval workflow</description>

  <properties>
    <java.version>17</java.version>
    <jjwt.version>0.12.3</jjwt.version>
  </properties>

  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <!-- JWT — all three required together -->
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <!-- Dev tools -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-devtools</artifactId><scope>runtime</scope><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    </plugins>
  </build>
</project>
```

---

## 2. application.properties

```properties
# ── Server (localhost only) ──────────────────────────────────
server.port=8080
server.address=127.0.0.1

# ── PostgreSQL ───────────────────────────────────────────────
spring.datasource.url=jdbc:postgresql://localhost:5432/edulearn
spring.datasource.username=edulearn_user
spring.datasource.password=changeme
spring.datasource.driver-class-name=org.postgresql.Driver

# ── JPA / Hibernate ─────────────────────────────────────────
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# ── JWT ──────────────────────────────────────────────────────
app.jwt.secret=change-this-to-a-256-bit-random-string-in-production
app.jwt.expiration-ms=86400000

# ── Static resources & SPA routing ──────────────────────────
spring.web.resources.static-locations=classpath:/static/
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false

# ── Logging ──────────────────────────────────────────────────
logging.level.root=WARN
logging.level.com.edulearn=INFO
```

---

## 3. Package Structure

```
src/main/java/com/edulearn/
├── EduLearnApplication.java
├── config/
│   ├── SecurityConfig.java          # JWT filter chain, CORS localhost only
│   ├── WebMvcConfig.java            # SPA fallback routing
│   └── JwtProperties.java           # @ConfigurationProperties for jwt.*
├── controller/
│   ├── AuthController.java          # POST /api/v1/auth/login|logout, GET /auth/me
│   ├── CategoryController.java
│   ├── SubjectController.java
│   ├── TopicController.java
│   ├── QuestionController.java
│   └── ApprovalController.java      # GET /approvals/pending, GET /my/submissions
├── service/
│   ├── AuthService.java
│   ├── ContentApprovalService.java  # shared approve/reject/submit logic
│   ├── CategoryService.java
│   ├── SubjectService.java
│   ├── TopicService.java
│   └── QuestionService.java
├── repository/
│   ├── UserRepository.java
│   ├── CategoryRepository.java
│   ├── SubjectRepository.java
│   ├── TopicRepository.java
│   ├── QuestionRepository.java
│   ├── QuestionOptionRepository.java
│   └── ApprovalLogRepository.java
├── entity/
│   ├── User.java
│   ├── Category.java
│   ├── Subject.java
│   ├── Topic.java
│   ├── Question.java
│   ├── QuestionOption.java
│   └── ApprovalLog.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── CategoryRequest.java
│   │   ├── SubjectRequest.java
│   │   ├── TopicRequest.java
│   │   ├── QuestionRequest.java
│   │   └── ApprovalActionRequest.java   # { "note": "looks good" }
│   └── response/
│       ├── AuthResponse.java            # { token, role, name }
│       ├── CategoryResponse.java
│       ├── SubjectResponse.java
│       ├── TopicResponse.java
│       ├── QuestionResponse.java
│       ├── TreeNodeResponse.java        # full nested tree
│       └── SubmissionResponse.java      # teacher's pending items
├── enums/
│   ├── UserRole.java                    # ADMIN, TEACHER, STUDENT
│   ├── ContentStatus.java               # DRAFT, PENDING, APPROVED, REJECTED
│   ├── QuestionType.java
│   └── CodeLanguage.java
├── exception/
│   ├── GlobalExceptionHandler.java      # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── ForbiddenException.java
│   └── ValidationException.java
├── security/
│   ├── JwtAuthFilter.java               # extends OncePerRequestFilter
│   ├── JwtUtil.java                     # generate / validate / extract claims
│   └── UserDetailsServiceImpl.java      # loads User from DB for Spring Security
└── util/
    ├── ApiResponse.java                 # standard envelope {status,message,data,pagination}
    └── PageMeta.java                    # {page, size, total, totalPages}

src/main/resources/
├── application.properties
└── static/
    ├── login.html
    ├── css/main.css
    ├── js/
    │   ├── auth.js                      # login/logout/token helpers
    │   ├── api.js                       # fetch wrapper with Authorization header
    │   └── tree.js                      # reusable tree component
    ├── admin/
    │   ├── dashboard.html
    │   ├── courses.html
    │   └── submissions.html
    └── teacher/
        ├── dashboard.html
        ├── courses.html
        └── submissions.html
```

---

## 4. Key Entity: Question.java

```java
@Entity
@Table(name = "questions")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE questions SET is_deleted=true, deleted_at=NOW() WHERE id=?")
public class Question {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private QuestionType questionType;

    private Short marks = 1;
    private String answerGuide;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();

    // Type-specific fields
    private Boolean correctBoolean;       // TRUE_FALSE
    private String  modelAnswer;          // SHORT_ANSWER
    @Enumerated(EnumType.STRING)
    private CodeLanguage codeLang;        // CODE
    private String starterCode;
    private String expectedOutput;
    private String imageUrl;              // IMAGE_BASED
    private String imageAltText;

    @Enumerated(EnumType.STRING)
    private ContentStatus status = ContentStatus.DRAFT;
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by")
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by")
    private User approvedBy;

    private OffsetDateTime approvedAt;
    private boolean isDeleted = false;
    private OffsetDateTime deletedAt;

    @CreationTimestamp private OffsetDateTime createdAt;
    @UpdateTimestamp  private OffsetDateTime updatedAt;
}
```

---

## 5. Security — JwtAuthFilter.java

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);
                UserDetails user = userDetailsService.loadUserByUsername(email);
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
```

---

## 6. Security Config — localhost CORS + public routes

```java
@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/", "/login.html", "/css/**", "/js/**",
                                 "/admin/**", "/teacher/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**",
                                 "/api/v1/subjects/**", "/api/v1/topics/**",
                                 "/api/v1/questions/**").permitAll()
                .requestMatchers("/api/v1/approvals/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:8080", "http://127.0.0.1:8080"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", cfg);
        return src;
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean public AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception { return c.getAuthenticationManager(); }
}
```

---

## 7. SPA Fallback — WebMvcConfig.java

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
    // Fallback: unknown routes → login.html so browser navigation works
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addErrorPages(
            new ErrorPage(HttpStatus.NOT_FOUND, "/login.html")
        );
    }
}
```

---

## 8. Approval Logic — ContentApprovalService.java

```java
@Service @RequiredArgsConstructor @Transactional
public class ContentApprovalService {
    private final ApprovalLogRepository logRepo;

    public void submit(ApprovableEntity entity, User actor) {
        ContentStatus prev = entity.getStatus();
        if (actor.getRole() == UserRole.ADMIN) {
            entity.setStatus(ContentStatus.APPROVED);
            entity.setApprovedBy(actor);
            entity.setApprovedAt(OffsetDateTime.now());
        } else {
            entity.setStatus(ContentStatus.PENDING);
        }
        entity.setUpdatedBy(actor);
        log(entity, prev, entity.getStatus(), actor, null);
    }

    public void approve(ApprovableEntity entity, User admin, String note) {
        ContentStatus prev = entity.getStatus();
        entity.setStatus(ContentStatus.APPROVED);
        entity.setApprovedBy(admin);
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setRejectionReason(null);
        entity.setUpdatedBy(admin);
        log(entity, prev, ContentStatus.APPROVED, admin, note);
    }

    public void reject(ApprovableEntity entity, User admin, String note) {
        ContentStatus prev = entity.getStatus();
        entity.setStatus(ContentStatus.REJECTED);
        entity.setRejectionReason(note);
        entity.setUpdatedBy(admin);
        log(entity, prev, ContentStatus.REJECTED, admin, note);
    }

    private void log(ApprovableEntity e, ContentStatus from, ContentStatus to, User actor, String note) {
        logRepo.save(ApprovalLog.builder()
            .entityType(e.getClass().getSimpleName().toUpperCase())
            .entityId(e.getId())
            .fromStatus(from).toStatus(to)
            .actionedBy(actor).note(note)
            .build());
    }
}
```

---

## 9. Standard API Response Wrapper

```java
public record ApiResponse<T>(String status, String message, T data, PageMeta pagination) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "OK", data, null);
    }
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>("success", msg, data, null);
    }
    public static <T> ApiResponse<T> paged(T data, Page<?> page) {
        return new ApiResponse<>("success", "OK", data,
            new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
    public static ApiResponse<Void> error(String msg) {
        return new ApiResponse<>("error", msg, null, null);
    }
}

public record PageMeta(int page, int size, long total, int totalPages) {}
```

---

## 10. Sample DTOs

```java
// CategoryRequest.java
public record CategoryRequest(
    @NotBlank @Size(max=120) String name,
    String description
) {}

// CategoryResponse.java
public record CategoryResponse(
    UUID id, String name, String description,
    ContentStatus status, String rejectionReason,
    String createdByName, OffsetDateTime createdAt
) {}

// ApprovalActionRequest.java
public record ApprovalActionRequest(String note) {}

// QuestionRequest.java
public record QuestionRequest(
    @NotBlank String questionText,
    @NotNull QuestionType questionType,
    @Min(1) Short marks,
    String answerGuide,
    // MCQ
    List<OptionRequest> options,
    // TRUE_FALSE
    Boolean correctBoolean,
    // SHORT_ANSWER
    String modelAnswer,
    // CODE
    CodeLanguage codeLang,
    String starterCode,
    String expectedOutput,
    // IMAGE_BASED
    String imageUrl,
    String imageAltText
) {}

public record OptionRequest(
    @NotBlank String optionText,
    boolean isCorrect,
    int displayOrder
) {}
```

---

## 11. Run Locally

```bash
# 1. Create the PostgreSQL database and user
psql -U postgres -c "CREATE DATABASE edulearn;"
psql -U postgres -c "CREATE USER edulearn_user WITH PASSWORD 'changeme';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE edulearn TO edulearn_user;"

# 2. Run schema DDL
psql -U edulearn_user -d edulearn -f sql/02-database-schema.sql

# 3. Build and start
./mvnw spring-boot:run

# 4. Open browser
open http://localhost:8080/login.html
```
