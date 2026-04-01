package com.mockhub.admin.service;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.service.EventService;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.repository.VenueRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceActivateTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventService eventService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void activateTicketmasterEvents_deactivatesSeedAndFeaturesTm() {
        when(eventRepository.deactivateSeedEvents()).thenReturn(100);
        when(eventRepository.featureTicketmasterEvents()).thenReturn(83);
        when(eventRepository.completePastTicketmasterEvents(any())).thenReturn(5);

        Map<String, Integer> result = adminService.activateTicketmasterEvents();

        assertThat(result.get("seedEventsDeactivated")).isEqualTo(100);
        assertThat(result.get("ticketmasterEventsFeatured")).isEqualTo(83);
        assertThat(result.get("pastEventsCompleted")).isEqualTo(5);
        verify(eventRepository).deactivateSeedEvents();
        verify(eventRepository).featureTicketmasterEvents();
        verify(eventRepository).completePastTicketmasterEvents(any());
    }
}
