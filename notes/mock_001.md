I'll help clarify these testing annotations! They can be confusing at first because some are from Mockito, some from Spring, and they have overlapping purposes.

## Core Mockito Annotations

**@Mock**
- Creates a mock (fake) object
- All methods return default values (null, 0, false, etc.) unless you stub them
- Use when you want a completely fake version of a dependency

```java
@Mock
private UserRepository userRepository;
```

**@InjectMocks**
- Creates an instance of the class under test
- Automatically injects all @Mock or @Spy objects into it
- Use on the class you're actually testing

```java
@InjectMocks
private UserService userService; // UserRepository will be injected here
```

**@Spy**
- Creates a partial mock - uses the real object but you can stub specific methods
- Real methods are called unless you override them
- Use when you want mostly real behavior with some mocked methods

```java
@Spy
private List<String> spyList = new ArrayList<>();
```

## Spring Test Annotations

**@Autowired**
- Used in Spring integration tests (not unit tests)
- Injects real Spring beans from the application context
- Use when you want to test with actual Spring components

```java
@Autowired
private UserService userService; // Real bean from Spring context
```

**@MockBean**
- Spring Boot annotation
- Creates a mock AND adds it to the Spring application context
- Replaces any existing bean of the same type
- Use in Spring integration tests when you want to mock a Spring bean

```java
@MockBean
private UserRepository userRepository; // Mock in Spring context
```

## Quick Comparison

**Unit Tests (Pure Mockito):**
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
}
```

**Spring Integration Tests:**
```java
@SpringBootTest
class UserServiceIntegrationTest {
    @MockBean
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
}
```

## Key Rules

1. **@Mock vs @MockBean**: Use @Mock for unit tests (no Spring), @MockBean for Spring integration tests
2. **@InjectMocks vs @Autowired**: Use @InjectMocks for the class under test in unit tests, @Autowired in Spring tests
3. **@Spy**: Use sparingly - it's usually better to fully mock or fully use real objects



<br><br><br><br><br><br><br>
 
Here's a concrete example with a simple service that manages users:Here's a complete example! Let me break down what's happening:

## Key Differences in the Examples:

**Unit Test (Pure Mockito):**
- Uses `@Mock` - creates fake UserRepository and EmailService
- Uses `@InjectMocks` - creates UserService and injects the mocks
- **Fast** - no Spring context needed
- **Isolated** - tests only UserService logic

**Spring Integration Test:**
- Uses `@MockBean` - creates mocks IN the Spring context
- Uses `@Autowired` - gets UserService from Spring (with mocked dependencies)
- **Slower** - loads Spring context
- **More realistic** - tests with Spring features (DI, AOP, etc.)

**@Spy Example:**
- The EmailService actually executes its real code
- But you can still verify it was called
- Useful when you want partial mocking

## When to use what?

- **95% of the time**: Use unit tests with `@Mock` and `@InjectMocks` (fast!)
- **Integration tests**: Use `@MockBean` and `@Autowired` when testing Spring-specific features
- **@Spy**: Rarely needed - only when you need real behavior + verification

```java
// ============= PRODUCTION CODE =============

// User.java
public class User {
private Long id;
private String name;
private String email;

    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}

// UserRepository.java (Spring Data JPA interface)
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
Optional<User> findByEmail(String email);
}

// EmailService.java
@Service
public class EmailService {
public void sendWelcomeEmail(String email) {
// Imagine this sends a real email
System.out.println("Sending email to: " + email);
}
}

// UserService.java (The class we want to test)
@Service
public class UserService {
private final UserRepository userRepository;
private final EmailService emailService;

    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    public User createUser(String name, String email) {
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        
        // Create and save user
        User user = new User(null, name, email);
        User savedUser = userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(email);
        
        return savedUser;
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

// ============= UNIT TEST (Pure Mockito) =============

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void createUser_Success() {
        // Arrange
        String name = "John Doe";
        String email = "john@example.com";
        User savedUser = new User(1L, name, email);
        
        // Mock the repository behavior
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(name, email);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(name, result.getName());
        assertEquals(email, result.getEmail());
        
        // Verify interactions
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(email);
    }
    
    @Test
    void createUser_UserAlreadyExists_ThrowsException() {
        // Arrange
        String email = "existing@example.com";
        User existingUser = new User(1L, "Existing User", email);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("New Name", email);
        });
        
        // Verify save was never called
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString());
    }
    
    @Test
    void getUserById_UserExists() {
        // Arrange
        Long userId = 1L;
        User user = new User(userId, "Jane Doe", "jane@example.com");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Act
        User result = userService.getUserById(userId);
        
        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).findById(userId);
    }
}

// ============= SPRING INTEGRATION TEST =============

@SpringBootTest
class UserServiceIntegrationTest {

    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private EmailService emailService;
    
    @Autowired
    private UserService userService;
    
    @Test
    void createUser_WithSpringContext() {
        // Arrange
        String name = "Alice Smith";
        String email = "alice@example.com";
        User savedUser = new User(10L, name, email);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(name, email);
        
        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(emailService).sendWelcomeEmail(email);
    }
}

// ============= EXAMPLE WITH @Spy =============

@ExtendWith(MockitoExtension.class)
class UserServiceSpyTest {

    @Mock
    private UserRepository userRepository;
    
    @Spy
    private EmailService emailService; // Using real EmailService but can spy on it
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void createUser_VerifyRealEmailServiceCalled() {
        // Arrange
        User savedUser = new User(1L, "Bob", "bob@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        userService.createUser("Bob", "bob@example.com");
        
        // Assert - verify real method was called
        verify(emailService).sendWelcomeEmail("bob@example.com");
        // The real sendWelcomeEmail() actually executed (printed to console)
    }
}
```


<br><br><br><br><br><br><br>



# What Happens Internally in Each Test

## 1. Unit Test with @Mock and @InjectMocks

```java
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
}
```

### Step-by-Step Internal Process:

**Before each test method:**

1. **MockitoExtension starts**
    - JUnit 5 detects `@ExtendWith(MockitoExtension.class)`
    - Mockito extension intercepts the test lifecycle

2. **@Mock annotations are processed**
    - Mockito scans the test class for `@Mock` annotations
    - For each @Mock field:
        - Creates a **Proxy object** using CGLIB or ByteBuddy
        - This proxy intercepts ALL method calls
        - Returns default values: `null` for objects, `0` for numbers, `false` for booleans
        - Stores the mock in Mockito's internal registry

3. **@InjectMocks is processed**
    - Mockito looks at `UserService` constructor/fields
    - Finds it needs: `UserRepository` and `EmailService`
    - **Injects the mock objects** created in step 2
    - Creates a **REAL instance** of `UserService` (not a mock!)
    - UserService constructor is called: `new UserService(mockRepo, mockEmail)`

4. **Test method runs**
    - When you call `userService.createUser()`, it runs the REAL code
    - But when it calls `userRepository.save()`, it hits the MOCK
    - The mock returns what you specified in `when(...).thenReturn(...)`

**Memory View:**
```
userRepository → [Mockito Proxy] → returns stubbed values
emailService   → [Mockito Proxy] → returns stubbed values
userService    → [Real UserService object] → uses the proxies above
```

---

## 2. Spring Integration Test with @MockBean

```java
@SpringBootTest
class UserServiceIntegrationTest {
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private EmailService emailService;
    
    @Autowired
    private UserService userService;
}
```

### Step-by-Step Internal Process:

**Before test class:**

1. **Spring Test Context starts**
    - `@SpringBootTest` tells Spring to bootstrap entire application
    - Spring scans for `@Component`, `@Service`, `@Repository`, etc.
    - Normally creates real beans for everything

2. **@MockBean intervention**
    - Spring detects `@MockBean` annotations
    - **BEFORE** creating real beans:
        - Creates Mockito mocks for UserRepository and EmailService
        - **Replaces** these beans in the ApplicationContext
        - If a real bean existed, it's thrown away

3. **Spring Dependency Injection**
    - Spring creates UserService bean
    - Looks for dependencies: UserRepository and EmailService
    - Finds the MOCK beans in the context
    - Injects the mocks into UserService constructor
    - UserService is still a REAL object managed by Spring

4. **@Autowired injection**
    - Spring injects the UserService bean into your test
    - This is the same bean from the ApplicationContext

**Memory View:**
```
Spring ApplicationContext:
  ├─ userRepository → [Mockito Proxy] (replaced real bean)
  ├─ emailService   → [Mockito Proxy] (replaced real bean)
  └─ userService    → [Real UserService] (uses mocks above)
       ↓
  @Autowired userService in test → same instance from context
```

---

## 3. Using @Spy

```java
@ExtendWith(MockitoExtension.class)
class UserServiceSpyTest {
    @Mock
    private UserRepository userRepository;
    
    @Spy
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
}
```

### Step-by-Step Internal Process:

1. **@Spy creates a partial mock**
    - Mockito creates a **REAL EmailService object**: `new EmailService()`
    - Then wraps it in a **Proxy**
    - The proxy intercepts method calls BUT:
        - If you stub it with `when()`, it returns the stubbed value
        - If you DON'T stub it, it calls the REAL method

2. **Method call flow with Spy:**
   ```
   emailService.sendWelcomeEmail("test@example.com")
       ↓
   [Mockito Spy Proxy]
       ↓
   Check: Is this method stubbed?
       ├─ YES → return stubbed value
       └─ NO  → call real method on real EmailService object
   ```

**Comparison:**

| Annotation | Object Created | Method Behavior |
|------------|----------------|-----------------|
| @Mock | Proxy only (no real object) | Always return defaults unless stubbed |
| @Spy | Real object + Proxy wrapper | Calls real methods unless stubbed |

---

## Impact of @ExtendWith(MockitoExtension.class)

### Without MockitoExtension:

```java
class UserServiceTest {
    @Mock
    private UserRepository userRepository; // ❌ Stays null!
    
    @Test
    void test() {
        // userRepository is NULL → NullPointerException
    }
}
```

### With MockitoExtension:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository; // ✅ Initialized!
}
```

**What the Extension Does:**

1. **Registers with JUnit 5**
    - Hooks into JUnit lifecycle callbacks
    - Runs before each test method

2. **Initializes Mockito annotations**
    - Equivalent to calling `MockitoAnnotations.openMocks(this)`
    - Scans test class for @Mock, @Spy, @InjectMocks
    - Creates and injects all mock objects

3. **Validates mock usage**
    - Checks for unused stubs (strict stubbing)
    - Reports mismatches between stubs and actual calls
    - Cleans up after each test

4. **Enables Mockito features**
    - Allows `@Captor` for capturing arguments
    - Enables verification (`verify()`)
    - Supports `@MockBean` integration in some contexts

**Alternative (Old JUnit 4 way):**
```java
class UserServiceTest {
    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this); // Manual initialization
    }
}
```

---

## Performance Comparison

| Test Type | Startup Time | Memory | Use Case |
|-----------|--------------|--------|----------|
| **Unit Test** (@Mock) | ~50ms | Low | Fast, isolated tests |
| **Integration Test** (@MockBean) | ~3-10s | High | Test Spring features |
| **Spy Test** (@Spy) | ~50ms | Medium | Real code + verification |

---

## Key Takeaways

1. **@Mock** → Creates fake proxy, no real object
2. **@Spy** → Creates real object + proxy wrapper
3. **@MockBean** → Creates fake proxy + adds to Spring context
4. **@ExtendWith(MockitoExtension.class)** → Initializes all Mockito annotations automatically
5. **@InjectMocks** → Creates REAL object of class under test, injects mocks
6. **@Autowired** → Gets bean from Spring context (real or mocked)

The main difference: **Unit tests bypass Spring entirely** (fast), while **Integration tests use Spring's dependency injection** (slower but more realistic).