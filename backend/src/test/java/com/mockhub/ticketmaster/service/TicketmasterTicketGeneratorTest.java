package com.mockhub.ticketmaster.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketmasterTicketGeneratorTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VenueRepository venueRepository;

    @Captor
    private ArgumentCaptor<List<Ticket>> ticketCaptor;

    private TicketmasterTicketGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TicketmasterTicketGenerator(
                ticketRepository, listingRepository, userRepository, venueRepository);
    }

    @Test
    void generateForEvent_givenVenueWithSections_createsTicketsForAllSeats() {
        Event event = createEventWithVenue();
        User seller = createSeller();

        when(userRepository.findAll()).thenReturn(List.of(seller));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generateForEvent(event);

        verify(ticketRepository, org.mockito.Mockito.atLeastOnce()).saveAll(ticketCaptor.capture());
        // First capture is the initial ticket creation (6 tickets for 2 rows * 3 seats)
        List<Ticket> tickets = ticketCaptor.getAllValues().getFirst();

        assertThat(tickets).hasSize(6);
        assertThat(tickets).allSatisfy(ticket -> {
            assertThat(ticket.getEvent()).isEqualTo(event);
            assertThat(ticket.getFaceValue()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(ticket.getTicketType()).isEqualTo("STANDARD");
            assertThat(ticket.getBarcode()).isNotNull();
        });
    }

    @Test
    void generateForEvent_givenVenueWithNoSections_createsGenericSections() {
        Venue venue = new Venue();
        venue.setName("New Venue");
        venue.setSlug("new-venue");
        venue.setCapacity(1000);
        venue.setVenueType("ARENA");
        venue.setAddressLine1("123 Test");
        venue.setCity("Test City");
        venue.setState("TS");
        venue.setZipCode("00000");
        venue.setCountry("US");
        venue.setSections(new ArrayList<>());

        Event event = new Event();
        event.setName("Test Event");
        event.setBasePrice(new BigDecimal("100.00"));
        event.setVenue(venue);

        User seller = createSeller();
        when(userRepository.findAll()).thenReturn(List.of(seller));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generateForEvent(event);

        // Should have created 3 generic sections (Floor, Lower Level, Upper Level)
        assertThat(venue.getSections()).hasSize(3);
        assertThat(venue.getSections().stream().map(Section::getName))
                .containsExactly("Floor", "Lower Level", "Upper Level");

        // Capacity should be updated: (5*20) + (10*20) + (10*20) = 500
        assertThat(venue.getCapacity()).isEqualTo(500);
    }

    @Test
    void createGenericSections_givenEmptyVenue_creates3SectionsWithCorrectLayout() {
        Venue venue = new Venue();
        venue.setSections(new ArrayList<>());

        generator.createGenericSections(venue);

        assertThat(venue.getSections()).hasSize(3);

        Section floor = venue.getSections().get(0);
        assertThat(floor.getName()).isEqualTo("Floor");
        assertThat(floor.getSectionType()).isEqualTo("FLOOR");
        assertThat(floor.getSeatRows()).hasSize(5);
        assertThat(floor.getSeatRows().getFirst().getSeats()).hasSize(20);
        assertThat(floor.getCapacity()).isEqualTo(100);

        Section lower = venue.getSections().get(1);
        assertThat(lower.getName()).isEqualTo("Lower Level");
        assertThat(lower.getSeatRows()).hasSize(10);
        assertThat(lower.getCapacity()).isEqualTo(200);

        Section upper = venue.getSections().get(2);
        assertThat(upper.getName()).isEqualTo("Upper Level");
        assertThat(upper.getSeatRows()).hasSize(10);
        assertThat(upper.getCapacity()).isEqualTo(200);

        assertThat(venue.getCapacity()).isEqualTo(500);
    }

    // --- Helpers ---

    private Event createEventWithVenue() {
        Venue venue = new Venue();
        venue.setName("Test Arena");
        venue.setCapacity(6);

        Section section = new Section();
        section.setVenue(venue);
        section.setName("Floor");
        section.setSectionType("FLOOR");
        section.setCapacity(6);

        List<SeatRow> rows = new ArrayList<>();
        for (int r = 0; r < 2; r++) {
            SeatRow row = new SeatRow();
            row.setSection(section);
            row.setRowLabel(String.valueOf((char) ('A' + r)));
            row.setSeatCount(3);

            List<Seat> seats = new ArrayList<>();
            for (int s = 1; s <= 3; s++) {
                Seat seat = new Seat();
                seat.setRow(row);
                seat.setSeatNumber(String.valueOf(s));
                seat.setSeatType("STANDARD");
                seat.setAisle(s == 1 || s == 3);
                seats.add(seat);
            }
            row.setSeats(seats);
            rows.add(row);
        }
        section.setSeatRows(rows);
        venue.setSections(new ArrayList<>(List.of(section)));

        Event event = new Event();
        event.setName("Test Event");
        event.setBasePrice(new BigDecimal("100.00"));
        event.setVenue(venue);

        return event;
    }

    private User createSeller() {
        User user = new User();
        user.setEmail("seller@example.com");
        user.setFirstName("Test");
        user.setLastName("Seller");
        return user;
    }
}
