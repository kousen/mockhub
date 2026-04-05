package com.mockhub.ticketmaster.dto;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.AllInclusivePricing;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Classification;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Dates;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.DoorsTimes;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Embedded;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Genre;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Image;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.PriceRange;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Segment;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Start;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Status;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.SubGenre;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Ticketing;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterEventResponseBuilderTest {

    @Test
    void build_givenNoFieldsSet_returnsRecordWithAllNulls() {
        TicketmasterEventResponse response = TicketmasterEventResponse.builder().build();

        assertThat(response.id()).isNull();
        assertThat(response.name()).isNull();
        assertThat(response.url()).isNull();
        assertThat(response.dates()).isNull();
        assertThat(response.classifications()).isNull();
        assertThat(response.images()).isNull();
        assertThat(response.priceRanges()).isNull();
        assertThat(response.ticketing()).isNull();
        assertThat(response.doorsTimes()).isNull();
        assertThat(response.embedded()).isNull();
    }

    @Test
    void build_givenAllFieldsSet_returnsFullyPopulatedRecord() {
        Dates dates = new Dates(
                new Start("2026-04-10", "20:00:00", "2026-04-11T00:00:00Z", false, false),
                "America/New_York",
                new Status("onsale"));
        List<Classification> classifications = List.of(new Classification(true,
                new Segment("1", "Music"), new Genre("1", "Rock"), new SubGenre("1", "Pop")));
        List<Image> images = List.of(
                new Image("https://example.com/img.jpg", "16_9", 1024, 576, false));
        List<PriceRange> priceRanges = List.of(new PriceRange("standard", "USD", 50.0, 200.0));
        Ticketing ticketing = new Ticketing(new AllInclusivePricing(true));
        DoorsTimes doorsTimes = new DoorsTimes("2026-04-10", "18:30:00", "2026-04-10T22:30:00Z");
        Embedded embedded = new Embedded(null, null);

        TicketmasterEventResponse response = TicketmasterEventResponse.builder()
                .id("TM-001")
                .name("Test Concert")
                .url("https://ticketmaster.com/test")
                .dates(dates)
                .classifications(classifications)
                .images(images)
                .priceRanges(priceRanges)
                .ticketing(ticketing)
                .doorsTimes(doorsTimes)
                .embedded(embedded)
                .build();

        assertThat(response.id()).isEqualTo("TM-001");
        assertThat(response.name()).isEqualTo("Test Concert");
        assertThat(response.url()).isEqualTo("https://ticketmaster.com/test");
        assertThat(response.dates()).isEqualTo(dates);
        assertThat(response.classifications()).isEqualTo(classifications);
        assertThat(response.images()).isEqualTo(images);
        assertThat(response.priceRanges()).isEqualTo(priceRanges);
        assertThat(response.ticketing()).isEqualTo(ticketing);
        assertThat(response.doorsTimes()).isEqualTo(doorsTimes);
        assertThat(response.embedded()).isEqualTo(embedded);
    }

    @Test
    void build_givenPartialFields_leavesUnsetFieldsNull() {
        TicketmasterEventResponse response = TicketmasterEventResponse.builder()
                .id("TM-PARTIAL")
                .name("Partial Event")
                .dates(new Dates(
                        new Start("2026-06-01", "20:00:00", "2026-06-01T20:00:00Z", false, false),
                        "America/New_York",
                        new Status("onsale")))
                .build();

        assertThat(response.id()).isEqualTo("TM-PARTIAL");
        assertThat(response.name()).isEqualTo("Partial Event");
        assertThat(response.dates()).isNotNull();
        assertThat(response.url()).isNull();
        assertThat(response.classifications()).isNull();
        assertThat(response.images()).isNull();
        assertThat(response.priceRanges()).isNull();
        assertThat(response.ticketing()).isNull();
        assertThat(response.doorsTimes()).isNull();
        assertThat(response.embedded()).isNull();
    }

    @Test
    void build_givenBuilderReused_eachBuildCreatesIndependentRecord() {
        TicketmasterEventResponse.Builder builder = TicketmasterEventResponse.builder()
                .id("TM-001")
                .name("First");

        TicketmasterEventResponse first = builder.build();
        TicketmasterEventResponse second = builder.name("Second").build();

        assertThat(first.name()).isEqualTo("First");
        assertThat(second.name()).isEqualTo("Second");
        assertThat(first.id()).isEqualTo("TM-001");
        assertThat(second.id()).isEqualTo("TM-001");
    }

    @Test
    void builder_givenStaticFactoryMethod_returnsNewBuilderInstance() {
        TicketmasterEventResponse.Builder builder = TicketmasterEventResponse.builder();

        assertThat(builder).isNotNull();
        assertThat(builder).isInstanceOf(TicketmasterEventResponse.Builder.class);
    }

    @Test
    void build_givenMethodChaining_eachSetterReturnsSameBuilder() {
        TicketmasterEventResponse.Builder builder = TicketmasterEventResponse.builder();

        assertThat(builder.id("id")).isSameAs(builder);
        assertThat(builder.name("name")).isSameAs(builder);
        assertThat(builder.url("url")).isSameAs(builder);
        assertThat(builder.dates(null)).isSameAs(builder);
        assertThat(builder.classifications(null)).isSameAs(builder);
        assertThat(builder.images(null)).isSameAs(builder);
        assertThat(builder.priceRanges(null)).isSameAs(builder);
        assertThat(builder.ticketing(null)).isSameAs(builder);
        assertThat(builder.doorsTimes(null)).isSameAs(builder);
        assertThat(builder.embedded(null)).isSameAs(builder);
    }

    @Test
    void build_givenBuiltRecord_isEqualToDirectConstructor() {
        Dates dates = new Dates(
                new Start("2026-04-10", "20:00:00", "2026-04-11T00:00:00Z", false, false),
                "America/New_York",
                new Status("onsale"));

        TicketmasterEventResponse fromBuilder = TicketmasterEventResponse.builder()
                .id("TM-EQ")
                .name("Equality Test")
                .dates(dates)
                .build();

        TicketmasterEventResponse fromConstructor = new TicketmasterEventResponse(
                "TM-EQ", "Equality Test", null, dates, null, null, null, null, null, null);

        assertThat(fromBuilder).isEqualTo(fromConstructor);
    }
}
