package com.mockhub.venue.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.venue.dto.SectionDto;
import com.mockhub.venue.dto.VenueDto;
import com.mockhub.venue.dto.VenueSummaryDto;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "venues", key = "#slug")
    public VenueDto getBySlug(String slug) {
        Venue venue = venueRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "slug", slug));
        return toVenueDto(venue);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VenueSummaryDto> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Venue> venuePage = venueRepository.findAll(pageable);

        List<VenueSummaryDto> content = venuePage.getContent().stream()
                .map(this::toVenueSummaryDto)
                .toList();

        return new PagedResponse<>(
                content,
                venuePage.getNumber(),
                venuePage.getSize(),
                venuePage.getTotalElements(),
                venuePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<VenueSummaryDto> getByCity(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Venue> venuePage = venueRepository.findByCity(city, pageable);

        List<VenueSummaryDto> content = venuePage.getContent().stream()
                .map(this::toVenueSummaryDto)
                .toList();

        return new PagedResponse<>(
                content,
                venuePage.getNumber(),
                venuePage.getSize(),
                venuePage.getTotalElements(),
                venuePage.getTotalPages()
        );
    }

    private VenueDto toVenueDto(Venue venue) {
        List<SectionDto> sectionDtos = venue.getSections().stream()
                .map(this::toSectionDto)
                .toList();

        return new VenueDto(
                venue.getId(),
                venue.getName(),
                venue.getSlug(),
                venue.getAddressLine1(),
                venue.getAddressLine2(),
                venue.getCity(),
                venue.getState(),
                venue.getZipCode(),
                venue.getCountry(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getCapacity(),
                venue.getVenueType(),
                venue.getImageUrl(),
                venue.getSvgMapUrl(),
                sectionDtos
        );
    }

    private VenueSummaryDto toVenueSummaryDto(Venue venue) {
        return new VenueSummaryDto(
                venue.getId(),
                venue.getName(),
                venue.getSlug(),
                venue.getCity(),
                venue.getState(),
                venue.getVenueType(),
                venue.getCapacity(),
                venue.getImageUrl()
        );
    }

    private SectionDto toSectionDto(Section section) {
        int totalRows = section.getSeatRows().size();
        int totalSeats = section.getSeatRows().stream()
                .mapToInt(row -> row.getSeats().size())
                .sum();

        return new SectionDto(
                section.getId(),
                section.getName(),
                section.getSectionType(),
                section.getCapacity(),
                section.getSortOrder(),
                section.getSvgPathId(),
                section.getSvgX(),
                section.getSvgY(),
                section.getSvgWidth(),
                section.getSvgHeight(),
                section.getColorHex(),
                totalRows,
                totalSeats
        );
    }
}
