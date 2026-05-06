package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.dto.PriceWarningDto;
import com.mockhub.ticket.dto.TicketComparisonOptionDto;
import com.mockhub.ticket.dto.TicketComparisonResponseDto;
import com.mockhub.ticket.dto.TicketSearchResultDto;

@Service
public class TicketComparisonService {

    private static final String JUDGMENT_SOURCE = "DETERMINISTIC_HEURISTIC";
    private static final String PRICE_WARNING_SOURCE = "RETURNED_MARKET_SET";
    private static final BigDecimal HIGH_PRICE_MULTIPLIER = new BigDecimal("1.50");
    private static final BigDecimal LOW_PRICE_MULTIPLIER = new BigDecimal("0.60");

    private final ListingService listingService;

    public TicketComparisonService(ListingService listingService) {
        this.listingService = listingService;
    }

    @Transactional(readOnly = true)
    public TicketComparisonResponseDto compareTickets(ListingSearchCriteria criteria, String preferredSection) {
        List<TicketSearchResultDto> listings = listingService.searchTickets(criteria);
        if (listings.isEmpty()) {
            return new TicketComparisonResponseDto(
                    0,
                    JUDGMENT_SOURCE,
                    "No matching listings were found for the supplied search criteria.",
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of()
            );
        }

        BigDecimal medianPrice = medianPrice(listings);
        Map<Long, List<String>> warningCodesByListingId = warningCodesByListingId(listings, medianPrice);
        List<PriceWarningDto> priceWarnings = priceWarnings(listings, medianPrice, warningCodesByListingId);

        List<ScoredListing> scoredListings = listings.stream()
                .map(listing -> scoreListing(listing, medianPrice, preferredSection, warningCodesByListingId))
                .sorted(Comparator.comparing(ScoredListing::score).reversed()
                        .thenComparing(scored -> scored.listing().price(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(scored -> scored.listing().listingId(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<TicketComparisonOptionDto> rankedOptions = rankedOptions(scoredListings);
        TicketComparisonOptionDto cheapestOption = cheapestOption(rankedOptions);
        TicketComparisonOptionDto bestValueOption = rankedOptions.getFirst();
        TicketComparisonOptionDto bestSectionOption = bestSectionOption(rankedOptions, preferredSection);
        TicketComparisonOptionDto lowestRiskOption = lowestRiskOption(rankedOptions);

        return new TicketComparisonResponseDto(
                listings.size(),
                JUDGMENT_SOURCE,
                "Rankings combine objective listing data with deterministic heuristics for price, section quality, "
                        + "seller risk, and price plausibility.",
                cheapestOption,
                bestValueOption,
                bestSectionOption,
                lowestRiskOption,
                rankedOptions,
                priceWarnings
        );
    }

    private ScoredListing scoreListing(TicketSearchResultDto listing, BigDecimal medianPrice, String preferredSection,
                                       Map<Long, List<String>> warningCodesByListingId) {
        int priceScore = priceScore(listing.price(), medianPrice);
        int sectionScore = sectionScore(listing.sectionName(), preferredSection);
        int riskScore = riskScore(listing, warningCodesByListingId.getOrDefault(listing.listingId(), List.of()));
        int warningPenalty = warningPenalty(warningCodesByListingId.getOrDefault(listing.listingId(), List.of()));
        int totalScore = Math.max(0, Math.min(100, priceScore + sectionScore + riskScore - warningPenalty));

        List<String> reasonCodes = reasonCodes(listing, preferredSection, priceScore, sectionScore, riskScore);
        List<String> warnings = warningCodesByListingId.getOrDefault(listing.listingId(), List.of());
        return new ScoredListing(listing, totalScore, reasonCodes, warnings, rationale(reasonCodes, warnings));
    }

    private int priceScore(BigDecimal price, BigDecimal medianPrice) {
        if (price == null || medianPrice == null || medianPrice.compareTo(BigDecimal.ZERO) == 0) {
            return 25;
        }
        BigDecimal ratio = price.divide(medianPrice, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.80")) <= 0) {
            return 45;
        }
        if (ratio.compareTo(BigDecimal.ONE) <= 0) {
            return 38;
        }
        if (ratio.compareTo(new BigDecimal("1.20")) <= 0) {
            return 28;
        }
        if (ratio.compareTo(HIGH_PRICE_MULTIPLIER) <= 0) {
            return 18;
        }
        return 8;
    }

    private int sectionScore(String sectionName, String preferredSection) {
        if (sectionName == null || sectionName.isBlank()) {
            return 8;
        }

        String normalizedSection = normalize(sectionName);
        if (preferredSection != null && !preferredSection.isBlank()) {
            String normalizedPreferred = normalize(preferredSection);
            if (normalizedSection.equals(normalizedPreferred)) {
                return 35;
            }
            if (normalizedSection.contains(normalizedPreferred) || normalizedPreferred.contains(normalizedSection)) {
                return 28;
            }
        }

        if (containsAny(normalizedSection, "floor", "orchestra", "front", "lower", "club", "pit")) {
            return 28;
        }
        if (containsAny(normalizedSection, "mezzanine", "middle", "loge")) {
            return 20;
        }
        if (containsAny(normalizedSection, "balcony", "upper", "rear")) {
            return 12;
        }
        return 16;
    }

    private int riskScore(TicketSearchResultDto listing, List<String> warnings) {
        int score = listing.sellerDisplayName() == null ? 20 : 14;
        if (warnings.isEmpty()) {
            score += 8;
        }
        if ("STANDARD".equalsIgnoreCase(listing.ticketType())) {
            score += 4;
        }
        return score;
    }

    private int warningPenalty(List<String> warnings) {
        int penalty = 0;
        if (warnings.contains("HIGH_PRICE_ANOMALY")) {
            penalty += 18;
        }
        if (warnings.contains("LOW_PRICE_OUTLIER")) {
            penalty += 8;
        }
        if (warnings.contains("MARKET_DATA_LIMITED")) {
            penalty += 4;
        }
        return penalty;
    }

    private List<String> reasonCodes(TicketSearchResultDto listing, String preferredSection, int priceScore,
                                     int sectionScore, int riskScore) {
        List<String> reasonCodes = new ArrayList<>();
        if (priceScore >= 38) {
            reasonCodes.add("COMPETITIVE_PRICE");
        } else if (priceScore <= 18) {
            reasonCodes.add("PRICE_ABOVE_MARKET");
        }

        if (preferredSection != null && !preferredSection.isBlank()
                && listing.sectionName() != null
                && normalize(listing.sectionName()).contains(normalize(preferredSection))) {
            reasonCodes.add("MATCHES_REQUESTED_SECTION");
        } else if (sectionScore >= 28) {
            reasonCodes.add("STRONG_SECTION");
        }

        if (riskScore >= 28) {
            reasonCodes.add("LOWER_MARKETPLACE_RISK");
        }
        if (listing.commercePolicyUrl() != null) {
            reasonCodes.add("POLICY_AVAILABLE");
        }
        if (reasonCodes.isEmpty()) {
            reasonCodes.add("MATCHES_SEARCH_CRITERIA");
        }
        return List.copyOf(reasonCodes);
    }

    private String rationale(List<String> reasonCodes, List<String> warnings) {
        StringBuilder rationale = new StringBuilder("Deterministic ranking based on ");
        rationale.append(String.join(", ", reasonCodes).toLowerCase(Locale.ROOT).replace('_', ' '));
        if (!warnings.isEmpty()) {
            rationale.append("; warnings: ")
                    .append(String.join(", ", warnings).toLowerCase(Locale.ROOT).replace('_', ' '));
        }
        rationale.append('.');
        return rationale.toString();
    }

    private List<TicketComparisonOptionDto> rankedOptions(List<ScoredListing> scoredListings) {
        List<TicketComparisonOptionDto> options = new ArrayList<>();
        for (int index = 0; index < scoredListings.size(); index++) {
            ScoredListing scored = scoredListings.get(index);
            options.add(new TicketComparisonOptionDto(
                    index + 1,
                    scored.listing(),
                    scored.score(),
                    scored.reasonCodes(),
                    scored.warnings(),
                    scored.rationale()
            ));
        }
        return List.copyOf(options);
    }

    private TicketComparisonOptionDto cheapestOption(List<TicketComparisonOptionDto> options) {
        return options.stream()
                .filter(option -> option.listing().price() != null)
                .min(Comparator.comparing(option -> option.listing().price()))
                .orElse(options.getFirst());
    }

    private TicketComparisonOptionDto bestSectionOption(List<TicketComparisonOptionDto> options,
                                                        String preferredSection) {
        return options.stream()
                .max(Comparator.comparing((TicketComparisonOptionDto option) ->
                                sectionScore(option.listing().sectionName(), preferredSection))
                        .thenComparing(TicketComparisonOptionDto::score)
                        .thenComparing(option -> option.listing().price(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .orElse(options.getFirst());
    }

    private TicketComparisonOptionDto lowestRiskOption(List<TicketComparisonOptionDto> options) {
        return options.stream()
                .max(Comparator.comparing((TicketComparisonOptionDto option) ->
                                riskScore(option.listing(), option.warnings()))
                        .thenComparing(TicketComparisonOptionDto::score)
                        .thenComparing(option -> option.listing().price(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .orElse(options.getFirst());
    }

    private BigDecimal medianPrice(List<TicketSearchResultDto> listings) {
        List<BigDecimal> prices = listings.stream()
                .map(TicketSearchResultDto::price)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (prices.isEmpty()) {
            return null;
        }
        int middle = prices.size() / 2;
        if (prices.size() % 2 == 1) {
            return prices.get(middle);
        }
        return prices.get(middle - 1).add(prices.get(middle)).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private Map<Long, List<String>> warningCodesByListingId(List<TicketSearchResultDto> listings,
                                                            BigDecimal medianPrice) {
        Map<Long, List<String>> warnings = new HashMap<>();
        boolean limitedMarketData = listings.stream().filter(listing -> listing.price() != null).count() < 3;

        for (TicketSearchResultDto listing : listings) {
            List<String> listingWarnings = new ArrayList<>();
            if (limitedMarketData) {
                listingWarnings.add("MARKET_DATA_LIMITED");
            }
            if (listing.price() != null && medianPrice != null && medianPrice.compareTo(BigDecimal.ZERO) > 0) {
                if (listing.price().compareTo(medianPrice.multiply(HIGH_PRICE_MULTIPLIER)) > 0) {
                    listingWarnings.add("HIGH_PRICE_ANOMALY");
                } else if (listing.price().compareTo(medianPrice.multiply(LOW_PRICE_MULTIPLIER)) < 0) {
                    listingWarnings.add("LOW_PRICE_OUTLIER");
                }
            }
            warnings.put(listing.listingId(), List.copyOf(listingWarnings));
        }
        return Map.copyOf(warnings);
    }

    private List<PriceWarningDto> priceWarnings(List<TicketSearchResultDto> listings, BigDecimal medianPrice,
                                                Map<Long, List<String>> warningCodesByListingId) {
        List<PriceWarningDto> warnings = new ArrayList<>();
        for (TicketSearchResultDto listing : listings) {
            for (String code : warningCodesByListingId.getOrDefault(listing.listingId(), List.of())) {
                warnings.add(new PriceWarningDto(
                        listing.listingId(),
                        code,
                        warningMessage(code, listing, medianPrice),
                        PRICE_WARNING_SOURCE
                ));
            }
        }
        return List.copyOf(warnings);
    }

    private String warningMessage(String code, TicketSearchResultDto listing, BigDecimal medianPrice) {
        return switch (code) {
            case "HIGH_PRICE_ANOMALY" -> "Listing price is more than 150% of the returned market median "
                    + formatMedian(medianPrice) + ".";
            case "LOW_PRICE_OUTLIER" -> "Listing price is less than 60% of the returned market median "
                    + formatMedian(medianPrice) + "; confirm ticket details before purchase.";
            case "MARKET_DATA_LIMITED" -> "Fewer than three comparable prices were returned, so price plausibility "
                    + "is based on limited market data.";
            default -> "Review listing " + listing.listingId() + " before purchase.";
        };
    }

    private String formatMedian(BigDecimal medianPrice) {
        if (medianPrice == null) {
            return "for this search";
        }
        return "$" + medianPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private record ScoredListing(
            TicketSearchResultDto listing,
            int score,
            List<String> reasonCodes,
            List<String> warnings,
            String rationale
    ) {
    }
}
