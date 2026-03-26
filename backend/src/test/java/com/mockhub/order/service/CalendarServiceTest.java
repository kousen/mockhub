package com.mockhub.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.event.entity.Event;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarServiceTest {

    private CalendarService calendarService;
    private Order testOrder;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService();

        Venue venue = new Venue();
        venue.setName("Radio City Music Hall");
        venue.setAddressLine1("1260 6th Ave");
        venue.setCity("New York");
        venue.setState("NY");
        venue.setZipCode("10020");
        venue.setCountry("US");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Yo-Yo Ma — Bach Cello Suites");
        testEvent.setArtistName("Yo-Yo Ma");
        testEvent.setEventDate(LocalDateTime.of(2026, 4, 23, 21, 0).toInstant(ZoneOffset.UTC));
        testEvent.setDoorsOpenAt(LocalDateTime.of(2026, 4, 23, 20, 0).toInstant(ZoneOffset.UTC));
        testEvent.setVenue(venue);

        Section section = new Section();
        section.setName("Orchestra");

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setSection(section);
        ticket.setEvent(testEvent);

        Listing listing = new Listing();
        listing.setId(1L);
        listing.setEvent(testEvent);
        listing.setTicket(ticket);

        OrderItem item = new OrderItem();
        item.setListing(listing);
        item.setTicket(ticket);
        item.setPricePaid(new BigDecimal("102.91"));

        testOrder = new Order();
        testOrder.setOrderNumber("MH-20260326-0002");
        testOrder.setTotal(new BigDecimal("232.75"));
        testOrder.setItems(List.of(item));
    }

    @Test
    @DisplayName("generateIcs - produces valid VCALENDAR wrapper")
    void generateIcs_producesValidVcalendarWrapper() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.startsWith("BEGIN:VCALENDAR"), "Should start with VCALENDAR");
        assertTrue(ics.contains("VERSION:2.0"), "Should include version");
        assertTrue(ics.contains("PRODID:-//MockHub//Ticket Calendar//EN"), "Should include prodid");
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"), "Should end with VCALENDAR");
    }

    @Test
    @DisplayName("generateIcs - includes VEVENT with event name as summary")
    void generateIcs_includesVeventWithEventName() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("BEGIN:VEVENT"), "Should contain VEVENT");
        assertTrue(ics.contains("SUMMARY:Yo-Yo Ma — Bach Cello Suites"), "Should include event name");
        assertTrue(ics.contains("END:VEVENT"), "Should close VEVENT");
    }

    @Test
    @DisplayName("generateIcs - includes event date as DTSTART in UTC")
    void generateIcs_includesEventDateAsDtstart() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("DTSTART:20260423T210000Z"), "Should include UTC start time");
    }

    @Test
    @DisplayName("generateIcs - includes DTEND two hours after start")
    void generateIcs_includesDtendTwoHoursAfterStart() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("DTEND:20260423T230000Z"), "Should include end time 2 hours after start");
    }

    @Test
    @DisplayName("generateIcs - includes venue name and address as LOCATION")
    void generateIcs_includesVenueAsLocation() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("LOCATION:Radio City Music Hall\\, 1260 6th Ave\\, New York\\, NY 10020"),
                "Should include full venue address with escaped commas");
    }

    @Test
    @DisplayName("generateIcs - includes order number and ticket details in DESCRIPTION")
    void generateIcs_includesOrderDetailsInDescription() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("Order: MH-20260326-0002"), "Should include order number");
        assertTrue(ics.contains("Doors open"), "Should mention doors-open time");
        assertTrue(ics.contains("1 ticket"), "Should include ticket count");
    }

    @Test
    @DisplayName("generateIcs - given event without doors-open time - omits doors info")
    void generateIcs_givenNoDoorsOpenTime_omitsDoorsInfo() {
        testEvent.setDoorsOpenAt(null);

        String ics = calendarService.generateIcs(testOrder);

        assertFalse(ics.contains("Doors open"), "Should not mention doors-open when null");
    }

    @Test
    @DisplayName("generateIcs - given multiple items from same event - shows ticket count")
    void generateIcs_givenMultipleItems_showsTicketCount() {
        Section section = new Section();
        section.setName("Orchestra");
        Ticket ticket2 = new Ticket();
        ticket2.setId(2L);
        ticket2.setSection(section);
        ticket2.setEvent(testEvent);
        Listing listing2 = new Listing();
        listing2.setId(2L);
        listing2.setEvent(testEvent);
        listing2.setTicket(ticket2);
        OrderItem item2 = new OrderItem();
        item2.setListing(listing2);
        item2.setTicket(ticket2);
        item2.setPricePaid(new BigDecimal("108.68"));
        testOrder.setItems(List.of(testOrder.getItems().getFirst(), item2));

        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("2 tickets"), "Should show plural ticket count");
    }

    @Test
    @DisplayName("generateIcs - uses CRLF line endings per RFC 5545")
    void generateIcs_usesCrlfLineEndings() {
        String ics = calendarService.generateIcs(testOrder);

        assertTrue(ics.contains("\r\n"), "Should use CRLF line endings");
        assertFalse(ics.contains("\r\n\n"), "Should not have extra newlines");
    }
}
