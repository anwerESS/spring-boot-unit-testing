package org.example.testwebmvccontroller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerAdvancedTest {

@Autowired
private MockMvc mockMvc;

@MockitoBean
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
