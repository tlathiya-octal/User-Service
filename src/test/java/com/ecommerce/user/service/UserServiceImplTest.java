package com.ecommerce.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.AccountStatus;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.config.KafkaTopicsProperties;
import com.ecommerce.user.event.EventPublisher;
import com.ecommerce.user.exception.DuplicateUserException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private KafkaTopicsProperties kafkaTopicsProperties;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("users");
//        userService = new UserServiceImpl(userRepository, userMapper, cacheManager, eventPublisher, kafkaTopicsProperties);
    }

    @Test
    void createUserShouldPersistNormalizedEmail() {
        UserRequest request = new UserRequest("User@Example.com", "John", "Doe", "9999999999", null, null, null);
        User entity = new User();
        User saved = buildUser();
        UserResponse response = buildResponse(saved);

        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(response);

        UserResponse created = userService.createUser(request);

        assertThat(created.email()).isEqualTo("user@example.com");
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void createUserShouldRejectDuplicateEmail() {
        UserRequest request = new UserRequest("user@example.com", "John", "Doe", "9999999999", null, null, null);
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateUserException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByIdShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserShouldNormalizeEmailAndReturnMappedResponse() {
        UUID id = UUID.randomUUID();
        User user = buildUser();
        user.setId(id);
        user.setEmail("old@example.com");
        UpdateUserRequest request = new UpdateUserRequest(
                "New@Example.com",
                "Jane",
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );
        UserResponse response = buildResponse(user);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse updated = userService.updateUser(id, request);

        assertThat(updated.id()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        verify(userMapper).updateEntity(request, user);
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private UserResponse buildResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getProfileImage(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
