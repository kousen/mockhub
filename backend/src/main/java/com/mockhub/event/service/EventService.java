package com.mockhub.event.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.util.SlugUtil;
import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.dto.TagDto;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.entity.Tag;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.repository.TagRepository;
import com.mockhub.event.specification.EventSpecification;
import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.service.PriceHistoryService;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.service.ListingService;
import com.mockhub.ticket.service.TicketService;
import com.mockhub.venue.dto.SectionAvailabilityDto;
import com.mockhub.venue.dto.VenueSummaryDto;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

@Service
public class EventService {

    private static final String SORT_EVENT_DATE = "eventDate";

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final VenueRepository venueRepository;
    private final ListingService listingService;
    private final PriceHistoryService priceHistoryService;
    private final TicketService ticketService;

    public EventService(EventRepository eventRepository,
                        CategoryRepository categoryRepository,
                        TagRepository tagRepository,
                        VenueRepository venueRepository,
                        ListingService listingService,
                        PriceHistoryService priceHistoryService,
                        TicketService ticketService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.venueRepository = venueRepository;
        this.listingService = listingService;
        this.priceHistoryService = priceHistoryService;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "events", key = "#slug")
    public EventDto getBySlug(String slug) {
        Event event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", slug));
        return toEventDto(event);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EventSummaryDto> listEvents(EventSearchRequest request) {
        Specification<Event> spec = buildSpecification(request);
        Sort sort = buildSort(request.sort());
        Pageable pageable = PageRequest.of(request.page(), request.size(), sort);

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        List<EventSummaryDto> content = eventPage.getContent().stream()
                .map(this::toEventSummaryDto)
                .toList();

        return new PagedResponse<>(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "featuredEvents")
    public List<EventSummaryDto> listFeatured() {
        List<Event> featured = eventRepository.findFeaturedEvents();
        return featured.stream()
                .map(this::toEventSummaryDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"events", "featuredEvents"}, allEntries = true)
    public EventDto createEvent(EventCreateRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.venueId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));

        Event event = new Event();
        event.setName(request.name());
        event.setSlug(SlugUtil.toSlug(request.name()));
        event.setDescription(request.description());
        event.setArtistName(request.artistName());
        event.setEventDate(request.eventDate());
        event.setDoorsOpenAt(request.doorsOpenAt());
        event.setBasePrice(request.basePrice());
        event.setMinPrice(request.basePrice());
        event.setMaxPrice(request.basePrice());
        event.setStatus("ACTIVE");
        event.setTotalTickets(0);
        event.setAvailableTickets(0);
        event.setFeatured(request.isFeatured() != null && request.isFeatured());
        event.setSpotifyArtistId(request.spotifyArtistId());
        event.setVenue(venue);
        event.setCategory(category);

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            event.setTags(new HashSet<>(tags));
        }

        Event savedEvent = eventRepository.save(event);
        return toEventDto(savedEvent);
    }

    @Transactional
    @CacheEvict(value = {"events", "featuredEvents"}, allEntries = true)
    public EventDto updateEvent(Long id, EventCreateRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        if (request.name() != null) {
            event.setName(request.name());
            event.setSlug(SlugUtil.toSlug(request.name()));
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if (request.artistName() != null) {
            event.setArtistName(request.artistName());
        }
        if (request.eventDate() != null) {
            event.setEventDate(request.eventDate());
        }
        if (request.doorsOpenAt() != null) {
            event.setDoorsOpenAt(request.doorsOpenAt());
        }
        if (request.basePrice() != null) {
            event.setBasePrice(request.basePrice());
        }
        if (request.isFeatured() != null) {
            event.setFeatured(request.isFeatured());
        }
        if (request.spotifyArtistId() != null) {
            event.setSpotifyArtistId(request.spotifyArtistId());
        }

        if (request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.venueId()));
            event.setVenue(venue);
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));
            event.setCategory(category);
        }

        if (request.tagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            event.setTags(new HashSet<>(tags));
        }

        Event savedEvent = eventRepository.save(event);
        return toEventDto(savedEvent);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tags")
    public List<TagDto> listTags() {
        return tagRepository.findAll(Sort.by("name").ascending()).stream()
                .map(this::toTagDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getActiveListingsByEventSlug(String eventSlug) {
        return listingService.getActiveListingsByEventSlug(eventSlug);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryDto> getPriceHistoryByEventSlug(String eventSlug) {
        return priceHistoryService.getByEventSlug(eventSlug);
    }

    @Transactional(readOnly = true)
    public List<SectionAvailabilityDto> getSectionAvailability(String eventSlug) {
        return ticketService.getSectionAvailability(eventSlug);
    }

    private Specification<Event> buildSpecification(EventSearchRequest request) {
        Specification<Event> spec = Specification.where(
                (Specification<Event>) (root, query, cb) -> cb.conjunction());

        if (request.status() != null && !request.status().isBlank()) {
            spec = spec.and(EventSpecification.hasStatus(request.status()));
        }

        if (request.q() != null && !request.q().isBlank()) {
            spec = spec.and(EventSpecification.nameOrArtistContains(request.q()));
        }

        if (request.category() != null && !request.category().isBlank()) {
            spec = spec.and(EventSpecification.hasCategory(request.category()));
        }

        if (request.tags() != null && !request.tags().isBlank()) {
            List<String> tagSlugs = Arrays.asList(request.tags().split(","));
            spec = spec.and(EventSpecification.hasTags(tagSlugs));
        }

        if (request.city() != null && !request.city().isBlank()) {
            spec = spec.and(EventSpecification.inCity(request.city()));
        }

        if (request.dateFrom() != null) {
            spec = spec.and(EventSpecification.eventDateAfter(request.dateFrom()));
        }

        if (request.dateTo() != null) {
            spec = spec.and(EventSpecification.eventDateBefore(request.dateTo()));
        }

        if (request.minPrice() != null) {
            spec = spec.and(EventSpecification.minPriceGreaterThanOrEqual(request.minPrice()));
        }

        if (request.maxPrice() != null) {
            spec = spec.and(EventSpecification.maxPriceLessThanOrEqual(request.maxPrice()));
        }

        return spec;
    }

    private Sort buildSort(String sortParam) {
        Sort primarySort;
        if (sortParam == null || sortParam.isBlank()) {
            primarySort = Sort.by(SORT_EVENT_DATE).ascending();
        } else {
            primarySort = switch (sortParam) {
                case "date", SORT_EVENT_DATE -> Sort.by(SORT_EVENT_DATE).ascending();
                case "dateDesc" -> Sort.by(SORT_EVENT_DATE).descending();
                case "price" -> Sort.by("minPrice").ascending();
                case "priceDesc" -> Sort.by("minPrice").descending();
                case "name" -> Sort.by("name").ascending();
                default -> Sort.by(SORT_EVENT_DATE).ascending();
            };
        }
        return primarySort.and(Sort.by("id").ascending());
    }

    private EventDto toEventDto(Event event) {
        Venue venue = event.getVenue();
        VenueSummaryDto venueSummary = new VenueSummaryDto(
                venue.getId(),
                venue.getName(),
                venue.getSlug(),
                venue.getCity(),
                venue.getState(),
                venue.getVenueType(),
                venue.getCapacity(),
                venue.getImageUrl()
        );

        CategoryDto categoryDto = toCategoryDto(event.getCategory());

        List<TagDto> tagDtos = event.getTags().stream()
                .map(this::toTagDto)
                .toList();

        return new EventDto(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getDescription(),
                event.getArtistName(),
                event.getEventDate(),
                event.getDoorsOpenAt(),
                event.getStatus(),
                event.getBasePrice(),
                event.getMinPrice(),
                event.getMaxPrice(),
                event.getTotalTickets(),
                event.getAvailableTickets(),
                event.isFeatured(),
                venueSummary,
                categoryDto,
                tagDtos,
                event.getPrimaryImageUrl(),
                event.getSpotifyArtistId()
        );
    }

    private EventSummaryDto toEventSummaryDto(Event event) {
        Venue venue = event.getVenue();
        return new EventSummaryDto(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getArtistName(),
                venue.getName(),
                venue.getCity(),
                event.getEventDate(),
                event.getMinPrice(),
                event.getAvailableTickets(),
                event.getPrimaryImageUrl(),
                event.getCategory().getName(),
                event.isFeatured()
        );
    }

    private CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getIcon(),
                category.getSortOrder()
        );
    }

    private TagDto toTagDto(Tag tag) {
        return new TagDto(
                tag.getId(),
                tag.getName(),
                tag.getSlug()
        );
    }
}
