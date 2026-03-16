package com.ecommerce.user.service.impl;

//import com.ecommerce.events.UserCreatedEvent;
import com.ecommerce.user.dto.PagedResponse;
import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.AccountStatus;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.DuplicateUserException;
import com.ecommerce.user.exception.DuplicateUserException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.entity.Address;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USERS_CACHE = "users";
    private static final String DEFAULT_FIRST_NAME = "New";
    private static final String DEFAULT_LAST_NAME = "User";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateUserException("User already exists with email: " + normalizedEmail);
        }
        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateUserException("User already exists with phone number: " + request.phoneNumber());
        }

        User user = userMapper.toEntity(request);
        user.setEmail(normalizedEmail);
        user.setAccountStatus(AccountStatus.ACTIVE);

        // REST-mode: when auth-service supplies a userId, persist the profile with
        // the same primary key so both databases are aligned without a mapping table.
        if (request.userId() != null) {
            user.setId(request.userId());
        }

        User savedUser = userRepository.save(user);

        UserResponse response = userMapper.toResponse(savedUser);
        cacheById(response);
        cacheByEmail(response);
        log.info("Created user profile for email={}", normalizedEmail);
        return response;
    }

    @Override
    @Cacheable(value = USERS_CACHE, key = "'user:' + #id")
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        UserResponse response = userMapper.toResponse(findUser(id));
        cacheByEmail(response);
        return response;
    }

    @Override
    @Cacheable(value = USERS_CACHE, key = "'email:' + #email.toLowerCase()")
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + normalizedEmail));
        UserResponse response = userMapper.toResponse(user);
        cacheById(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserResponse> userPage = userRepository.findAllByAccountStatusIsNotNull(pageable)
                .map(userMapper::toResponse);
        return new PagedResponse<>(
                userPage.getContent(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    @Override
    @Transactional
    @CachePut(value = USERS_CACHE, key = "'user:' + #id")
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        String previousEmail = user.getEmail();

        if (request.email() != null) {
            String normalizedEmail = normalizeEmail(request.email());
            if (!normalizedEmail.equalsIgnoreCase(previousEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new DuplicateUserException("User already exists with email: " + normalizedEmail);
            }
            user.setEmail(normalizedEmail);
        }

        userMapper.updateEntity(request, user);
        User savedUser = userRepository.save(user);
        UserResponse response = userMapper.toResponse(savedUser);

        evictEmail(previousEmail);
        cacheByEmail(response);
        log.info("Updated user profile id={}", id);
        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = USERS_CACHE, key = "'user:' + #id")
    public void deleteUser(UUID id) {
        User user = findUser(id);
        userRepository.delete(user);
        evictEmail(user.getEmail());
        log.info("Soft deleted user profile id={}", id);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found for id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        User user = findUser(userId);
        Address address = Address.builder()
                .user(user)
                .street(request.street())
                .city(request.city())
                .state(request.state())
                .zipCode(request.zipCode())
                .country(request.country())
                .build();
        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Address doesn't belong to user");
        }
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setZipCode(request.zipCode());
        address.setCountry(request.country());
        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Address doesn't belong to user");
        }
        addressRepository.delete(address);
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry()
        );
    }

    private void cacheById(UserResponse response) {
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.put("user:" + response.id(), response);
        }
    }

    private void cacheByEmail(UserResponse response) {
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.put("email:" + normalizeEmail(response.email()), response);
        }
    }

    private void evictEmail(String email) {
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.evict("email:" + normalizeEmail(email));
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
