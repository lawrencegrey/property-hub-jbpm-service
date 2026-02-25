package com.greysoft.jbpm_engine.service;

import com.greysoft.jbpm_engine.dto.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PropertyService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyService.class);

    private final WebClient webClient;
    private final AuthService authService;

    public PropertyService(
            @Value("${property.service.url}") String propertyApiUrl,
            AuthService authService) {
        this.webClient = WebClient.builder().baseUrl(propertyApiUrl).build();
        this.authService = authService;
    }

    private String getAuthorizationHeader() {
        return "Bearer " + authService.getAccessToken();
    }

    public Optional<PropertyDto> getPropertyById(UUID id) {
        try {
            logger.info("Fetching property by id: {}", id);
            PropertyDto property = webClient.get()
                    .uri("/properties/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PropertyDto.class)
                    .block();
            return Optional.ofNullable(property);
        } catch (Exception e) {
            logger.error("Error fetching property {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PropertyDto> updateProperty(UUID id, PropertyDto property) {
        try {
            logger.info("Updating property: {}", id);
            PropertyDto updated = webClient.put()
                    .uri("/properties/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(property)
                    .retrieve()
                    .bodyToMono(PropertyDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating property {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean deletePropertyAndAllRelatedData(UUID propertyId) {
        try {
            logger.info("Deleting property and all related data: {}", propertyId);
            webClient.delete()
                    .uri("/properties/{id}", propertyId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting property {}: {}", propertyId, e.getMessage(), e);
            return false;
        }
    }

    public PropertyDto updateOfferStatus(UUID propertyId, boolean underOffer, boolean acceptingOffers) {
        try {
            logger.info("Updating offer status for property: {}, underOffer={}, acceptingOffers={}", propertyId, underOffer, acceptingOffers);
            Optional<PropertyDto> propertyOpt = getPropertyById(propertyId);
            if (propertyOpt.isEmpty()) {
                throw new RuntimeException("Property not found: " + propertyId);
            }
            PropertyDto property = propertyOpt.get();
            property.setUnderOffer(underOffer);
            property.setAcceptingOffers(acceptingOffers);
            return updateProperty(propertyId, property).orElseThrow(() -> new RuntimeException("Failed to update property"));
        } catch (Exception e) {
            logger.error("Error updating offer status for property {}: {}", propertyId, e.getMessage(), e);
            throw new RuntimeException("Failed to update offer status", e);
        }
    }

    // ========== Offer Methods ==========

    public Optional<OfferDto> addOffer(OfferDto offerDto) {
        try {
            logger.info("Creating offer for propertyId: {}", offerDto.getPropertyId());
            OfferDto created = webClient.post()
                    .uri("/properties/offers")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(offerDto)
                    .retrieve()
                    .bodyToMono(OfferDto.class)
                    .block();
            return Optional.ofNullable(created);
        } catch (Exception e) {
            logger.error("Error creating offer: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<OfferDto> getOfferById(UUID offerId) {
        try {
            logger.info("Fetching offer by id: {}", offerId);
            OfferDto offer = webClient.get()
                    .uri("/properties/offers/{id}", offerId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(OfferDto.class)
                    .block();
            return Optional.ofNullable(offer);
        } catch (Exception e) {
            logger.error("Error fetching offer {}: {}", offerId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<OfferDto> updateOffer(UUID offerId, OfferDto offerDto) {
        try {
            logger.info("Updating offer: {}", offerId);
            OfferDto updated = webClient.put()
                    .uri("/properties/offers/{id}", offerId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(offerDto)
                    .retrieve()
                    .bodyToMono(OfferDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating offer {}: {}", offerId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean deleteOffer(UUID offerId) {
        try {
            logger.info("Deleting offer: {}", offerId);
            webClient.delete()
                    .uri("/properties/offers/{id}", offerId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting offer {}: {}", offerId, e.getMessage(), e);
            return false;
        }
    }

    // ========== Engagement Methods ==========

    public EngagementDto createEngagement(EngagementDto engagementDto) {
        try {
            logger.info("Creating engagement for propertyId: {}", engagementDto.getPropertyId());
            EngagementDto created = webClient.post()
                    .uri("/properties/engagements")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(engagementDto)
                    .retrieve()
                    .bodyToMono(EngagementDto.class)
                    .block();
            return created;
        } catch (Exception e) {
            logger.error("Error creating engagement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create engagement", e);
        }
    }

    // ========== Engagement Party Methods ==========

    public EngagementPartyDto createEngagementParty(EngagementPartyDto engagementPartyDto) {
        try {
            logger.info("Creating engagement party for engagementId: {}", engagementPartyDto.getEngagementId());
            EngagementPartyDto created = webClient.post()
                    .uri("/properties/engagement-parties")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(engagementPartyDto)
                    .retrieve()
                    .bodyToMono(EngagementPartyDto.class)
                    .block();
            return created;
        } catch (Exception e) {
            logger.error("Error creating engagement party: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create engagement party", e);
        }
    }

    public Optional<EngagementPartyDto> getEngagementPartyById(UUID engagementPartyId) {
        try {
            logger.info("Fetching engagement party: {}", engagementPartyId);
            EngagementPartyDto party = webClient.get()
                    .uri("/properties/engagement-parties/{id}", engagementPartyId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(EngagementPartyDto.class)
                    .block();
            return Optional.ofNullable(party);
        } catch (Exception e) {
            logger.error("Error fetching engagement party {}: {}", engagementPartyId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<EngagementPartyDto> updateEngagementParty(UUID engagementPartyId, EngagementPartyDto engagementParty) {
        try {
            logger.info("Updating engagement party: {}", engagementPartyId);
            EngagementPartyDto updated = webClient.put()
                    .uri("/properties/engagement-parties/{id}", engagementPartyId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(engagementParty)
                    .retrieve()
                    .bodyToMono(EngagementPartyDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating engagement party {}: {}", engagementPartyId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean deleteEngagementParty(UUID engagementPartyId) {
        try {
            logger.info("Deleting engagement party: {}", engagementPartyId);
            webClient.delete()
                    .uri("/properties/engagement-parties/{id}", engagementPartyId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting engagement party {}: {}", engagementPartyId, e.getMessage(), e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<EngagementPartyDto> getEngagementPartiesByEngagementId(UUID engagementId) {
        try {
            logger.info("Fetching engagement parties for engagementId: {}", engagementId);
            List<EngagementPartyDto> parties = webClient.get()
                    .uri("/properties/engagements/{id}/parties", engagementId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EngagementPartyDto>>() {})
                    .block();
            return parties != null ? parties : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching engagement parties for engagement {}: {}", engagementId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ========== Rental/Sale/Lease Methods ==========

    public Optional<PropertyRentalDto> getPropertyRentalById(UUID rentalId) {
        try {
            logger.info("Fetching property rental: {}", rentalId);
            PropertyRentalDto rental = webClient.get()
                    .uri("/properties/rentals/{id}", rentalId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PropertyRentalDto.class)
                    .block();
            return Optional.ofNullable(rental);
        } catch (Exception e) {
            logger.error("Error fetching rental {}: {}", rentalId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PropertyRentalDto> updatePropertyRental(UUID rentalId, PropertyRentalDto rental) {
        try {
            logger.info("Updating property rental: {}", rentalId);
            PropertyRentalDto updated = webClient.put()
                    .uri("/properties/rentals/{id}", rentalId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(rental)
                    .retrieve()
                    .bodyToMono(PropertyRentalDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating rental {}: {}", rentalId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PropertySalesDto> getPropertySaleById(UUID saleId) {
        try {
            logger.info("Fetching property sale: {}", saleId);
            PropertySalesDto sale = webClient.get()
                    .uri("/properties/sales/{id}", saleId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PropertySalesDto.class)
                    .block();
            return Optional.ofNullable(sale);
        } catch (Exception e) {
            logger.error("Error fetching sale {}: {}", saleId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PropertySalesDto> updatePropertySale(UUID saleId, PropertySalesDto sale) {
        try {
            logger.info("Updating property sale: {}", saleId);
            PropertySalesDto updated = webClient.put()
                    .uri("/properties/sales/{id}", saleId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(sale)
                    .retrieve()
                    .bodyToMono(PropertySalesDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating sale {}: {}", saleId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PropertyLeaseDto> getPropertyLeaseById(UUID leaseId) {
        try {
            logger.info("Fetching property lease: {}", leaseId);
            PropertyLeaseDto lease = webClient.get()
                    .uri("/properties/leases/{id}", leaseId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PropertyLeaseDto.class)
                    .block();
            return Optional.ofNullable(lease);
        } catch (Exception e) {
            logger.error("Error fetching lease {}: {}", leaseId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PropertyLeaseDto> updatePropertyLease(UUID leaseId, PropertyLeaseDto lease) {
        try {
            logger.info("Updating property lease: {}", leaseId);
            PropertyLeaseDto updated = webClient.put()
                    .uri("/properties/leases/{id}", leaseId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(lease)
                    .retrieve()
                    .bodyToMono(PropertyLeaseDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating lease {}: {}", leaseId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean isServiceUp() {
        try {
            webClient.get()
                    .uri("/health")
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.warn("[isServiceUp] Property API health check failed: {}", e.getMessage());
            return false;
        }
    }
}
