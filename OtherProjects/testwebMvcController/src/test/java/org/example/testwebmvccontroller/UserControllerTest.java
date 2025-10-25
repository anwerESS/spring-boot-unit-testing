package org.example.testwebmvccontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
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



// ============= CHEAT SHEET =============

/*
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
*/
