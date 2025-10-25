Let me explain how to test MVC controllers with MockMvc!Perfect! Let me break down MockMvc testing:

## What is MockMvc?

MockMvc simulates HTTP requests to your controller **without starting a real web server**. It's like sending requests through a fake browser.

## Two Main Approaches

### 1. @WebMvcTest (RECOMMENDED - Fast)
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService; // Mock the service!
}
```

**What happens:**
- Loads **only the web layer** (controllers)
- Service is **mocked** - no real business logic
- Super fast (no database, no full Spring context)

### 2. @SpringBootTest + @AutoConfigureMockMvc (Full Integration)
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserRepository repository; // Mock only repository
}
```

**What happens:**
- Loads **full Spring context**
- Real service layer runs
- Slower but more realistic

## Key MockMvc Patterns

**Basic Request:**
```java
mockMvc.perform(get("/api/users/1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.name").value("John"));
```

**POST with JSON body:**
```java
mockMvc.perform(post("/api/users")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(user)))
    .andExpect(status().isCreated());
```

**Verify service was called:**
```java
verify(userService).getUserById(1L);
```

## Important Notes

1. **@WebMvcTest only loads the controller** - you MUST mock dependencies with @MockBean
2. **jsonPath** is used to check JSON responses (like jQuery selectors)
3. **ObjectMapper** converts Java objects to JSON strings
4. Use `.andDo(print())` to see full request/response details when debugging

<br><br><br><br><br><br><br><br>



```Java
// ============= PRODUCTION CODE =============

// User.java
public class User {
private Long id;
private String name;
private String email;
private int age;

    // Constructors, getters, setters...
    public User() {}
    
    public User(Long id, String name, String email, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

// UserService.java
@Service
public class UserService {
private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    
    public User createUser(User user) {
        if (user.getAge() < 18) {
            throw new IllegalArgumentException("User must be 18 or older");
        }
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setAge(userDetails.getAge());
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}

// UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody User userDetails) {
        User updated = userService.updateUser(id, userDetails);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

// ErrorResponse.java
public class ErrorResponse {
private String code;
private String message;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    // Getters
    public String getCode() { return code; }
    public String getMessage() { return message; }
}

// ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
public ResourceNotFoundException(String message) {
super(message);
}
}

// ============= TEST 1: @WebMvcTest (Lightweight, RECOMMENDED) =============

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // ===== GET /api/users =====
    
    @Test
    void getAllUsers_ReturnsListOfUsers() throws Exception {
        // Arrange
        List<User> users = Arrays.asList(
            new User(1L, "John Doe", "john@example.com", 25),
            new User(2L, "Jane Smith", "jane@example.com", 30)
        );
        when(userService.getAllUsers()).thenReturn(users);
        
        // Act & Assert
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("John Doe"))
            .andExpect(jsonPath("$[0].email").value("john@example.com"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Jane Smith"));
        
        verify(userService, times(1)).getAllUsers();
    }
    
    @Test
    void getAllUsers_WhenNoUsers_ReturnsEmptyList() throws Exception {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        
        // Act & Assert
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
    
    // ===== GET /api/users/{id} =====
    
    @Test
    void getUserById_WhenUserExists_ReturnsUser() throws Exception {
        // Arrange
        User user = new User(1L, "John Doe", "john@example.com", 25);
        when(userService.getUserById(1L)).thenReturn(user);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.age").value(25));
        
        verify(userService).getUserById(1L);
    }
    
    @Test
    void getUserById_WhenUserDoesNotExist_Returns404() throws Exception {
        // Arrange
        when(userService.getUserById(999L))
            .thenThrow(new ResourceNotFoundException("User not found with id: 999"));
        
        // Act & Assert
        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User not found with id: 999"));
    }
    
    // ===== POST /api/users =====
    
    @Test
    void createUser_WithValidData_ReturnsCreatedUser() throws Exception {
        // Arrange
        User inputUser = new User(null, "Alice", "alice@example.com", 22);
        User savedUser = new User(1L, "Alice", "alice@example.com", 22);
        
        when(userService.createUser(any(User.class))).thenReturn(savedUser);
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputUser)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"));
        
        verify(userService).createUser(any(User.class));
    }
    
    @Test
    void createUser_WithUnderageUser_Returns400() throws Exception {
        // Arrange
        User underageUser = new User(null, "Minor", "minor@example.com", 15);
        
        when(userService.createUser(any(User.class)))
            .thenThrow(new IllegalArgumentException("User must be 18 or older"));
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(underageUser)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("User must be 18 or older"));
    }
    
    @Test
    void createUser_WithInvalidJson_Returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
            .andExpect(status().isBadRequest());
    }
    
    // ===== PUT /api/users/{id} =====
    
    @Test
    void updateUser_WithValidData_ReturnsUpdatedUser() throws Exception {
        // Arrange
        User updatedUser = new User(1L, "John Updated", "john.updated@example.com", 26);
        
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);
        
        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Updated"))
            .andExpect(jsonPath("$.email").value("john.updated@example.com"));
        
        verify(userService).updateUser(eq(1L), any(User.class));
    }
    
    @Test
    void updateUser_WhenUserDoesNotExist_Returns404() throws Exception {
        // Arrange
        User user = new User(null, "NonExistent", "none@example.com", 25);
        
        when(userService.updateUser(eq(999L), any(User.class)))
            .thenThrow(new ResourceNotFoundException("User not found with id: 999"));
        
        // Act & Assert
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isNotFound());
    }
    
    // ===== DELETE /api/users/{id} =====
    
    @Test
    void deleteUser_WhenUserExists_Returns204() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1L);
        
        // Act & Assert
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());
        
        verify(userService).deleteUser(1L);
    }
    
    @Test
    void deleteUser_WhenUserDoesNotExist_Returns404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
            .when(userService).deleteUser(999L);
        
        // Act & Assert
        mockMvc.perform(delete("/api/users/999"))
            .andExpect(status().isNotFound());
    }
}

// ============= TEST 2: @SpringBootTest with MockMvc (Full Integration) =============

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void fullIntegrationTest_CreateAndRetrieveUser() throws Exception {
        // Arrange
        User user = new User(null, "Integration Test", "integration@example.com", 30);
        User savedUser = new User(1L, "Integration Test", "integration@example.com", 30);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));
        
        // Act & Assert - Create user
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
        
        // Act & Assert - Retrieve user
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Integration Test"));
    }
}

// ============= TEST 3: Testing Request Headers & Parameters =============

@WebMvcTest(UserController.class)
class UserControllerAdvancedTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testWithCustomHeaders() throws Exception {
        // Arrange
        User user = new User(1L, "Header Test", "test@example.com", 25);
        when(userService.getUserById(1L)).thenReturn(user);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                .header("X-Custom-Header", "CustomValue")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("$.name").value("Header Test"));
    }
    
    @Test
    void testResponseBodyInDetail() throws Exception {
        // Arrange
        User user = new User(1L, "Detailed Test", "detail@example.com", 28);
        when(userService.getUserById(1L)).thenReturn(user);
        
        // Act & Assert with andDo(print()) to see full request/response
        mockMvc.perform(get("/api/users/1"))
            .andDo(print()) // Prints request and response details
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").isNotEmpty())
            .andExpect(jsonPath("$.email").value(containsString("@")));
    }
}
```
```
// ============= CHEAT SHEET =============


MockMvc Common Methods:
┌────────────────────────────────────────────────────────────────────┐
│ REQUEST                                                             │
├────────────────────────────────────────────────────────────────────┤
│ perform(get("/path"))              - GET request                   │
│ perform(post("/path"))             - POST request                  │
│ perform(put("/path"))              - PUT request                   │
│ perform(delete("/path"))           - DELETE request                │
│ perform(patch("/path"))            - PATCH request                 │
│                                                                     │
│ .contentType(MediaType.JSON)       - Set Content-Type              │
│ .content(json)                     - Set request body              │
│ .header("name", "value")           - Add header                    │
│ .param("key", "value")             - Add query parameter           │
│ .accept(MediaType.JSON)            - Set Accept header             │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│ ASSERTIONS                                                          │
├────────────────────────────────────────────────────────────────────┤
│ .andExpect(status().isOk())        - Assert HTTP 200               │
│ .andExpect(status().isCreated())   - Assert HTTP 201               │
│ .andExpect(status().isNotFound())  - Assert HTTP 404               │
│ .andExpect(status().isBadRequest())- Assert HTTP 400               │
│                                                                     │
│ .andExpect(jsonPath("$.name").value("John"))  - Check JSON field   │
│ .andExpect(jsonPath("$", hasSize(2)))         - Check array size   │
│ .andExpect(jsonPath("$.id").exists())         - Check field exists │
│                                                                     │
│ .andExpect(content().string("text"))          - Check body text    │
│ .andExpect(header().exists("name"))           - Check header       │
│                                                                     │
│ .andDo(print())                    - Print request/response        │
└────────────────────────────────────────────────────────────────────┘

Annotations Comparison:
┌─────────────────────┬──────────────────┬─────────────────────────┐
│ Annotation          │ Loads           │ Use Case                 │
├─────────────────────┼──────────────────┼─────────────────────────┤
│ @WebMvcTest         │ Only MVC layer   │ Fast controller tests    │
│                     │ Mock service     │ (RECOMMENDED)            │
├─────────────────────┼──────────────────┼─────────────────────────┤
│ @SpringBootTest +   │ Full context     │ End-to-end integration   │
│ @AutoConfigureMockMvc│                 │ tests                    │
└─────────────────────┴──────────────────┴─────────────────────────┘
```

