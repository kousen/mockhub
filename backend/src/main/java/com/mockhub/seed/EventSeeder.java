package com.mockhub.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.entity.Tag;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.repository.TagRepository;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

@Component
public class EventSeeder {

    private static final Logger log = LoggerFactory.getLogger(EventSeeder.class);

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final Random random = new Random(42);

    public EventSeeder(EventRepository eventRepository,
                       VenueRepository venueRepository,
                       CategoryRepository categoryRepository,
                       TagRepository tagRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void seed() {
        if (eventRepository.count() > 0) {
            log.info("Events already seeded, skipping");
            return;
        }

        List<Venue> venues = venueRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        if (venues.isEmpty() || categories.isEmpty()) {
            log.warn("Cannot seed events: venues or categories are empty");
            return;
        }

        List<Tag> allTags = tagRepository.findAll();
        List<EventData> eventDataList = getEventData();
        int created = 0;

        for (EventData eventData : eventDataList) {
            Venue venue = pickVenueForType(venues, eventData.venueType);
            Category category = findCategory(categories, eventData.categorySlug);

            if (venue == null || category == null) {
                continue;
            }

            Event event = new Event();
            event.setName(eventData.name);
            event.setSlug(slugify(eventData.name) + "-" + (created + 1));
            event.setDescription(eventData.description);
            event.setArtistName(eventData.artistName);
            event.setVenue(venue);
            event.setCategory(category);

            int daysFromNow = random.nextInt(180) + 1;
            int hour = 18 + random.nextInt(4);
            Instant eventDate = Instant.now()
                    .plus(daysFromNow, ChronoUnit.DAYS)
                    .truncatedTo(ChronoUnit.DAYS)
                    .plus(hour, ChronoUnit.HOURS);
            event.setEventDate(eventDate);
            event.setDoorsOpenAt(eventDate.minus(1, ChronoUnit.HOURS));

            event.setStatus("ACTIVE");
            event.setBasePrice(eventData.basePrice);
            event.setMinPrice(eventData.basePrice.multiply(new BigDecimal("0.80")));
            event.setMaxPrice(eventData.basePrice.multiply(new BigDecimal("2.50")));
            event.setTotalTickets(venue.getCapacity());
            event.setAvailableTickets(venue.getCapacity());
            event.setFeatured(created < 12);

            Set<Tag> eventTags = new HashSet<>();
            for (String tagSlug : eventData.tagSlugs) {
                allTags.stream()
                        .filter(t -> t.getSlug().equals(tagSlug))
                        .findFirst()
                        .ifPresent(eventTags::add);
            }
            event.setTags(eventTags);

            eventRepository.save(event);
            created++;
        }

        log.info("Seeded {} events", created);
    }

    private Venue pickVenueForType(List<Venue> venues, String venueType) {
        List<Venue> matching = venues.stream()
                .filter(v -> v.getVenueType().equals(venueType))
                .toList();
        if (matching.isEmpty()) {
            return venues.get(random.nextInt(venues.size()));
        }
        return matching.get(random.nextInt(matching.size()));
    }

    private Category findCategory(List<Category> categories, String slug) {
        return categories.stream()
                .filter(c -> c.getSlug().equals(slug))
                .findFirst()
                .orElse(null);
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private List<EventData> getEventData() {
        List<EventData> events = new ArrayList<>();

        // Rock concerts
        events.add(new EventData("Foo Fighters World Tour", "Foo Fighters",
                "Dave Grohl and the Foo Fighters bring their electrifying rock performance to the stage.",
                "concerts", new BigDecimal("95.00"), "ARENA", List.of("rock", "indoor")));
        events.add(new EventData("The Rolling Stones - Hackney Diamonds Tour", "The Rolling Stones",
                "The legendary Rolling Stones return for another unforgettable night of rock and roll.",
                "concerts", new BigDecimal("250.00"), "STADIUM", List.of("rock", "outdoor")));
        events.add(new EventData("Green Day - Saviors Tour", "Green Day",
                "Punk rock icons Green Day perform hits spanning their 30-year career.",
                "concerts", new BigDecimal("85.00"), "ARENA", List.of("rock", "indoor")));
        events.add(new EventData("Pearl Jam Live", "Pearl Jam",
                "Eddie Vedder and Pearl Jam deliver an epic night of grunge and alternative rock.",
                "concerts", new BigDecimal("120.00"), "ARENA", List.of("rock", "indoor")));
        events.add(new EventData("Radiohead - In Rainbows", "Radiohead",
                "Thom Yorke and Radiohead push musical boundaries in this immersive concert experience.",
                "concerts", new BigDecimal("150.00"), "AMPHITHEATER", List.of("rock", "outdoor")));
        events.add(new EventData("Arctic Monkeys Live", "Arctic Monkeys",
                "Sheffield's finest bring their signature sound to a night of indie rock perfection.",
                "concerts", new BigDecimal("110.00"), "ARENA", List.of("rock", "indoor")));
        events.add(new EventData("The Black Keys - Dropout Boogie Tour", "The Black Keys",
                "Blues-rock duo The Black Keys bring raw energy and gritty riffs.",
                "concerts", new BigDecimal("75.00"), "THEATER", List.of("rock", "indoor")));

        // Pop concerts
        events.add(new EventData("Taylor Swift - Eras Tour", "Taylor Swift",
                "The global phenomenon continues. Experience every era of Taylor Swift's career.",
                "concerts", new BigDecimal("450.00"), "STADIUM", List.of("pop", "outdoor")));
        events.add(new EventData("Beyonce - Renaissance World Tour", "Beyonce",
                "Queen Bey returns to the stage with her most spectacular production yet.",
                "concerts", new BigDecimal("350.00"), "STADIUM", List.of("pop", "outdoor")));
        events.add(new EventData("Ed Sheeran Mathematics Tour", "Ed Sheeran",
                "Ed Sheeran performs with just his guitar and loop pedal in this intimate stadium show.",
                "concerts", new BigDecimal("120.00"), "STADIUM", List.of("pop", "outdoor")));
        events.add(new EventData("Billie Eilish - Happier Than Ever Tour", "Billie Eilish",
                "The Grammy-winning artist brings her atmospheric pop to the arena stage.",
                "concerts", new BigDecimal("130.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("Dua Lipa - Future Nostalgia Tour", "Dua Lipa",
                "Dance-pop superstar Dua Lipa brings the party with her chart-topping hits.",
                "concerts", new BigDecimal("110.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("Harry Styles - Love On Tour", "Harry Styles",
                "Harry Styles delivers a joyful celebration of music, fashion, and love.",
                "concerts", new BigDecimal("175.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("Adele - Weekends with Adele", "Adele",
                "The voice of a generation performs her greatest hits in an intimate residency.",
                "concerts", new BigDecimal("300.00"), "THEATER", List.of("pop", "indoor")));

        // Hip-Hop concerts
        events.add(new EventData("Kendrick Lamar - Big Steppers Tour", "Kendrick Lamar",
                "Pulitzer Prize-winning artist Kendrick Lamar delivers a thought-provoking performance.",
                "concerts", new BigDecimal("150.00"), "ARENA", List.of("hip-hop", "indoor")));
        events.add(new EventData("Drake - It's All A Blur Tour", "Drake",
                "The Toronto superstar brings his catalog of hits to the stage.",
                "concerts", new BigDecimal("200.00"), "ARENA", List.of("hip-hop", "indoor")));
        events.add(new EventData("Tyler, the Creator - Call Me If You Get Lost Tour",
                "Tyler, the Creator",
                "Tyler brings his creative vision to life with elaborate stage design and unmatched energy.",
                "concerts", new BigDecimal("95.00"), "ARENA", List.of("hip-hop", "indoor")));
        events.add(new EventData("J. Cole - The Off-Season Tour", "J. Cole",
                "Hip-hop's most introspective artist performs tracks from across his discography.",
                "concerts", new BigDecimal("110.00"), "ARENA", List.of("hip-hop", "indoor")));
        events.add(new EventData("SZA - SOS Tour", "SZA",
                "R&B sensation SZA brings her vulnerability and vocal prowess to a sold-out show.",
                "concerts", new BigDecimal("125.00"), "ARENA", List.of("hip-hop", "indoor")));

        // Country concerts
        events.add(new EventData("Morgan Wallen - One Night At A Time", "Morgan Wallen",
                "Country music's biggest star brings his chart-topping hits to the stadium.",
                "concerts", new BigDecimal("95.00"), "STADIUM", List.of("country", "outdoor")));
        events.add(new EventData("Luke Combs - World Tour", "Luke Combs",
                "Luke Combs delivers heartfelt country music with his powerful voice.",
                "concerts", new BigDecimal("85.00"), "ARENA", List.of("country", "indoor")));
        events.add(new EventData("Chris Stapleton - All-American Road Show", "Chris Stapleton",
                "Grammy winner Chris Stapleton showcases his soulful blend of country and blues.",
                "concerts", new BigDecimal("120.00"), "AMPHITHEATER", List.of("country", "outdoor")));
        events.add(new EventData("Zach Bryan - Burn, Burn, Burn Tour", "Zach Bryan",
                "The rising country star brings raw, honest songwriting to the arena stage.",
                "concerts", new BigDecimal("90.00"), "ARENA", List.of("country", "indoor")));

        // Jazz concerts
        events.add(new EventData("Wynton Marsalis Quintet", "Wynton Marsalis",
                "The legendary trumpeter and his quintet perform classic and modern jazz compositions.",
                "concerts", new BigDecimal("65.00"), "CLUB", List.of("jazz", "indoor")));
        events.add(new EventData("Kamasi Washington Live", "Kamasi Washington",
                "Saxophonist Kamasi Washington delivers cosmic jazz that transcends genre boundaries.",
                "concerts", new BigDecimal("55.00"), "CLUB", List.of("jazz", "indoor")));
        events.add(new EventData("Robert Glasper Experiment", "Robert Glasper",
                "Genre-bending pianist Robert Glasper fuses jazz with hip-hop and R&B.",
                "concerts", new BigDecimal("50.00"), "CLUB", List.of("jazz", "indoor")));
        events.add(new EventData("Esperanza Spalding - Songwrights Apothecary", "Esperanza Spalding",
                "Grammy-winning bassist and singer Esperanza Spalding performs her genre-defying music.",
                "concerts", new BigDecimal("60.00"), "CLUB", List.of("jazz", "indoor")));
        events.add(new EventData("Brad Mehldau Solo Piano", "Brad Mehldau",
                "One of jazz's most celebrated pianists performs an evening of solo improvisations.",
                "concerts", new BigDecimal("75.00"), "CLUB", List.of("jazz", "indoor")));

        // Classical concerts
        events.add(new EventData("Yo-Yo Ma - Bach Cello Suites", "Yo-Yo Ma",
                "The world's most renowned cellist performs Bach's beloved Cello Suites.",
                "concerts", new BigDecimal("150.00"), "THEATER", List.of("classical", "indoor")));
        events.add(new EventData("Lang Lang Piano Recital", "Lang Lang",
                "Virtuoso pianist Lang Lang performs an evening of Chopin and Liszt.",
                "concerts", new BigDecimal("175.00"), "THEATER", List.of("classical", "indoor")));
        events.add(new EventData("Hilary Hahn - Violin Recital", "Hilary Hahn",
                "Grammy-winning violinist Hilary Hahn performs a program of Bach and Brahms.",
                "concerts", new BigDecimal("120.00"), "THEATER", List.of("classical", "indoor")));

        // Sports - Basketball
        events.add(new EventData("Lakers vs. Celtics", null,
                "The NBA's greatest rivalry continues as the Lakers host the Celtics.",
                "sports", new BigDecimal("180.00"), "ARENA", List.of("nba", "indoor")));
        events.add(new EventData("Warriors vs. Nets", null,
                "Two of the NBA's most exciting teams face off in this marquee matchup.",
                "sports", new BigDecimal("150.00"), "ARENA", List.of("nba", "indoor")));
        events.add(new EventData("Knicks vs. Bulls", null,
                "A classic Eastern Conference rivalry under the bright lights of the Garden.",
                "sports", new BigDecimal("120.00"), "ARENA", List.of("nba", "indoor")));
        events.add(new EventData("Celtics vs. 76ers", null,
                "Atlantic Division rivals clash in this heated basketball matchup.",
                "sports", new BigDecimal("110.00"), "ARENA", List.of("nba", "indoor")));
        events.add(new EventData("Lakers vs. Warriors", null,
                "California's biggest NBA rivalry continues in this Pacific Division showdown.",
                "sports", new BigDecimal("200.00"), "ARENA", List.of("nba", "indoor")));

        // Sports - Football
        events.add(new EventData("Giants vs. Cowboys", null,
                "NFC East rivals meet in this Sunday afternoon showdown at MetLife.",
                "sports", new BigDecimal("250.00"), "STADIUM", List.of("nfl", "outdoor")));
        events.add(new EventData("Rams vs. 49ers", null,
                "NFC West rivals battle it out at SoFi Stadium in this divisional matchup.",
                "sports", new BigDecimal("225.00"), "STADIUM", List.of("nfl", "outdoor")));
        events.add(new EventData("Bears vs. Packers", null,
                "The oldest rivalry in the NFL continues as the Bears host the Packers.",
                "sports", new BigDecimal("175.00"), "STADIUM", List.of("nfl", "outdoor")));
        events.add(new EventData("Chiefs vs. Bills", null,
                "AFC powerhouses collide in what promises to be a playoff-caliber matchup.",
                "sports", new BigDecimal("275.00"), "STADIUM", List.of("nfl", "outdoor")));

        // Sports - Baseball
        events.add(new EventData("Yankees vs. Red Sox", null,
                "Baseball's most storied rivalry continues at Yankee Stadium.",
                "sports", new BigDecimal("85.00"), "STADIUM", List.of("mlb", "outdoor")));
        events.add(new EventData("Dodgers vs. Giants", null,
                "West Coast baseball's fiercest rivalry under the California sun.",
                "sports", new BigDecimal("75.00"), "STADIUM", List.of("mlb", "outdoor")));
        events.add(new EventData("Cubs vs. Cardinals", null,
                "The heartland's greatest baseball rivalry at iconic Wrigley Field.",
                "sports", new BigDecimal("65.00"), "STADIUM", List.of("mlb", "outdoor")));
        events.add(new EventData("Mets vs. Phillies", null,
                "NL East rivals face off in this mid-season divisional battle.",
                "sports", new BigDecimal("55.00"), "STADIUM", List.of("mlb", "outdoor")));

        // Sports - Hockey
        events.add(new EventData("Rangers vs. Islanders", null,
                "New York's hockey rivalry ignites at Madison Square Garden.",
                "sports", new BigDecimal("130.00"), "ARENA", List.of("nhl", "indoor")));
        events.add(new EventData("Bruins vs. Canadiens", null,
                "Original Six rivals clash in this historic hockey matchup.",
                "sports", new BigDecimal("110.00"), "ARENA", List.of("nhl", "indoor")));
        events.add(new EventData("Blackhawks vs. Red Wings", null,
                "Two Original Six teams renew their historic rivalry on the ice.",
                "sports", new BigDecimal("95.00"), "ARENA", List.of("nhl", "indoor")));

        // Theater - Broadway
        events.add(new EventData("Hamilton", null,
                "Lin-Manuel Miranda's groundbreaking musical about Alexander Hamilton.",
                "theater", new BigDecimal("300.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("Wicked", null,
                "The untold story of the witches of Oz. One of Broadway's longest-running shows.",
                "theater", new BigDecimal("200.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("The Lion King", null,
                "Julie Taymor's visually stunning adaptation of Disney's beloved animated film.",
                "theater", new BigDecimal("175.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("Dear Evan Hansen", null,
                "A deeply personal and contemporary musical about connection in the digital age.",
                "theater", new BigDecimal("150.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("Hadestown", null,
                "A folk-inspired retelling of the Orpheus and Eurydice myth, set in a Depression-era underworld.",
                "theater", new BigDecimal("160.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("Chicago", null,
                "The longest-running American musical in Broadway history. All that jazz.",
                "theater", new BigDecimal("130.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("Moulin Rouge! The Musical", null,
                "A theatrical celebration of truth, beauty, freedom, and love.",
                "theater", new BigDecimal("225.00"), "THEATER", List.of("broadway", "indoor")));
        events.add(new EventData("Les Miserables", null,
                "The epic tale of broken dreams, unrequited love, and redemption.",
                "theater", new BigDecimal("140.00"), "THEATER", List.of("broadway", "indoor")));

        // Comedy
        events.add(new EventData("Dave Chappelle Live", "Dave Chappelle",
                "Comedy legend Dave Chappelle delivers an evening of sharp, unfiltered stand-up.",
                "comedy", new BigDecimal("125.00"), "THEATER", List.of("stand-up", "indoor")));
        events.add(new EventData("John Mulaney - From Scratch", "John Mulaney",
                "John Mulaney returns with all-new material in his signature razor-sharp style.",
                "comedy", new BigDecimal("95.00"), "THEATER", List.of("stand-up", "indoor")));
        events.add(new EventData("Ali Wong - Single Lady Tour", "Ali Wong",
                "Ali Wong brings her fearless and hilarious comedy to the stage.",
                "comedy", new BigDecimal("85.00"), "THEATER", List.of("stand-up", "indoor")));
        events.add(new EventData("Nate Bargatze - Be Funny Tour", "Nate Bargatze",
                "The nicest man in stand-up comedy delivers wholesome laughs for everyone.",
                "comedy", new BigDecimal("75.00"), "THEATER", List.of("stand-up", "indoor")));
        events.add(new EventData("Trevor Noah - Off The Record Tour", "Trevor Noah",
                "The former Daily Show host delivers witty observations on global culture.",
                "comedy", new BigDecimal("90.00"), "THEATER", List.of("stand-up", "indoor")));
        events.add(new EventData("Hasan Minhaj - The King's Jester", "Hasan Minhaj",
                "Hasan Minhaj blends personal storytelling with sharp political humor.",
                "comedy", new BigDecimal("70.00"), "THEATER", List.of("stand-up", "indoor")));

        // Festivals
        events.add(new EventData("Coachella Music Festival - Weekend 1", null,
                "The world's premier music festival featuring dozens of artists across multiple stages.",
                "festivals", new BigDecimal("500.00"), "AMPHITHEATER", List.of("outdoor")));
        events.add(new EventData("Bonnaroo Music Festival", null,
                "Four days of music, art, and community in the Tennessee countryside.",
                "festivals", new BigDecimal("375.00"), "AMPHITHEATER", List.of("outdoor")));
        events.add(new EventData("Lollapalooza", null,
                "Chicago's iconic multi-day music festival in Grant Park.",
                "festivals", new BigDecimal("400.00"), "AMPHITHEATER", List.of("outdoor")));
        events.add(new EventData("Austin City Limits Music Festival", null,
                "Two weekends of live music in the heart of Austin, Texas.",
                "festivals", new BigDecimal("325.00"), "AMPHITHEATER", List.of("outdoor")));
        events.add(new EventData("Outside Lands Music Festival", null,
                "San Francisco's premier music and food festival in Golden Gate Park.",
                "festivals", new BigDecimal("350.00"), "AMPHITHEATER", List.of("outdoor")));
        events.add(new EventData("Electric Daisy Carnival", null,
                "The world's largest electronic music festival, featuring spectacular stages and production.",
                "festivals", new BigDecimal("425.00"), "STADIUM", List.of("outdoor")));

        // Extra events to reach 100+
        events.add(new EventData("Bruce Springsteen & The E Street Band", "Bruce Springsteen",
                "The Boss and the E Street Band bring decades of rock anthems to the stage.",
                "concerts", new BigDecimal("200.00"), "STADIUM", List.of("rock", "outdoor")));
        events.add(new EventData("Bad Bunny - Most Wanted Tour", "Bad Bunny",
                "Latin music's biggest star brings reggaeton and trap to the arena.",
                "concerts", new BigDecimal("175.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("The Weeknd - After Hours Tour", "The Weeknd",
                "The Weeknd delivers a cinematic concert experience unlike anything else.",
                "concerts", new BigDecimal("160.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("Metallica - M72 World Tour", "Metallica",
                "Thrash metal legends Metallica deliver two hours of raw power and iconic riffs.",
                "concerts", new BigDecimal("175.00"), "STADIUM", List.of("rock", "outdoor")));
        events.add(new EventData("Post Malone - Twelve Carat Tour", "Post Malone",
                "Genre-blending artist Post Malone performs his biggest hits live.",
                "concerts", new BigDecimal("100.00"), "ARENA", List.of("hip-hop", "indoor")));
        events.add(new EventData("Olivia Rodrigo - GUTS World Tour", "Olivia Rodrigo",
                "The pop-rock sensation performs her debut and sophomore albums.",
                "concerts", new BigDecimal("135.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("Coldplay - Music of the Spheres Tour", "Coldplay",
                "Coldplay transforms stadiums into a galaxy of light and sound.",
                "concerts", new BigDecimal("140.00"), "STADIUM", List.of("rock", "outdoor")));
        events.add(new EventData("Imagine Dragons - Mercury Tour", "Imagine Dragons",
                "Imagine Dragons deliver arena-shaking anthems and explosive energy.",
                "concerts", new BigDecimal("95.00"), "ARENA", List.of("rock", "indoor")));
        events.add(new EventData("Red Hot Chili Peppers - Unlimited Love Tour",
                "Red Hot Chili Peppers",
                "RHCP bring their funk-infused rock to fans in this high-energy arena show.",
                "concerts", new BigDecimal("130.00"), "ARENA", List.of("rock", "indoor")));
        events.add(new EventData("Usher - Past Present Future Tour", "Usher",
                "R&B legend Usher brings three decades of hits and incredible dancing.",
                "concerts", new BigDecimal("160.00"), "ARENA", List.of("pop", "indoor")));
        events.add(new EventData("Megan Thee Stallion - Hot Girl Summer Tour",
                "Megan Thee Stallion",
                "Houston's own Megan Thee Stallion delivers a night of high-energy hip-hop.",
                "concerts", new BigDecimal("85.00"), "ARENA", List.of("hip-hop", "indoor")));
        events.add(new EventData("Kenny Chesney - Sun Goes Down Tour", "Kenny Chesney",
                "Country superstar Kenny Chesney brings island vibes to the stadium.",
                "concerts", new BigDecimal("90.00"), "STADIUM", List.of("country", "outdoor")));
        events.add(new EventData("Norah Jones - Live at Blue Note", "Norah Jones",
                "Nine-time Grammy winner Norah Jones performs in an intimate club setting.",
                "concerts", new BigDecimal("80.00"), "CLUB", List.of("jazz", "indoor")));
        events.add(new EventData("Phantom of the Opera", null,
                "Andrew Lloyd Webber's masterpiece returns to Broadway.",
                "theater", new BigDecimal("165.00"), "THEATER", List.of("broadway", "indoor")));

        return events;
    }

    private record EventData(String name, String artistName, String description,
                             String categorySlug, BigDecimal basePrice, String venueType,
                             List<String> tagSlugs) {
    }
}
