package com.mockhub.ticketmaster.service;

import java.util.List;

import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;

public interface TicketmasterService {

    List<TicketmasterEventResponse> searchEvents(String classificationName,
                                                  String startDateTime,
                                                  String endDateTime,
                                                  int size,
                                                  int page);

    /**
     * Fetch a single event by its Ticketmaster ID.
     * Returns null if the event is not found or the API call fails.
     */
    TicketmasterEventResponse getEvent(String eventId);
}
