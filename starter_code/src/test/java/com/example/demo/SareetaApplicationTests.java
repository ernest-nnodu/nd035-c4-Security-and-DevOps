package com.example.demo;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import com.fasterxml.jackson.core.type.TypeReference;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
	private ItemRepository itemRepository;

	@MockBean
	private OrderRepository orderRepository;

	@MockBean
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper json;

	private User mockUser;

	private Item mockItem;

	private Cart mockCart;

	private UserOrder mockOrder;

	@Test
	public void contextLoads() {
	}

	@BeforeEach
	void setup() {
		mockUser = createUser(1L, "user", "password");
		mockItem = createItem();
		mockCart = createCart();

		when(passwordEncoder.encode(anyString())).thenReturn("password");

		when(cartRepository.save(any())).thenReturn(mockCart);
		when(itemRepository.findById(mockItem.getId())).thenReturn(Optional.ofNullable(mockItem));

		when(userRepository.save(any(User.class))).thenReturn(mockUser);
		when(userRepository.findById(1L)).thenReturn(Optional.ofNullable(mockUser));
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
	@WithMockUser(username = "user")
	@DisplayName("User not found with invalid id")
	public void findUserById_withAuthenticatedUserAndInvalidId_isNotFound() throws Exception {

		mockMvc.perform(get("/api/user/id/100")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Unauthenticated user cannot retrieve a user by id")
	public void findUserById_withUnauthenticatedUserAndValidId_isForbidden() throws Exception {

		mockMvc.perform(get("/api/user/id/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "user")
	@DisplayName("Authenticated user can retrieve a user by username")
	public void findUserByUsername_withAuthenticatedUserAndUsername_returnsUser() throws Exception {
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
	@WithMockUser(username = "user")
	@DisplayName("User not found with invalid username")
	public void findUserByUsername_withAuthenticatedUserAndInvalidUsername_isNotFound() throws Exception {
		mockMvc.perform(get("/api/user/username")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Unauthenticated user cannot retrieve a user by username")
	public void findUserByUsername_withUnAuthenticatedUserAndUsername_isForbidden() throws Exception {
		mockMvc.perform(get("/api/user/user")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "user")
	@DisplayName("Authenticated user can add item to cart")
	public void addToCart_authenticatedUserAddItemToCart_returnsCartWithItem() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/cart/addToCart")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(createCartRequest(mockUser, mockItem, 1))))
				.andExpect(status().isOk())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		Cart returnedCart = json.readValue(response, Cart.class);

		assertAll(
				() -> assertNotNull(returnedCart),
				() -> assertEquals(mockUser.getId(), returnedCart.getUser().getId()),
				() -> assertEquals(mockItem.getId(), returnedCart.getItems().getFirst().getId()));
	}

	@Test
	@WithMockUser
	@DisplayName("Get items returns list of items")
	public void getItems_returnsItems() throws Exception {
		when(itemRepository.findAll()).thenReturn(List.of(mockItem));

		MvcResult result = mockMvc.perform(get("/api/item")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		List<Item> returnedItems = json.readValue(response, new TypeReference<List<Item>>() {});

		assertAll(
				() -> assertNotNull(returnedItems),
				() -> assertFalse(returnedItems.isEmpty()),
				() -> assertEquals(mockItem.getId(), returnedItems.getFirst().getId()));
	}

	@Test
	@WithMockUser
	@DisplayName("Get item by id")
	public void getItemById() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/item/1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		Item returnedItem = json.readValue(response, Item.class);

		assertAll(
				() -> assertNotNull(returnedItem),
				() -> assertEquals(mockItem.getId(), returnedItem.getId()));
	}

	@Test
	@DisplayName("Unauthenticated user cannot add item to cart")
	public void addToCart_unauthenticatedUserAddItemToCart_isForbidden() throws Exception {
		User user = new User();
		user.setUsername("testUser");

		Item item = new Item();
		item.setId(1L);

		mockMvc.perform(post("/api/cart/addToCart")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(createCartRequest(user, item, 1))))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	@DisplayName("Add to cart fails due to item not found")
	public void addToCart_invalidItemId_returnsNotFound() throws Exception {
		User user = new User();
		user.setUsername("testUser");

		Item item = new Item();
		item.setId(10L);

		mockMvc.perform(post("/api/cart/addToCart")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(createCartRequest(user, item, 1))))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "user")
	@DisplayName("Authenticated user can remove item from cart")
	public void removeFromCart_authenticatedUserRemoveItemFromCart_returnsEmptyCart() throws Exception {
		ModifyCartRequest request = createCartRequest(mockUser, mockItem, 1);
		Cart emptyCart = new Cart();
		emptyCart.setUser(mockUser);
		emptyCart.setItems(new ArrayList<>());

		when(cartRepository.save(any())).thenReturn(emptyCart);

		MvcResult result = mockMvc.perform(post("/api/cart/removeFromCart")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		Cart returnedCart = json.readValue(response, Cart.class);

		assertAll(
				() -> assertNotNull(returnedCart),
				() -> assertEquals(mockUser.getId(), returnedCart.getUser().getId()),
				() -> assertTrue(returnedCart.getItems().isEmpty()));
	}

	@Test
	@DisplayName("Remove from cart fails when user not authenticated")
	public void removeFromCart_unauthenticatedUserRemoveItemFromCart_isForbidden() throws Exception {
		ModifyCartRequest request = createCartRequest(mockUser, mockItem, 1);

		mockMvc.perform(post("/api/cart/removeFromCart")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	@DisplayName("Remove from cart fails when item not found")
	public void removeFromCart_invalidItemId_returnsNotFound() throws Exception {
		Item item = new Item();
		item.setId(10L);
		ModifyCartRequest request = createCartRequest(mockUser, item, 1);

		mockMvc.perform(post("/api/cart/removeFromCart")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	@DisplayName("Authenticated user can submit order")
	public void submitOrder_authenticatedUser_returnOrder() throws Exception {
		mockOrder = UserOrder.createFromCart(mockCart);
		mockUser.setCart(mockCart);
		when(orderRepository.save(any())).thenReturn(mockOrder);

		MvcResult result = mockMvc.perform(post("/api/order/submit/user")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		UserOrder order = json.readValue(response, UserOrder.class);

		assertAll(
				() -> assertNotNull(order),
				() -> assertFalse(order.getItems().isEmpty()),
				() -> assertEquals(mockUser.getId(), order.getUser().getId())
		);
	}

	@Test
	@DisplayName("Unauthenticated user cannot submit order")
	public void submitOrder_unauthenticatedUser_isForbidden() throws Exception {
		mockMvc.perform(post("/api/order/submit/testuser")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	@DisplayName("Submit order fails due to user not found")
	public void submitOrder_invalidUsername_returnsUserNotFound() throws Exception {
		mockMvc.perform(post("/api/order/submit/testuser")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	/*@Test
	@WithMockUser
	@DisplayName("")
	public void getOrderForUser_validUsername_returnsOrder() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/order/history/username")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andReturn();

		String response = result.getResponse().getContentAsString();
		List<UserOrder> userOrders = json.readValue(response, new TypeReference<List<UserOrder>>() {});

		assertAll(
				() -> assertNotNull(userOrders),
				() -> assertFalse(userOrders.isEmpty()),
				() -> assertEquals(mockUser.getId(), userOrders.getFirst().getUser().getId()),
				() -> assertEquals(mockItem.getId(), userOrders.getFirst().getItems().getFirst().getId()));
	}*/

	private User createUser(long id, String username, String password) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setPassword(password);
		user.setCart(new Cart());

		return user;
	}

	private CreateUserRequest createUserRequest(String username, String password, String confirmPassword) {
		CreateUserRequest userRequest = new CreateUserRequest();
		userRequest.setUsername(username);
		userRequest.setPassword(password);
		userRequest.setConfirmPassword(confirmPassword);

		return userRequest;
	}

	private Item createItem() {
		Item item = new Item();

		item.setId(1L);
		item.setName("Round widget");
		item.setPrice(new BigDecimal("2.99"));
		item.setDescription("Widget that is round");

		return item;
	}

	private ModifyCartRequest createCartRequest(User user, Item item, int quantity) {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername(user.getUsername());
		request.setItemId(item.getId());
		request.setQuantity(quantity);

		return request;
	}

	private Cart createCart() {
		Cart cart = new Cart();

		cart.setId(1L);
		cart.setUser(mockUser);
		cart.setItems(List.of(mockItem));

		return cart;
	}

}
