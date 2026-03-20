package com.mockhub.seed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

@Component
public class VenueSeeder {

    private static final Logger log = LoggerFactory.getLogger(VenueSeeder.class);

    private final VenueRepository venueRepository;

    public VenueSeeder(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional
    public void seed() {
        if (venueRepository.count() > 0) {
            log.info("Venues already seeded, skipping");
            return;
        }

        List<VenueData> venues = getVenueData();
        int totalSeats = 0;

        for (VenueData venueData : venues) {
            Venue venue = new Venue();
            venue.setName(venueData.name);
            venue.setSlug(slugify(venueData.name));
            venue.setAddressLine1(venueData.address);
            venue.setCity(venueData.city);
            venue.setState(venueData.state);
            venue.setZipCode(venueData.zipCode);
            venue.setCountry("US");
            venue.setLatitude(venueData.latitude);
            venue.setLongitude(venueData.longitude);
            venue.setCapacity(venueData.capacity);
            venue.setVenueType(venueData.venueType);

            List<Section> sections = createSections(venue, venueData);
            venue.setSections(sections);

            int seatCount = sections.stream()
                    .flatMap(s -> s.getSeatRows().stream())
                    .mapToInt(SeatRow::getSeatCount)
                    .sum();
            totalSeats += seatCount;

            venueRepository.save(venue);
        }

        log.info("Seeded {} venues with {} total seats", venues.size(), totalSeats);
    }

    private List<Section> createSections(Venue venue, VenueData venueData) {
        List<Section> sections = new ArrayList<>();
        int sortOrder = 0;

        for (SectionData sectionData : venueData.sections) {
            Section section = new Section();
            section.setVenue(venue);
            section.setName(sectionData.name);
            section.setSectionType(sectionData.type);
            section.setCapacity(sectionData.rowCount * sectionData.seatsPerRow);
            section.setSortOrder(sortOrder++);
            section.setColorHex(sectionData.colorHex);

            List<SeatRow> rows = createRows(section, sectionData.rowCount, sectionData.seatsPerRow);
            section.setSeatRows(rows);

            sections.add(section);
        }
        return sections;
    }

    private List<SeatRow> createRows(Section section, int rowCount, int seatsPerRow) {
        List<SeatRow> rows = new ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            String rowLabel = String.valueOf((char) ('A' + r));
            SeatRow row = new SeatRow();
            row.setSection(section);
            row.setRowLabel(rowLabel);
            row.setSeatCount(seatsPerRow);
            row.setSortOrder(r);

            List<Seat> seats = new ArrayList<>();
            for (int s = 1; s <= seatsPerRow; s++) {
                Seat seat = new Seat();
                seat.setRow(row);
                seat.setSeatNumber(String.valueOf(s));
                seat.setSeatType("STANDARD");
                seat.setAisle(s == 1 || s == seatsPerRow);
                seats.add(seat);
            }
            row.setSeats(seats);
            rows.add(row);
        }
        return rows;
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|(?:-$))", "");
    }

    private List<VenueData> getVenueData() {
        List<VenueData> venues = new ArrayList<>();

        // Large Arenas
        venues.add(new VenueData("Madison Square Garden", "4 Pennsylvania Plaza", "New York", "NY",
                "10001", new BigDecimal("40.750504"), new BigDecimal("-73.993439"), 20789, "ARENA",
                List.of(
                        new SectionData("Floor", "FLOOR", 10, 20, "#FF4444"),
                        new SectionData("Lower Bowl", "LOWER", 15, 25, "#FF8800"),
                        new SectionData("Club Level", "CLUB", 10, 20, "#FFCC00"),
                        new SectionData("Upper Bowl", "UPPER", 20, 30, "#44BB44")
                )));

        venues.add(new VenueData("Crypto.com Arena", "1111 S Figueroa St", "Los Angeles", "CA",
                "90015", new BigDecimal("34.043000"), new BigDecimal("-118.267254"), 20000, "ARENA",
                List.of(
                        new SectionData("Floor", "FLOOR", 8, 18, "#FF4444"),
                        new SectionData("Premier", "LOWER", 12, 22, "#FF8800"),
                        new SectionData("Suite Level", "CLUB", 8, 16, "#FFCC00"),
                        new SectionData("Upper Concourse", "UPPER", 18, 28, "#44BB44")
                )));

        venues.add(new VenueData("United Center", "1901 W Madison St", "Chicago", "IL",
                "60612", new BigDecimal("41.880694"), new BigDecimal("-87.674194"), 23500, "ARENA",
                List.of(
                        new SectionData("Floor", "FLOOR", 10, 20, "#FF4444"),
                        new SectionData("100 Level", "LOWER", 15, 24, "#FF8800"),
                        new SectionData("200 Level", "CLUB", 12, 22, "#FFCC00"),
                        new SectionData("300 Level", "UPPER", 20, 30, "#44BB44")
                )));

        venues.add(new VenueData("Chase Center", "1 Warriors Way", "San Francisco", "CA",
                "94158", new BigDecimal("37.768056"), new BigDecimal("-122.387778"), 18064, "ARENA",
                List.of(
                        new SectionData("Courtside", "FLOOR", 4, 20, "#FF4444"),
                        new SectionData("Lower Bowl", "LOWER", 14, 22, "#FF8800"),
                        new SectionData("Club Level", "CLUB", 10, 18, "#FFCC00"),
                        new SectionData("Upper Bowl", "UPPER", 16, 26, "#44BB44")
                )));

        venues.add(new VenueData("Barclays Center", "620 Atlantic Ave", "Brooklyn", "NY",
                "11217", new BigDecimal("40.682661"), new BigDecimal("-73.975225"), 19000, "ARENA",
                List.of(
                        new SectionData("Floor", "FLOOR", 8, 20, "#FF4444"),
                        new SectionData("100 Level", "LOWER", 12, 24, "#FF8800"),
                        new SectionData("Suite Level", "CLUB", 8, 16, "#FFCC00"),
                        new SectionData("200 Level", "UPPER", 18, 28, "#44BB44")
                )));

        venues.add(new VenueData("TD Garden", "100 Legends Way", "Boston", "MA",
                "02114", new BigDecimal("42.366303"), new BigDecimal("-71.062228"), 19580, "ARENA",
                List.of(
                        new SectionData("Loge", "LOWER", 12, 22, "#FF8800"),
                        new SectionData("Club Level", "CLUB", 8, 18, "#FFCC00"),
                        new SectionData("Balcony", "UPPER", 16, 28, "#44BB44")
                )));

        // Theaters
        venues.add(new VenueData("Radio City Music Hall", "1260 6th Ave", "New York", "NY",
                "10020", new BigDecimal("40.759976"), new BigDecimal("-73.979988"), 6015, "THEATER",
                List.of(
                        new SectionData("Orchestra", "ORCHESTRA", 20, 30, "#FF4444"),
                        new SectionData("First Mezzanine", "MEZZANINE", 12, 28, "#FF8800"),
                        new SectionData("Second Mezzanine", "MEZZANINE", 10, 26, "#FFCC00"),
                        new SectionData("Third Mezzanine", "BALCONY", 8, 24, "#44BB44")
                )));

        venues.add(new VenueData("The Fillmore", "1805 Geary Blvd", "San Francisco", "CA",
                "94115", new BigDecimal("37.784172"), new BigDecimal("-122.433068"), 1315, "THEATER",
                List.of(
                        new SectionData("General Admission Floor", "FLOOR", 1, 20, "#FF4444"),
                        new SectionData("Balcony", "BALCONY", 8, 15, "#FF8800")
                )));

        venues.add(new VenueData("The Beacon Theatre", "2124 Broadway", "New York", "NY",
                "10023", new BigDecimal("40.780808"), new BigDecimal("-73.981290"), 2894, "THEATER",
                List.of(
                        new SectionData("Orchestra", "ORCHESTRA", 15, 25, "#FF4444"),
                        new SectionData("Loge", "MEZZANINE", 8, 20, "#FF8800"),
                        new SectionData("Upper Balcony", "BALCONY", 10, 22, "#44BB44")
                )));

        venues.add(new VenueData("The Wiltern", "3790 Wilshire Blvd", "Los Angeles", "CA",
                "90010", new BigDecimal("34.061667"), new BigDecimal("-118.309722"), 1850, "THEATER",
                List.of(
                        new SectionData("Orchestra", "ORCHESTRA", 12, 20, "#FF4444"),
                        new SectionData("Loge", "MEZZANINE", 6, 18, "#FF8800"),
                        new SectionData("Balcony", "BALCONY", 8, 16, "#44BB44")
                )));

        // Outdoor Amphitheaters
        venues.add(new VenueData("Red Rocks Amphitheatre", "18300 W Alameda Pkwy", "Morrison", "CO",
                "80465", new BigDecimal("39.665389"), new BigDecimal("-105.205278"), 9525, "AMPHITHEATER",
                List.of(
                        new SectionData("Front Row Reserved", "RESERVED", 5, 30, "#FF4444"),
                        new SectionData("Reserved Seating", "RESERVED", 15, 30, "#FF8800"),
                        new SectionData("General Admission", "GA", 20, 30, "#44BB44")
                )));

        venues.add(new VenueData("Hollywood Bowl", "2301 N Highland Ave", "Los Angeles", "CA",
                "90068", new BigDecimal("34.112222"), new BigDecimal("-118.338889"), 17500, "AMPHITHEATER",
                List.of(
                        new SectionData("Pool Circle", "RESERVED", 8, 20, "#FF4444"),
                        new SectionData("Garden Boxes", "BOX", 10, 18, "#FF8800"),
                        new SectionData("Terrace", "RESERVED", 15, 25, "#FFCC00"),
                        new SectionData("Bench Seating", "GA", 20, 30, "#44BB44")
                )));

        venues.add(new VenueData("The Gorge Amphitheatre", "754 Silica Rd NW", "George", "WA",
                "98848", new BigDecimal("47.102778"), new BigDecimal("-119.997778"), 27500, "AMPHITHEATER",
                List.of(
                        new SectionData("Orchestra Pit", "FLOOR", 8, 25, "#FF4444"),
                        new SectionData("Reserved Seating", "RESERVED", 15, 30, "#FF8800"),
                        new SectionData("General Admission Lawn", "GA", 25, 40, "#44BB44")
                )));

        venues.add(new VenueData("Merriweather Post Pavilion", "10475 Little Patuxent Pkwy",
                "Columbia", "MD", "21044", new BigDecimal("39.210556"), new BigDecimal("-76.862222"),
                19319, "AMPHITHEATER",
                List.of(
                        new SectionData("Orchestra", "RESERVED", 10, 25, "#FF4444"),
                        new SectionData("Pavilion", "RESERVED", 12, 22, "#FF8800"),
                        new SectionData("Lawn", "GA", 20, 35, "#44BB44")
                )));

        // Small Clubs
        venues.add(new VenueData("Blue Note Jazz Club", "131 W 3rd St", "New York", "NY",
                "10012", new BigDecimal("40.730708"), new BigDecimal("-74.000672"), 240, "CLUB",
                List.of(
                        new SectionData("Front Tables", "TABLE", 4, 8, "#FF4444"),
                        new SectionData("Bar Area", "GA", 3, 10, "#FF8800"),
                        new SectionData("Back Tables", "TABLE", 4, 8, "#44BB44")
                )));

        venues.add(new VenueData("Village Vanguard", "178 7th Ave S", "New York", "NY",
                "10014", new BigDecimal("40.735998"), new BigDecimal("-74.001635"), 123, "CLUB",
                List.of(
                        new SectionData("Front Row", "TABLE", 2, 8, "#FF4444"),
                        new SectionData("Main Floor", "TABLE", 4, 10, "#FF8800"),
                        new SectionData("Side Seating", "TABLE", 3, 6, "#44BB44")
                )));

        venues.add(new VenueData("The Troubadour", "9081 Santa Monica Blvd", "West Hollywood", "CA",
                "90069", new BigDecimal("34.081667"), new BigDecimal("-118.389444"), 500, "CLUB",
                List.of(
                        new SectionData("Floor", "GA", 1, 20, "#FF4444"),
                        new SectionData("Balcony", "BALCONY", 5, 12, "#FF8800")
                )));

        venues.add(new VenueData("9:30 Club", "815 V St NW", "Washington", "DC",
                "20001", new BigDecimal("38.917222"), new BigDecimal("-77.023889"), 1200, "CLUB",
                List.of(
                        new SectionData("Main Floor", "GA", 1, 25, "#FF4444"),
                        new SectionData("Balcony", "BALCONY", 6, 15, "#FF8800")
                )));

        venues.add(new VenueData("First Avenue", "701 1st Ave N", "Minneapolis", "MN",
                "55403", new BigDecimal("44.979167"), new BigDecimal("-93.276111"), 1550, "CLUB",
                List.of(
                        new SectionData("Main Room Floor", "GA", 1, 25, "#FF4444"),
                        new SectionData("Balcony", "BALCONY", 6, 16, "#FF8800")
                )));

        // Sports Stadiums
        venues.add(new VenueData("Yankee Stadium", "1 E 161st St", "Bronx", "NY",
                "10451", new BigDecimal("40.829167"), new BigDecimal("-73.926389"), 54251, "STADIUM",
                List.of(
                        new SectionData("Field Level", "LOWER", 15, 25, "#FF4444"),
                        new SectionData("Main Level", "LOWER", 20, 30, "#FF8800"),
                        new SectionData("Grandstand", "UPPER", 20, 30, "#FFCC00"),
                        new SectionData("Bleachers", "GA", 15, 35, "#44BB44")
                )));

        venues.add(new VenueData("SoFi Stadium", "1001 Stadium Dr", "Inglewood", "CA",
                "90301", new BigDecimal("33.953333"), new BigDecimal("-118.339167"), 70240, "STADIUM",
                List.of(
                        new SectionData("Field Level", "LOWER", 20, 30, "#FF4444"),
                        new SectionData("Club Level", "CLUB", 15, 25, "#FF8800"),
                        new SectionData("Upper Level", "UPPER", 25, 35, "#FFCC00"),
                        new SectionData("Top Deck", "UPPER", 20, 30, "#44BB44")
                )));

        venues.add(new VenueData("Wrigley Field", "1060 W Addison St", "Chicago", "IL",
                "60613", new BigDecimal("41.948333"), new BigDecimal("-87.655556"), 41649, "STADIUM",
                List.of(
                        new SectionData("Field Box", "LOWER", 12, 22, "#FF4444"),
                        new SectionData("Terrace Reserved", "LOWER", 15, 25, "#FF8800"),
                        new SectionData("Upper Deck", "UPPER", 18, 28, "#FFCC00"),
                        new SectionData("Bleachers", "GA", 10, 30, "#44BB44")
                )));

        return venues;
    }

    private record VenueData(String name, String address, String city, String state,
                             String zipCode, BigDecimal latitude, BigDecimal longitude,
                             int capacity, String venueType, List<SectionData> sections) {
    }

    private record SectionData(String name, String type, int rowCount, int seatsPerRow, String colorHex) {
    }
}
