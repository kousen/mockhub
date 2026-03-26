package com.mockhub.order.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mockhub.event.entity.Event;
import com.mockhub.order.entity.Order;
import com.mockhub.venue.entity.Venue;

@Service
public class CalendarService {

    private static final DateTimeFormatter ICS_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String CRLF = "\r\n";
    private static final int EVENT_DURATION_HOURS = 2;

    public String generateIcs(Order order) {
        Event event = order.getItems().getFirst().getListing().getEvent();
        Venue venue = event.getVenue();
        int ticketCount = order.getItems().size();

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR").append(CRLF);
        ics.append("VERSION:2.0").append(CRLF);
        ics.append("PRODID:-//MockHub//Ticket Calendar//EN").append(CRLF);
        ics.append("CALSCALE:GREGORIAN").append(CRLF);
        ics.append("METHOD:PUBLISH").append(CRLF);

        ics.append("BEGIN:VEVENT").append(CRLF);
        ics.append("UID:").append(UUID.nameUUIDFromBytes(order.getOrderNumber().getBytes())).append(CRLF);
        ics.append("DTSTAMP:").append(ICS_DATE_FORMAT.format(Instant.now())).append(CRLF);
        ics.append("DTSTART:").append(ICS_DATE_FORMAT.format(event.getEventDate())).append(CRLF);
        ics.append("DTEND:").append(ICS_DATE_FORMAT.format(
                event.getEventDate().plusSeconds(EVENT_DURATION_HOURS * 3600L))).append(CRLF);
        ics.append("SUMMARY:").append(event.getName()).append(CRLF);
        ics.append("LOCATION:").append(formatLocation(venue)).append(CRLF);
        ics.append("DESCRIPTION:").append(formatDescription(order, event, ticketCount)).append(CRLF);
        ics.append("STATUS:CONFIRMED").append(CRLF);
        ics.append("END:VEVENT").append(CRLF);

        ics.append("END:VCALENDAR").append(CRLF);
        return ics.toString();
    }

    private String formatLocation(Venue venue) {
        StringBuilder location = new StringBuilder();
        location.append(escapeIcs(venue.getName()));
        location.append("\\, ").append(escapeIcs(venue.getAddressLine1()));
        location.append("\\, ").append(escapeIcs(venue.getCity()));
        location.append("\\, ").append(escapeIcs(venue.getState()));
        location.append(" ").append(venue.getZipCode());
        return location.toString();
    }

    private String formatDescription(Order order, Event event, int ticketCount) {
        StringBuilder desc = new StringBuilder();
        desc.append("Order: ").append(order.getOrderNumber());
        desc.append("\\n").append(ticketCount).append(ticketCount == 1 ? " ticket" : " tickets");
        desc.append("\\nTotal: $").append(order.getTotal().toPlainString());

        if (event.getDoorsOpenAt() != null) {
            String doorsTime = DateTimeFormatter.ofPattern("h:mm a")
                    .withZone(ZoneOffset.UTC)
                    .format(event.getDoorsOpenAt());
            desc.append("\\nDoors open at ").append(doorsTime).append(" UTC");
        }

        return desc.toString();
    }

    private String escapeIcs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", "\\,").replace(";", "\\;");
    }
}
