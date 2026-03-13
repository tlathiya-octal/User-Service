package com.ecommerce.user.dto;

import java.util.UUID;

public record AddressResponse(
    UUID id,
    String street,
    String city,
    String state,
    String zipCode,
    String country
) {}
