package com.mockhub.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

@Component
public class TicketSeeder {

    private static final Logger log = LoggerFactory.getLogger(TicketSeeder.class);
    private static final double LISTING_PERCENTAGE = 0.35;

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final ListingRepository listingRepository;
    private final Random random = new Random(42);

    public TicketSeeder(EventRepository eventRepository,
                        TicketRepository ticketRepository,
                        ListingRepository listingRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional
    public void seed() {
        if (ticketRepository.count() > 0) {
            log.info("Tickets already seeded, skipping");
            return;
        }

        List<Event> events = eventRepository.findAll();
        int totalTickets = 0;
        int totalListings = 0;

        for (Event event : events) {
            Venue venue = event.getVenue();
            List<Ticket> eventTickets = new ArrayList<>();

            for (Section section : venue.getSections()) {
                for (SeatRow row : section.getSeatRows()) {
                    for (Seat seat : row.getSeats()) {
                        Ticket ticket = new Ticket();
                        ticket.setEvent(event);
                        ticket.setSeat(seat);
                        ticket.setSection(section);
                        ticket.setTicketType("STANDARD");
                        ticket.setFaceValue(event.getBasePrice());
                        ticket.setStatus("AVAILABLE");
                        ticket.setBarcode(UUID.randomUUID().toString().substring(0, 12));
                        eventTickets.add(ticket);
                    }
                }
            }

            List<Ticket> savedTickets = ticketRepository.saveAll(eventTickets);
            totalTickets += savedTickets.size();

            // Create listings for ~35% of tickets
            List<Listing> eventListings = new ArrayList<>();
            for (Ticket ticket : savedTickets) {
                if (random.nextDouble() < LISTING_PERCENTAGE) {
                    Listing listing = new Listing();
                    listing.setTicket(ticket);
                    listing.setEvent(event);

                    // Vary price around base price (+-20%)
                    double priceVariation = 0.80 + (random.nextDouble() * 0.40);
                    BigDecimal listedPrice = event.getBasePrice()
                            .multiply(BigDecimal.valueOf(priceVariation))
                            .setScale(2, RoundingMode.HALF_UP);
                    listing.setListedPrice(listedPrice);
                    listing.setComputedPrice(listedPrice);
                    listing.setPriceMultiplier(BigDecimal.valueOf(priceVariation)
                            .setScale(3, RoundingMode.HALF_UP));
                    listing.setStatus("ACTIVE");
                    listing.setListedAt(Instant.now());

                    eventListings.add(listing);
                    ticket.setStatus("LISTED");
                }
            }

            if (!eventListings.isEmpty()) {
                listingRepository.saveAll(eventListings);
                ticketRepository.saveAll(savedTickets.stream()
                        .filter(t -> "LISTED".equals(t.getStatus()))
                        .toList());
                totalListings += eventListings.size();
            }
        }

        log.info("Seeded {} tickets with {} listings across {} events",
                totalTickets, totalListings, events.size());
    }
}
