package com.mockhub.ticketmaster.service;

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

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.event.entity.Event;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

@Component
public class TicketmasterTicketGenerator {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterTicketGenerator.class);
    private static final double LISTING_PERCENTAGE = 0.35;
    private static final int SEATS_PER_ROW = 20;

    private final TicketRepository ticketRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final Random random = new Random();

    public TicketmasterTicketGenerator(TicketRepository ticketRepository,
                                       ListingRepository listingRepository,
                                       UserRepository userRepository,
                                       VenueRepository venueRepository) {
        this.ticketRepository = ticketRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
    }

    @Transactional
    public void generateForEvent(Event event) {
        Venue venue = event.getVenue();

        // If venue has no sections, create generic ones
        if (venue.getSections().isEmpty()) {
            createGenericSections(venue);
            venueRepository.save(venue);
        }

        List<User> sellers = userRepository.findAll().stream()
                .filter(u -> !u.getEmail().equals("admin@mockhub.com"))
                .toList();

        if (sellers.isEmpty()) {
            log.warn("No sellers available for ticket generation, skipping listings for event: {}",
                    event.getName());
            createTicketsOnly(event, venue);
            return;
        }

        int totalTickets = 0;
        int totalListings = 0;
        int sellerIndex = 0;

        for (Section section : venue.getSections()) {
            List<Ticket> sectionTickets = new ArrayList<>();

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
                    sectionTickets.add(ticket);
                }
            }

            List<Ticket> savedTickets = ticketRepository.saveAll(sectionTickets);
            totalTickets += savedTickets.size();

            List<Listing> sectionListings = new ArrayList<>();
            for (Ticket ticket : savedTickets) {
                if (random.nextDouble() < LISTING_PERCENTAGE) {
                    Listing listing = new Listing();
                    listing.setTicket(ticket);
                    listing.setEvent(event);

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
                    listing.setSeller(sellers.get(sellerIndex % sellers.size()));
                    sellerIndex++;

                    sectionListings.add(listing);
                    ticket.setStatus("LISTED");
                }
            }

            if (!sectionListings.isEmpty()) {
                listingRepository.saveAll(sectionListings);
                ticketRepository.saveAll(savedTickets.stream()
                        .filter(t -> "LISTED".equals(t.getStatus()))
                        .toList());
                totalListings += sectionListings.size();
            }
        }

        log.info("Generated {} tickets and {} listings for event: {}",
                totalTickets, totalListings, event.getName());
    }

    void createGenericSections(Venue venue) {
        record SectionTemplate(String name, String type, String color, int rowCount,
                               double capacityFraction) {
        }

        List<SectionTemplate> templates = List.of(
                new SectionTemplate("Floor", "FLOOR", "#FF4444", 5, 0.20),
                new SectionTemplate("Lower Level", "LOWER", "#4488FF", 10, 0.40),
                new SectionTemplate("Upper Level", "UPPER", "#44BB44", 10, 0.40));

        int totalCapacity = 0;
        int sortOrder = 0;

        for (SectionTemplate template : templates) {
            Section section = new Section();
            section.setVenue(venue);
            section.setName(template.name());
            section.setSectionType(template.type());
            section.setSortOrder(sortOrder++);
            section.setColorHex(template.color());
            section.setSvgPathId(template.name().toLowerCase().replace(" ", "-"));

            int sectionCapacity = template.rowCount() * SEATS_PER_ROW;
            section.setCapacity(sectionCapacity);
            totalCapacity += sectionCapacity;

            List<SeatRow> rows = new ArrayList<>();
            for (int r = 0; r < template.rowCount(); r++) {
                SeatRow row = new SeatRow();
                row.setSection(section);
                row.setRowLabel(String.valueOf((char) ('A' + r)));
                row.setSeatCount(SEATS_PER_ROW);
                row.setSortOrder(r);

                List<Seat> seats = new ArrayList<>();
                for (int s = 1; s <= SEATS_PER_ROW; s++) {
                    Seat seat = new Seat();
                    seat.setRow(row);
                    seat.setSeatNumber(String.valueOf(s));
                    seat.setSeatType("STANDARD");
                    seat.setAisle(s == 1 || s == SEATS_PER_ROW);
                    seats.add(seat);
                }
                row.setSeats(seats);
                rows.add(row);
            }
            section.setSeatRows(rows);
            venue.getSections().add(section);
        }

        venue.setCapacity(totalCapacity);
        log.info("Created generic sections for venue: {} (capacity: {})",
                venue.getName(), totalCapacity);
    }

    private void createTicketsOnly(Event event, Venue venue) {
        int totalTickets = 0;
        for (Section section : venue.getSections()) {
            List<Ticket> sectionTickets = new ArrayList<>();
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
                    sectionTickets.add(ticket);
                }
            }
            ticketRepository.saveAll(sectionTickets);
            totalTickets += sectionTickets.size();
        }
        log.info("Generated {} tickets (no listings) for event: {}", totalTickets, event.getName());
    }
}
