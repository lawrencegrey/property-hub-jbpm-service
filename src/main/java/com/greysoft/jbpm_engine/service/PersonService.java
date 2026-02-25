package com.greysoft.jbpm_engine.service;

import com.greysoft.jbpm_engine.dto.KycDto;
import com.greysoft.jbpm_engine.dto.people.AddressDto;
import com.greysoft.jbpm_engine.dto.people.PersonDto;
import com.greysoft.jbpm_engine.dto.people.PersonRoleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

    private final WebClient webClient;
    private final AuthService authService;

    public PersonService(
            @Value("${person.service.url}") String personApiUrl,
            AuthService authService) {
        this.webClient = WebClient.builder().baseUrl(personApiUrl).build();
        this.authService = authService;
    }

    private String getAuthorizationHeader() {
        return "Bearer " + authService.getAccessToken();
    }

    public Optional<PersonDto> getPersonById(UUID id) {
        try {
            logger.info("Fetching person by id: {}", id);
            PersonDto person = webClient.get()
                    .uri("/persons/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PersonDto.class)
                    .block();
            return Optional.ofNullable(person);
        } catch (Exception e) {
            logger.error("Error fetching person {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getKycByPersonId(UUID personId) {
        try {
            logger.info("Fetching KYC for person: {}", personId);
            Map<String, Object> kyc = webClient.get()
                    .uri("/persons/{id}/kyc", personId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return Optional.ofNullable(kyc);
        } catch (Exception e) {
            logger.error("Error fetching KYC for person {}: {}", personId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PersonDto> updatePerson(UUID id, PersonDto person) {
        try {
            logger.info("Updating person: {}", id);
            PersonDto updated = webClient.put()
                    .uri("/persons/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(person)
                    .retrieve()
                    .bodyToMono(PersonDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating person {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<KycDto> getKycByPersonIdAsDto(UUID personId) {
        try {
            logger.info("Fetching KYC DTO for person: {}", personId);
            KycDto kyc = webClient.get()
                    .uri("/persons/{id}/kyc", personId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(KycDto.class)
                    .block();
            return Optional.ofNullable(kyc);
        } catch (Exception e) {
            logger.error("Error fetching KYC DTO for person {}: {}", personId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Mono<KycDto> updateKyc(UUID kycId, KycDto kycDto) {
        try {
            logger.info("Updating KYC: {}", kycId);
            return Mono.justOrEmpty(
                webClient.put()
                    .uri("/persons/kyc/{id}", kycId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(kycDto)
                    .retrieve()
                    .bodyToMono(KycDto.class)
                    .block()
            );
        } catch (Exception e) {
            logger.error("Error updating KYC {}: {}", kycId, e.getMessage(), e);
            return Mono.error(e);
        }
    }

    public Optional<PersonRoleDto> getPersonRoleById(UUID personRoleId) {
        try {
            logger.info("Fetching person role by id: {}", personRoleId);
            PersonRoleDto role = webClient.get()
                    .uri("/persons/roles/{id}", personRoleId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PersonRoleDto.class)
                    .block();
            return Optional.ofNullable(role);
        } catch (Exception e) {
            logger.error("Error fetching person role {}: {}", personRoleId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<PersonRoleDto> updatePersonRole(UUID personRoleId, PersonRoleDto personRole) {
        try {
            logger.info("Updating person role: {}", personRoleId);
            PersonRoleDto updated = webClient.put()
                    .uri("/persons/roles/{id}", personRoleId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(personRole)
                    .retrieve()
                    .bodyToMono(PersonRoleDto.class)
                    .block();
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            logger.error("Error updating person role {}: {}", personRoleId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean deletePersonRole(UUID personRoleId) {
        try {
            logger.info("Deleting person role: {}", personRoleId);
            webClient.delete()
                    .uri("/persons/roles/{id}", personRoleId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting person role {}: {}", personRoleId, e.getMessage(), e);
            return false;
        }
    }

    public Optional<PersonRoleDto> verifyPersonRole(UUID personRoleId) {
        try {
            logger.info("Verifying person role: {}", personRoleId);
            PersonRoleDto result = webClient.put()
                    .uri("/persons/roles/{id}/verify", personRoleId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .bodyToMono(PersonRoleDto.class)
                    .block();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            logger.error("Error verifying person role {}: {}", personRoleId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<AddressDto> getAddressById(UUID addressId) {
        try {
            logger.info("Fetching address by id: {}", addressId);
            AddressDto address = webClient.get()
                    .uri("/persons/addresses/{id}", addressId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(AddressDto.class)
                    .block();
            return Optional.ofNullable(address);
        } catch (Exception e) {
            logger.error("Error fetching address {}: {}", addressId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<AddressDto> getAddressByDocumentId(UUID documentId) {
        try {
            logger.info("Fetching address by documentId: {}", documentId);
            AddressDto address = webClient.get()
                    .uri("/persons/addresses/document/{documentId}", documentId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(AddressDto.class)
                    .block();
            return Optional.ofNullable(address);
        } catch (Exception e) {
            logger.error("Error fetching address by documentId {}: {}", documentId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean deleteAddress(UUID addressId) {
        try {
            logger.info("Deleting address: {}", addressId);
            webClient.delete()
                    .uri("/persons/addresses/{id}", addressId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error deleting address {}: {}", addressId, e.getMessage(), e);
            return false;
        }
    }

    public boolean verifyAddress(UUID addressId) {
        try {
            logger.info("Verifying address: {}", addressId);
            webClient.put()
                    .uri("/persons/addresses/{id}/verify", addressId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Error verifying address {}: {}", addressId, e.getMessage(), e);
            return false;
        }
    }

    public void deleteAllPersonData(UUID personId) {
        try {
            logger.info("Deleting all data for person: {}", personId);
            webClient.delete()
                    .uri("/persons/{id}/all", personId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            logger.info("Successfully deleted all data for person: {}", personId);
        } catch (Exception e) {
            logger.error("Error deleting all data for person {}: {}", personId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete all person data for " + personId, e);
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
            logger.warn("[isServiceUp] Person API health check failed: {}", e.getMessage());
            return false;
        }
    }
}
