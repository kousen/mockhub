package com.mockhub.venue.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.venue.dto.VenueDto;
import com.mockhub.venue.dto.VenueSummaryDto;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    private Venue testVenue;

    @BeforeEach
    void setUp() {
        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Madison Square Garden");
        testVenue.setSlug("madison-square-garden");
        testVenue.setAddressLine1("4 Pennsylvania Plaza");
        testVenue.setCity("New York");
        testVenue.setState("NY");
        testVenue.setZipCode("10001");
        testVenue.setCountry("US");
        testVenue.setCapacity(20000);
        testVenue.setVenueType("ARENA");
        testVenue.setSections(new ArrayList<>());
    }

    @Test
    @DisplayName("getBySlug - given existing slug - returns venue DTO with sections")
    void getBySlug_givenExistingSlug_returnsVenueDto() {
        when(venueRepository.findBySlug("madison-square-garden"))
                .thenReturn(Optional.of(testVenue));

        VenueDto result = venueService.getBySlug("madison-square-garden");

        assertNotNull(result, "Venue DTO should not be null");
        assertEquals("Madison Square Garden", result.name(), "Venue name should match");
        assertEquals("New York", result.city(), "City should match");
        assertEquals(20000, result.capacity(), "Capacity should match");
    }

    @Test
    @DisplayName("getBySlug - given nonexistent slug - throws ResourceNotFoundException")
    void getBySlug_givenNonexistentSlug_throwsResourceNotFoundException() {
        when(venueRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> venueService.getBySlug("nonexistent"),
                "Should throw ResourceNotFoundException for unknown slug");
    }

    @Test
    @DisplayName("listAll - given venues exist - returns paged response")
    void listAll_givenVenuesExist_returnsPagedResponse() {
        Page<Venue> page = new PageImpl<>(List.of(testVenue));
        when(venueRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<VenueSummaryDto> result = venueService.listAll(0, 20);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one venue");
        assertEquals("Madison Square Garden", result.content().get(0).name(),
                "Venue name should match");
    }

    @Test
    @DisplayName("getByCity - given city with venues - returns filtered paged response")
    void getByCity_givenCityWithVenues_returnsFilteredPagedResponse() {
        Page<Venue> page = new PageImpl<>(List.of(testVenue));
        when(venueRepository.findByCity(eq("New York"), any(Pageable.class))).thenReturn(page);

        PagedResponse<VenueSummaryDto> result = venueService.getByCity("New York", 0, 20);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one venue");
    }
}
