package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticket.dto.ListingCreateRequest;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ListingRepository listingRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public ListingService(ListingRepository listingRepository,
                          TicketRepository ticketRepository,
                          EventRepository eventRepository) {
        this.listingRepository = listingRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
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

        if (!"AVAILABLE".equals(ticket.getStatus())) {
            throw new ConflictException("Ticket is not available for listing");
        }

        ticket.setStatus("LISTED");
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
