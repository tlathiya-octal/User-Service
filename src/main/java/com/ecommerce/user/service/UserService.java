package com.ecommerce.user.service;

import com.ecommerce.user.dto.PagedResponse;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUserById(UUID id);

    UserResponse getUserByEmail(String email);

    PagedResponse<UserResponse> listUsers(int page, int size);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);

    List<AddressResponse> getUserAddresses(UUID userId);
    
    AddressResponse addAddress(UUID userId, AddressRequest request);
    
    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);
    
    void deleteAddress(UUID userId, UUID addressId);
}
