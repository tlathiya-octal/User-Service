package com.ecommerce.user.controller;

import com.ecommerce.user.dto.PagedResponse;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.service.UserService;
import java.util.List;
import com.ecommerce.user.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "User profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a user profile")
    @ApiResponse(responseCode = "201", description = "User created")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping
    @Operation(summary = "List users with pagination")
    public ResponseEntity<PagedResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.listUsers(page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestHeader("X-User-Id") UUID userId,
                                                          @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @GetMapping("/me/addresses")
    @Operation(summary = "List user addresses")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userService.getUserAddresses(userId));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Add an address")
    public ResponseEntity<AddressResponse> addAddress(@RequestHeader("X-User-Id") UUID userId,
                                                      @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(userId, request));
    }

    @PutMapping("/me/addresses/{id}")
    @Operation(summary = "Update an address")
    public ResponseEntity<AddressResponse> updateAddress(@RequestHeader("X-User-Id") UUID userId,
                                                         @PathVariable UUID id,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(userId, id, request));
    }

    @DeleteMapping("/me/addresses/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<Void> deleteAddress(@RequestHeader("X-User-Id") UUID userId,
                                              @PathVariable UUID id) {
        userService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user profile")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a user profile")
    @ApiResponse(responseCode = "204", description = "User deleted")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
