package com.mockhub.ticketmaster.service;

import java.util.List;

import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;

public interface TicketmasterService {

    List<TicketmasterEventResponse> searchEvents(String classificationName,
                                                  String startDateTime,
                                                  String endDateTime,
                                                  int size,
                                                  int page);
}
