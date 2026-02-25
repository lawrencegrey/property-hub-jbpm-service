package com.greysoft.jbpm_engine.dto.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDto {
    private UUID id;
    private UUID ownerId;
    private String title;
    private String description;
    private String propertyType;
    private String status;
    private String listingType;
    private BigDecimal price;
    private String currency;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String parish;
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double squareFootage;
    private Double lotSize;
    private Integer yearBuilt;
    private String listingStatus;
    private boolean verified;
    private boolean active;
    private boolean underOffer;
    private boolean acceptingOffers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
