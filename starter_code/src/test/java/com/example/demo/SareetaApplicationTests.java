package com.example.demo;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SareetaApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private CartRepository cartRepository;

	@MockBean
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper json;

	@Test
	public void contextLoads() {
	}

	@Test
	@DisplayName("Create user with valid credentials is successful")
	public void createUser_withValidCredentials_returnsUserAndStatusOK() throws Exception {
		User mockUser = createUser(1L, "user", "password");
		CreateUserRequest userRequest = createUserRequest("user", "password", "password");

		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		when(cartRepository.save(any())).thenReturn(new Cart());
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		MvcResult result = mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(userRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		User returnedUser = json.readValue(responseBody, User.class);

		Assertions.assertEquals(mockUser.getUsername(), returnedUser.getUsername());
	}

	@Test
	@DisplayName("Create user with short password is rejected")
	public void createUser_withShortPassword_returnsBadRequest() throws Exception {
		CreateUserRequest userRequest = createUserRequest("user", "pass", "pass");

		mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(userRequest)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Create user with mismatch between passwords is rejected")
	public void createUser_withPasswordsMismatch_returnsBadRequest() throws Exception {
		CreateUserRequest userRequest = createUserRequest("user", "pass", "word");

		mockMvc.perform(post("/api/user/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(userRequest)))
				.andExpect(status().isBadRequest());
	}


	private User createUser(long id, String username, String password) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setPassword(password);

		return user;
	}

	private CreateUserRequest createUserRequest(String username, String password, String confirmPassword) {
		CreateUserRequest userRequest = new CreateUserRequest();
		userRequest.setUsername(username);
		userRequest.setPassword(password);
		userRequest.setConfirmPassword(confirmPassword);

		return userRequest;
	}

}
