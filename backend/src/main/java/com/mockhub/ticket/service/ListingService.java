package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.dto.EarningsSummaryDto;
import com.mockhub.ticket.dto.ListingCreateRequest;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.dto.SaleDto;
import com.mockhub.ticket.dto.SellListingRequest;
import com.mockhub.ticket.dto.SellerListingDto;
import com.mockhub.ticket.dto.TicketSearchResultDto;
import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.dto.UpdatePriceRequest;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.ticket.specification.ListingSearchSpecification;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SOLD = "SOLD";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String TICKET_AVAILABLE = "AVAILABLE";
    private static final String TICKET_LISTED = "LISTED";
    private static final String LISTING_RESOURCE = "Listing";

    private final ListingRepository listingRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ListingService(ListingRepository listingRepository,
                          TicketRepository ticketRepository,
                          EventRepository eventRepository,
                          UserRepository userRepository,
                          OrderItemRepository orderItemRepository) {
        this.listingRepository = listingRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getActiveListingsByEventSlug(String eventSlug) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));

        List<Listing> listings = listingRepository.findByEventIdAndStatus(event.getId(), STATUS_ACTIVE);
        return listings.stream()
                .map(this::toListingDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getActiveListingsByEventSlugPaginated(String eventSlug, int page, int size) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));

        List<Listing> listings = listingRepository.findByEventIdAndStatusOrderByPrice(
                event.getId(), STATUS_ACTIVE, PageRequest.of(page, size));
        return listings.stream()
                .map(this::toListingDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countActiveListingsByEventSlug(String eventSlug) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));
        return listingRepository.countByEventIdAndStatus(event.getId(), STATUS_ACTIVE);
    }

    @Transactional(readOnly = true)
    public ListingDto getListingById(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(LISTING_RESOURCE, "id", listingId));
        return toListingDto(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getActiveListingsByEventId(Long eventId) {
        List<Listing> listings = listingRepository.findByEventIdAndStatus(eventId, STATUS_ACTIVE);
        return listings.stream()
                .map(this::toListingDto)
                .toList();
    }

    @Transactional
    public ListingDto createListing(ListingCreateRequest request) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", request.ticketId()));

        if (!TICKET_AVAILABLE.equals(ticket.getStatus())) {
            throw new ConflictException("Ticket is not available for listing");
        }

        ticket.setStatus(TICKET_LISTED);
        ticketRepository.save(ticket);

        Listing listing = new Listing();
        listing.setTicket(ticket);
        listing.setEvent(ticket.getEvent());
        listing.setListedPrice(request.listedPrice());
        listing.setComputedPrice(request.listedPrice());
        listing.setPriceMultiplier(BigDecimal.ONE);
        listing.setStatus(STATUS_ACTIVE);
        listing.setListedAt(Instant.now());
        listing.setExpiresAt(request.expiresAt());

        Listing saved = listingRepository.save(listing);
        log.info("Created listing {} for ticket {}", saved.getId(), ticket.getId());
        return toListingDto(saved);
    }

    @Transactional
    public void updateListingPrices(Long eventId, BigDecimal multiplier) {
        List<Listing> activeListings = listingRepository.findByEventIdAndStatus(eventId, STATUS_ACTIVE);

        for (Listing listing : activeListings) {
            BigDecimal computedPrice = listing.getListedPrice().multiply(multiplier);
            listing.setComputedPrice(computedPrice);
            listing.setPriceMultiplier(multiplier);
        }

        listingRepository.saveAll(activeListings);
        log.debug("Updated {} listing prices for event {}", activeListings.size(), eventId);
    }

    @Transactional(readOnly = true)
    public BigDecimal[] getComputedPriceRange(Long eventId) {
        List<Listing> activeListings = listingRepository.findByEventIdAndStatus(eventId, STATUS_ACTIVE);
        BigDecimal min = null;
        BigDecimal max = null;
        for (Listing listing : activeListings) {
            BigDecimal price = listing.getComputedPrice();
            if (price != null) {
                if (min == null || price.compareTo(min) < 0) {
                    min = price;
                }
                if (max == null || price.compareTo(max) > 0) {
                    max = price;
                }
            }
        }
        return new BigDecimal[]{min, max};
    }

    // -- Seller flow methods --

    @Transactional
    public SellerListingDto createSellerListing(String userEmail,
                                                SellListingRequest request) {
        User seller = resolveUser(userEmail);
        Event event = eventRepository.findBySlug(request.eventSlug())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event", "slug", request.eventSlug()));

        Ticket ticket = ticketRepository.findByEventIdAndSectionAndRowAndSeat(
                        event.getId(),
                        request.sectionName(),
                        request.rowLabel(),
                        request.seatNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket",
                        "seat",
                        request.sectionName() + "/" + request.rowLabel()
                                + "/" + request.seatNumber()));

        if (!TICKET_AVAILABLE.equals(ticket.getStatus())) {
            throw new ConflictException("Ticket is not available for listing");
        }

        if (listingRepository.existsByTicketIdAndStatus(ticket.getId(), STATUS_ACTIVE)) {
            throw new ConflictException(
                    "An active listing already exists for this ticket");
        }

        ticket.setStatus(TICKET_LISTED);
        ticketRepository.save(ticket);

        Listing listing = new Listing();
        listing.setTicket(ticket);
        listing.setEvent(event);
        listing.setSeller(seller);
        listing.setListedPrice(request.price());
        listing.setComputedPrice(request.price());
        listing.setPriceMultiplier(BigDecimal.ONE);
        listing.setStatus(STATUS_ACTIVE);
        listing.setListedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        log.info("Seller {} created listing {} for ticket {}",
                seller.getEmail(), saved.getId(), ticket.getId());
        return toSellerListingDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SellerListingDto> getSellerListings(String userEmail,
                                                     String statusFilter) {
        User seller = resolveUser(userEmail);
        List<Listing> listings;

        if (statusFilter != null && !statusFilter.isBlank()) {
            listings = listingRepository.findBySellerIdAndStatus(
                    seller.getId(), statusFilter.toUpperCase());
        } else {
            listings = listingRepository.findBySellerId(seller.getId());
        }

        return listings.stream()
                .map(this::toSellerListingDto)
                .toList();
    }

    @Transactional
    public SellerListingDto updateListingPrice(Long listingId,
                                                String userEmail,
                                                UpdatePriceRequest request) {
        User seller = resolveUser(userEmail);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        LISTING_RESOURCE, "id", listingId));

        verifyOwnership(listing, seller);

        if (!STATUS_ACTIVE.equals(listing.getStatus())) {
            throw new ConflictException(
                    "Can only update price of active listings");
        }

        listing.setListedPrice(request.price());
        listing.setComputedPrice(
                request.price().multiply(listing.getPriceMultiplier()));
        Listing saved = listingRepository.save(listing);

        log.info("Seller {} updated listing {} price to {}",
                seller.getEmail(), listingId, request.price());
        return toSellerListingDto(saved);
    }

    @Transactional
    public void deactivateListing(Long listingId, String userEmail) {
        User seller = resolveUser(userEmail);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        LISTING_RESOURCE, "id", listingId));

        verifyOwnership(listing, seller);

        if (!STATUS_ACTIVE.equals(listing.getStatus())) {
            throw new ConflictException(
                    "Can only deactivate active listings");
        }

        listing.setStatus(STATUS_CANCELLED);
        listingRepository.save(listing);

        Ticket ticket = listing.getTicket();
        ticket.setStatus(TICKET_AVAILABLE);
        ticketRepository.save(ticket);

        log.info("Seller {} deactivated listing {}",
                seller.getEmail(), listingId);
    }

    @Transactional(readOnly = true)
    public EarningsSummaryDto getEarningsSummary(String userEmail) {
        User seller = resolveUser(userEmail);
        Long sellerId = seller.getId();

        BigDecimal totalEarnings =
                orderItemRepository.sumEarningsBySellerId(sellerId);
        long totalListings =
                listingRepository.countBySellerIdAndStatus(sellerId, STATUS_ACTIVE)
                + listingRepository.countBySellerIdAndStatus(sellerId, STATUS_SOLD)
                + listingRepository.countBySellerIdAndStatus(
                        sellerId, STATUS_CANCELLED);
        long activeListings =
                listingRepository.countBySellerIdAndStatus(
                        sellerId, STATUS_ACTIVE);
        long soldListings =
                listingRepository.countBySellerIdAndStatus(
                        sellerId, STATUS_SOLD);

        List<OrderItem> completedSales =
                orderItemRepository.findCompletedSalesBySellerId(sellerId);
        List<SaleDto> recentSales = completedSales.stream()
                .limit(10)
                .map(this::toSaleDto)
                .toList();

        return new EarningsSummaryDto(
                totalEarnings,
                totalListings,
                activeListings,
                soldListings,
                recentSales);
    }

    @Transactional(readOnly = true)
    public List<TicketSearchResultDto> searchTickets(ListingSearchCriteria criteria) {
        Specification<Listing> spec = ListingSearchSpecification.fromCriteria(criteria);

        // Phase 1: Find matching listing IDs via Specification
        List<Long> listingIds = listingRepository.findAll(spec, PageRequest.of(0, criteria.limit()))
                .map(Listing::getId)
                .toList();

        if (listingIds.isEmpty()) {
            return List.of();
        }

        // Phase 2: Fetch full details with eager joins
        List<Listing> listings = listingRepository.findByIdsWithDetails(listingIds);

        return listings.stream()
                .map(this::toTicketSearchResultDto)
                .toList();
    }

    private TicketSearchResultDto toTicketSearchResultDto(Listing listing) {
        Ticket ticket = listing.getTicket();
        Event event = listing.getEvent();

        String rowLabel = null;
        String seatNumber = null;
        if (ticket.getSeat() != null) {
            rowLabel = ticket.getSeat().getRow().getRowLabel();
            seatNumber = ticket.getSeat().getSeatNumber();
        }

        String sellerDisplayName = null;
        if (listing.getSeller() != null) {
            sellerDisplayName = listing.getSeller().getFirstName() + " "
                    + listing.getSeller().getLastName().charAt(0) + ".";
        }

        return new TicketSearchResultDto(
                listing.getId(),
                ticket.getId(),
                event.getName(),
                event.getSlug(),
                event.getArtistName(),
                event.getCategory().getName(),
                event.getVenue().getName(),
                event.getVenue().getCity(),
                event.getEventDate(),
                ticket.getSection().getName(),
                rowLabel,
                seatNumber,
                ticket.getTicketType(),
                listing.getComputedPrice(),
                sellerDisplayName
        );
    }

    // -- Private helpers --

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));
    }

    private void verifyOwnership(Listing listing, User seller) {
        if (listing.getSeller() == null
                || !listing.getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedException(
                    "You do not own this listing");
        }
    }

    private SellerListingDto toSellerListingDto(Listing listing) {
        Ticket ticket = listing.getTicket();
        Event event = listing.getEvent();
        String sectionName = ticket.getSection().getName();
        String rowLabel = null;
        String seatNumber = null;

        if (ticket.getSeat() != null) {
            rowLabel = ticket.getSeat().getRow().getRowLabel();
            seatNumber = ticket.getSeat().getSeatNumber();
        }

        return new SellerListingDto(
                listing.getId(),
                ticket.getId(),
                event.getSlug(),
                event.getName(),
                event.getEventDate(),
                event.getVenue().getName(),
                sectionName,
                rowLabel,
                seatNumber,
                ticket.getTicketType(),
                listing.getListedPrice(),
                listing.getComputedPrice(),
                ticket.getFaceValue(),
                listing.getStatus(),
                listing.getListedAt(),
                listing.getCreatedAt());
    }

    private SaleDto toSaleDto(OrderItem orderItem) {
        Ticket ticket = orderItem.getTicket();
        String sectionName = ticket.getSection().getName();
        String seatInfo = sectionName;

        if (ticket.getSeat() != null) {
            seatInfo = sectionName + ", Row "
                    + ticket.getSeat().getRow().getRowLabel()
                    + ", Seat " + ticket.getSeat().getSeatNumber();
        }

        return new SaleDto(
                orderItem.getOrder().getOrderNumber(),
                orderItem.getListing().getEvent().getName(),
                sectionName,
                seatInfo,
                orderItem.getPricePaid(),
                orderItem.getOrder().getConfirmedAt());
    }

    private ListingDto toListingDto(Listing listing) {
        Ticket ticket = listing.getTicket();
        String sectionName = ticket.getSection().getName();
        String rowLabel = null;
        String seatNumber = null;

        if (ticket.getSeat() != null) {
            rowLabel = ticket.getSeat().getRow().getRowLabel();
            seatNumber = ticket.getSeat().getSeatNumber();
        }

        String sellerDisplayName = null;
        if (listing.getSeller() != null) {
            sellerDisplayName = listing.getSeller().getFirstName() + " "
                    + listing.getSeller().getLastName().charAt(0) + ".";
        }

        return new ListingDto(
                listing.getId(),
                ticket.getId(),
                listing.getEvent().getSlug(),
                sectionName,
                rowLabel,
                seatNumber,
                ticket.getTicketType(),
                listing.getListedPrice(),
                listing.getComputedPrice(),
                listing.getPriceMultiplier(),
                listing.getStatus(),
                listing.getListedAt(),
                sellerDisplayName
        );
    }
}
