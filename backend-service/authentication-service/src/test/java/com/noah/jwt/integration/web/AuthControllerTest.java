package com.noah.jwt.integration.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.jwt.dto.ErrorDto;
import com.noah.jwt.dto.JwtTokenDto;
import com.noah.jwt.dto.LoginDto;
import com.noah.jwt.integration.config.PostgresTest;
import com.noah.jwt.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static com.noah.jwt.integration.config.JsonObjectMapper.asObject;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@AutoConfigureMockMvc
@SpringBootTest
class AuthControllerTest extends PostgresTest {

  private static final String REGISTER_URL = "/api/auth/register";
  private static final String LOGIN_URL = "/api/auth/login";
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private UserService userService;

  @Test
  @DisplayName("Register user")
  void testRegister() throws Exception {
    String username = "test";
    String json = getLoginJson(username, PASSWORD);
    performPostRequestAndExpectStatus(json, REGISTER_URL, MockMvcResultMatchers.status().isOk());
    UserDetails loadedUser = userService.loadUserByUsername(username);
    Assertions.assertEquals(username, loadedUser.getUsername());
  }

  @Test
  @DisplayName("Register with existing username")
  void testRegisterExistingUsername() throws Exception {
    String json = new ObjectMapper().writeValueAsString(
        LoginDto.builder()
            .username(USERNAME)
            .password(PASSWORD)
            .build()
    );
    MvcResult result = performPostRequestAndExpectStatus(json, REGISTER_URL,
        MockMvcResultMatchers.status().is4xxClientError())
        .andExpect(
            content().contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
    
    ErrorDto error = asObject(result.getResponse().getContentAsString(), ErrorDto.class);
    Assertions.assertEquals("DUPLICATE_USER_NAME", error.getCode());
  }

  @Test
  @DisplayName("Test Login")
  void testLogin() throws Exception {
    String json = getLoginJson(USERNAME, PASSWORD);
    MvcResult result =
        performPostRequestAndExpectStatus(json, LOGIN_URL, MockMvcResultMatchers.status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();
    JwtTokenDto responseTokenDTO = new ObjectMapper().readValue(
        result.getResponse().getContentAsString(),
        JwtTokenDto.class
    );
    Assertions.assertNotNull(responseTokenDTO);
  }

  @Test
  @DisplayName("Test Invalid Login - via password")
  void testInvalidLogin() throws Exception {
    String json = getLoginJson(USERNAME, PASSWORD + "wrong");
    performPostRequestAndExpectStatus(json, LOGIN_URL,
        MockMvcResultMatchers.status().is4xxClientError());
  }

  @Test
  @DisplayName("Test Invalid Login - user doesn't exist")
  void testLoginWithFakeUser() throws Exception {
    String json = getLoginJson(USERNAME + "1", PASSWORD);
    performPostRequestAndExpectStatus(json, LOGIN_URL,
        MockMvcResultMatchers.status().isBadRequest())
        .andExpect(content().string("Invalid username or password"));
  }

  @Test
  @DisplayName("Test Refresh Token")
  void testRefreshToken() throws Exception {
    String loginJson = getLoginJson(USERNAME, PASSWORD);
    MvcResult result = performPostRequestAndExpectStatus(loginJson, LOGIN_URL,
        MockMvcResultMatchers.status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();
    JwtTokenDto responseTokenDTO = new ObjectMapper().readValue(
        result.getResponse().getContentAsString(),
        JwtTokenDto.class
    );
    Assertions.assertNotNull(responseTokenDTO);
    String tokenJson = new ObjectMapper().writeValueAsString(responseTokenDTO);
    performPostRequestAndExpectStatus(tokenJson, "/api/auth/token",
        MockMvcResultMatchers.status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();
  }

  private String getLoginJson(String username, String password) throws JsonProcessingException {
    LoginDto loginDTO = new LoginDto(
        username,
        password
    );
    return new ObjectMapper().writeValueAsString(loginDTO);
  }

  private ResultActions performPostRequestAndExpectStatus(String content, String url,
                                                          ResultMatcher statusMatcher)
      throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders
            .post(url)
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(statusMatcher);
  }
}