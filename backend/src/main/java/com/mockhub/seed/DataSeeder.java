package com.mockhub.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserSeeder userSeeder;
    private final VenueSeeder venueSeeder;
    private final EventSeeder eventSeeder;
    private final TicketSeeder ticketSeeder;

    public DataSeeder(UserSeeder userSeeder,
                      VenueSeeder venueSeeder,
                      EventSeeder eventSeeder,
                      TicketSeeder ticketSeeder) {
        this.userSeeder = userSeeder;
        this.venueSeeder = venueSeeder;
        this.eventSeeder = eventSeeder;
        this.ticketSeeder = ticketSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting data seeding for dev profile...");

        long startTime = System.currentTimeMillis();

        userSeeder.seed();
        venueSeeder.seed();
        eventSeeder.seed();
        ticketSeeder.seed();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Data seeding completed in {} ms", elapsed);
    }
}
