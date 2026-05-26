package com.mockhub.ticketmaster.dto;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse.ExternalLink;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterAttractionResponseBuilderTest {

    @Test
    void build_givenNoFieldsSet_returnsRecordWithAllNulls() {
        TicketmasterAttractionResponse response = TicketmasterAttractionResponse.builder().build();

        assertThat(response.id()).isNull();
        assertThat(response.name()).isNull();
        assertThat(response.externalLinks()).isNull();
    }

    @Test
    void build_givenAllFieldsSet_returnsFullyPopulatedRecord() {
        Map<String, List<ExternalLink>> externalLinks = Map.of(
                "spotify", List.of(
                        new ExternalLink("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null)));

        TicketmasterAttractionResponse response = TicketmasterAttractionResponse.builder()
                .id("K8vZ9171ob7")
                .name("Eagles")
                .externalLinks(externalLinks)
                .build();

        assertThat(response.id()).isEqualTo("K8vZ9171ob7");
        assertThat(response.name()).isEqualTo("Eagles");
        assertThat(response.externalLinks()).isEqualTo(externalLinks);
    }

    @Test
    void build_givenPartialFields_leavesUnsetFieldsNull() {
        TicketmasterAttractionResponse response = TicketmasterAttractionResponse.builder()
                .id("K8vZ9171ob7")
                .name("Eagles")
                .build();

        assertThat(response.id()).isEqualTo("K8vZ9171ob7");
        assertThat(response.name()).isEqualTo("Eagles");
        assertThat(response.externalLinks()).isNull();
        assertThat(response).isEqualTo(new TicketmasterAttractionResponse("K8vZ9171ob7", "Eagles", null));
    }

    @Test
    void build_givenBuilderReused_eachBuildCreatesIndependentRecord() {
        TicketmasterAttractionResponse.Builder builder = TicketmasterAttractionResponse.builder()
                .id("K8vZ9171ob7")
                .name("First");

        TicketmasterAttractionResponse first = builder.build();
        TicketmasterAttractionResponse second = builder.name("Second").build();

        assertThat(first.name()).isEqualTo("First");
        assertThat(second.name()).isEqualTo("Second");
        assertThat(first.id()).isEqualTo("K8vZ9171ob7");
        assertThat(second.id()).isEqualTo("K8vZ9171ob7");
    }

    @Test
    void builder_givenStaticFactoryMethod_returnsNewBuilderInstance() {
        TicketmasterAttractionResponse.Builder builder = TicketmasterAttractionResponse.builder();

        assertThat(builder).isNotNull();
        assertThat(builder).isInstanceOf(TicketmasterAttractionResponse.Builder.class);
    }

    @Test
    void build_givenMethodChaining_eachSetterReturnsSameBuilder() {
        TicketmasterAttractionResponse.Builder builder = TicketmasterAttractionResponse.builder();

        assertThat(builder.id("id")).isSameAs(builder);
        assertThat(builder.name("name")).isSameAs(builder);
        assertThat(builder.externalLinks(null)).isSameAs(builder);
    }

    @Test
    void build_givenBuiltRecord_isEqualToDirectConstructor() {
        Map<String, List<ExternalLink>> externalLinks = Map.of(
                "spotify", List.of(
                        new ExternalLink("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null)));

        TicketmasterAttractionResponse fromBuilder = TicketmasterAttractionResponse.builder()
                .id("K8vZ9171ob7")
                .name("Eagles")
                .externalLinks(externalLinks)
                .build();

        TicketmasterAttractionResponse fromConstructor = new TicketmasterAttractionResponse(
                "K8vZ9171ob7", "Eagles", externalLinks);

        assertThat(fromBuilder).isEqualTo(fromConstructor);
    }
}
