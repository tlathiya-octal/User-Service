package com.ecommerce.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.user.config.SecurityConfig;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.AccountStatus;
import com.ecommerce.user.exception.GlobalExceptionHandler;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void createUserShouldReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(
                userId,
                "user@example.com",
                "John",
                "Doe",
                "9999999999",
                null,
                null,
                null,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                null,               // userId — optional, omitted by external callers
                                "user@example.com",
                                "John",
                                "Doe",
                                "9999999999",
                                null,
                                null,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void getCurrentUserShouldResolveUserFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(
                userId,
                "user@example.com",
                "John",
                "Doe",
                "9999999999",
                null,
                null,
                null,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(jwtUtil.extractUserId(any())).thenReturn(userId);
        when(userService.getUserById(eq(userId))).thenReturn(response);

        mockMvc.perform(get("/users/me").with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}
