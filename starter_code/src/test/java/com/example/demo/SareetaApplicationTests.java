package com.example.demo;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.security.JWTUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

	private User mockUser;

	private String jwtToken;

	@Test
	public void contextLoads() {
	}

	@BeforeEach
	void setup() {
		mockUser = createUser(1L, "user", "password");

		when(passwordEncoder.encode(anyString())).thenReturn("password");
		when(cartRepository.save(any())).thenReturn(new Cart());
		when(userRepository.save(any(User.class))).thenReturn(mockUser);
		when(userRepository.findByUsername("user")).thenReturn(mockUser);
	}

	@Test
	@DisplayName("Create user with valid credentials is successful")
	public void createUser_withValidCredentials_returnsUserAndStatusOK() throws Exception {
		CreateUserRequest userRequest = createUserRequest("user", "password", "password");

		MvcResult result = mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(userRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		User returnedUser = json.readValue(responseBody, User.class);

		assertEquals(mockUser.getUsername(), returnedUser.getUsername());
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

	@Test
	@WithMockUser(username = "user")
	@DisplayName("Authenticated user can retrieve a user by id")
	public void findUserById_withAuthenticatedUserAndValidId_returnsUser() throws Exception {
		User mockUser = createUser(1L, "user", "password");

		when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

		MvcResult result = mockMvc.perform(get("/api/user/id/1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		User returnedUser = json.readValue(responseBody, User.class);

		assertAll(
				() -> assertNotNull(returnedUser),
				() -> assertEquals(mockUser.getId(), returnedUser.getId())
		);
	}

	@Test
	@DisplayName("Unauthenticated user cannot retrieve a user by id")
	public void findUserById_withUnauthenticatedUserAndValidId_isForbidden() throws Exception {
		User mockUser = createUser(1L, "user", "password");

		when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
		jwtToken = JWTUtils.generateToken("user");

		mockMvc.perform(get("/api/user/id/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "user")
	@DisplayName("Authenticated user can retrieve a user by username")
	public void findUserByUsername_withAuthenticatedUserAndUsername_returnsUser() throws Exception {
		User mockUser = createUser(1L, "user", "password");

		when(userRepository.findByUsername(anyString())).thenReturn(mockUser);

		MvcResult result = mockMvc.perform(get("/api/user/user")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		User returnedUser = json.readValue(responseBody, User.class);

		assertAll(
				() -> assertNotNull(returnedUser),
				() -> assertEquals(mockUser.getUsername(), returnedUser.getUsername())
		);
	}

	@Test
	@DisplayName("Unauthenticated user cannot retrieve a user by username")
	public void findUserByUsername_withUnAuthenticatedUserAndUsername_isForbidden() throws Exception {
		User mockUser = createUser(1L, "user", "password");

		when(userRepository.findByUsername(anyString())).thenReturn(mockUser);

		mockMvc.perform(get("/api/user/user")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
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
