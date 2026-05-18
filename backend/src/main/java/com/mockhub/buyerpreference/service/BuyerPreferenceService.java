package com.mockhub.buyerpreference.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.buyerpreference.dto.BuyerPreferenceApplication;
import com.mockhub.buyerpreference.dto.BuyerPreferenceContextDto;
import com.mockhub.buyerpreference.dto.BuyerPreferencesDto;
import com.mockhub.buyerpreference.dto.UpdateBuyerPreferencesRequest;
import com.mockhub.buyerpreference.entity.BuyerPreferences;
import com.mockhub.buyerpreference.entity.RiskTolerance;
import com.mockhub.buyerpreference.repository.BuyerPreferencesRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.ticket.dto.ListingSearchCriteria;

@Service
public class BuyerPreferenceService {

    private static final String USER_RESOURCE = "User";
    private static final String EMAIL_FIELD = "email";
    private static final int MAX_LIST_ITEMS = 20;

    private final BuyerPreferencesRepository buyerPreferencesRepository;
    private final UserRepository userRepository;

    public BuyerPreferenceService(BuyerPreferencesRepository buyerPreferencesRepository,
                                  UserRepository userRepository) {
        this.buyerPreferencesRepository = buyerPreferencesRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BuyerPreferencesDto getPreferences(String userEmail) {
        User user = resolveUser(userEmail);
        return buyerPreferencesRepository.findByUserId(user.getId())
                .map(this::toDto)
                .orElseGet(() -> emptyPreferences(user));
    }

    @Transactional
    public BuyerPreferencesDto updatePreferences(String userEmail, UpdateBuyerPreferencesRequest request) {
        User user = resolveUser(userEmail);
        BuyerPreferences preferences = buyerPreferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    BuyerPreferences created = new BuyerPreferences();
                    created.setUser(user);
                    return created;
                });

        applyRequest(preferences, request);
        try {
            return toDto(buyerPreferencesRepository.saveAndFlush(preferences));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Preferences already exist for this user; retry the update.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<BuyerPreferencesDto> findStoredPreferencesByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return buyerPreferencesRepository.findByUserId(userId)
                .map(this::toDto)
                .filter(this::hasAnyPreference);
    }

    @Transactional(readOnly = true)
    public BuyerPreferenceApplication applyToSearchCriteria(String userEmail,
                                                            ListingSearchCriteria criteria,
                                                            String preferredSection) {
        if (isBlank(userEmail)) {
            return new BuyerPreferenceApplication(criteria, preferredSection, BuyerPreferenceContextDto.none());
        }

        BuyerPreferencesDto preferences = getPreferences(userEmail);
        if (!hasAnyPreference(preferences)) {
            return new BuyerPreferenceApplication(criteria, preferredSection, BuyerPreferenceContextDto.none());
        }

        List<String> appliedFilters = new ArrayList<>();
        List<String> rationale = new ArrayList<>();

        String category = firstNonBlank(criteria.categorySlug(),
                first(preferences.preferredCategories()));
        if (isBlank(criteria.categorySlug()) && !isBlank(category)) {
            appliedFilters.add("category=" + category);
            rationale.add("Applied preferred category '" + category + "'");
        }

        String city = firstNonBlank(criteria.city(), first(preferences.preferredCities()));
        if (isBlank(criteria.city()) && !isBlank(city)) {
            appliedFilters.add("city=" + city);
            rationale.add("Applied preferred city '" + city + "'");
        }

        String section = firstNonBlank(criteria.section(), first(preferences.preferredSections()));
        boolean appliedSectionFilter = isBlank(criteria.section()) && !isBlank(section);
        if (appliedSectionFilter) {
            appliedFilters.add("section=" + section);
            rationale.add("Applied preferred section '" + section + "'");
        }

        BigDecimal maxPrice = criteria.maxPrice();
        if (maxPrice == null && preferences.maxTotalPrice() != null) {
            maxPrice = preferences.maxTotalPrice();
            appliedFilters.add("maxPrice=" + maxPrice);
            rationale.add("Applied maximum all-in ticket price $" + maxPrice);
        }

        String effectivePreferredSection = firstNonBlank(preferredSection, first(preferences.preferredSections()));
        if (isBlank(preferredSection) && !isBlank(effectivePreferredSection) && !appliedSectionFilter) {
            appliedFilters.add("preferredSection=" + effectivePreferredSection);
            rationale.add("Ranked toward preferred section '" + effectivePreferredSection + "'");
        }

        if (preferences.riskTolerance() != null) {
            rationale.add("Risk tolerance: " + preferences.riskTolerance().name());
        }
        if (preferences.allInPriceOnly()) {
            rationale.add("User prefers all-in price framing");
        }
        if (preferences.willingToWaitForPriceDrops()) {
            rationale.add("User is willing to wait for price drops");
        }

        ListingSearchCriteria appliedCriteria = new ListingSearchCriteria(
                criteria.query(),
                category,
                city,
                criteria.minPrice(),
                maxPrice,
                section,
                criteria.dateFrom(),
                criteria.dateTo(),
                criteria.limit());

        BuyerPreferenceContextDto context = new BuyerPreferenceContextDto(
                true,
                !appliedFilters.isEmpty(),
                List.copyOf(appliedFilters),
                List.copyOf(rationale));
        return new BuyerPreferenceApplication(appliedCriteria, effectivePreferredSection, context);
    }

    public BuyerPreferenceContextDto recommendationContext(Optional<BuyerPreferencesDto> preferences) {
        if (preferences.isEmpty() || !hasAnyPreference(preferences.get())) {
            return BuyerPreferenceContextDto.none();
        }

        BuyerPreferencesDto dto = preferences.get();
        List<String> rationale = new ArrayList<>();
        addListRationale(rationale, "preferred artists", dto.preferredArtists());
        addListRationale(rationale, "preferred categories", dto.preferredCategories());
        addListRationale(rationale, "preferred cities", dto.preferredCities());
        addListRationale(rationale, "preferred venues", dto.preferredVenues());
        addListRationale(rationale, "preferred sections", dto.preferredSections());
        addListRationale(rationale, "disliked venues", dto.dislikedVenues());
        addListRationale(rationale, "disliked categories", dto.dislikedCategories());
        if (dto.maxTotalPrice() != null) {
            rationale.add("maximum all-in ticket price $" + dto.maxTotalPrice());
        }
        if (dto.maxServiceFeePercent() != null) {
            rationale.add("maximum service fee tolerance " + dto.maxServiceFeePercent() + "%");
        }
        if (!isBlank(dto.accessibilityNeeds())) {
            rationale.add("accessibility needs provided");
        }
        if (dto.riskTolerance() != null) {
            rationale.add("risk tolerance " + dto.riskTolerance().name());
        }
        if (dto.willingToWaitForPriceDrops()) {
            rationale.add("willing to wait for price drops");
        }

        return new BuyerPreferenceContextDto(true, true, List.of(), List.copyOf(rationale));
    }

    public String formatForPrompt(Optional<BuyerPreferencesDto> preferences) {
        if (preferences.isEmpty() || !hasAnyPreference(preferences.get())) {
            return "";
        }

        BuyerPreferencesDto dto = preferences.get();
        StringBuilder context = new StringBuilder("Explicit ticket-shopping preferences:\n");
        appendList(context, "Preferred artists", dto.preferredArtists());
        appendList(context, "Preferred categories", dto.preferredCategories());
        appendList(context, "Preferred cities", dto.preferredCities());
        appendList(context, "Preferred venues", dto.preferredVenues());
        appendList(context, "Preferred sections or seat types", dto.preferredSections());
        appendList(context, "Disliked venues", dto.dislikedVenues());
        appendList(context, "Disliked categories", dto.dislikedCategories());
        if (dto.maxTotalPrice() != null) {
            context.append("Maximum all-in ticket price: $").append(dto.maxTotalPrice()).append(".\n");
        }
        if (dto.maxServiceFeePercent() != null) {
            context.append("Maximum service fee tolerance: ")
                    .append(dto.maxServiceFeePercent()).append("%.\n");
        }
        if (dto.allInPriceOnly()) {
            context.append("They prefer all-in price explanations.\n");
        }
        if (!isBlank(dto.accessibilityNeeds())) {
            context.append("Accessibility needs: ").append(dto.accessibilityNeeds()).append(".\n");
        }
        if (dto.riskTolerance() != null) {
            context.append("Risk tolerance: ").append(dto.riskTolerance().name()).append(".\n");
        }
        if (dto.willingToWaitForPriceDrops()) {
            context.append("They are willing to wait for price drops.\n");
        }
        return context.toString();
    }

    private void applyRequest(BuyerPreferences preferences, UpdateBuyerPreferencesRequest request) {
        preferences.setPreferredArtists(cleanList(request.preferredArtists()));
        preferences.setPreferredCategories(cleanList(request.preferredCategories()));
        preferences.setPreferredCities(cleanList(request.preferredCities()));
        preferences.setPreferredVenues(cleanList(request.preferredVenues()));
        preferences.setPreferredSections(cleanList(request.preferredSections()));
        preferences.setDislikedVenues(cleanList(request.dislikedVenues()));
        preferences.setDislikedCategories(cleanList(request.dislikedCategories()));
        preferences.setMaxTotalPrice(normalizeAmount(request.maxTotalPrice()));
        preferences.setMaxServiceFeePercent(normalizePercent(request.maxServiceFeePercent()));
        preferences.setAllInPriceOnly(request.allInPriceOnly());
        preferences.setAccessibilityNeeds(normalizeText(request.accessibilityNeeds()));
        preferences.setRiskTolerance(request.riskTolerance() != null
                ? request.riskTolerance()
                : RiskTolerance.BEST_VALUE);
        preferences.setWillingToWaitForPriceDrops(request.willingToWaitForPriceDrops());
    }

    private BuyerPreferencesDto toDto(BuyerPreferences preferences) {
        return new BuyerPreferencesDto(
                preferences.getId(),
                preferences.getUser().getId(),
                copyList(preferences.getPreferredArtists()),
                copyList(preferences.getPreferredCategories()),
                copyList(preferences.getPreferredCities()),
                copyList(preferences.getPreferredVenues()),
                copyList(preferences.getPreferredSections()),
                copyList(preferences.getDislikedVenues()),
                copyList(preferences.getDislikedCategories()),
                preferences.getMaxTotalPrice(),
                preferences.getMaxServiceFeePercent(),
                preferences.isAllInPriceOnly(),
                preferences.getAccessibilityNeeds(),
                preferences.getRiskTolerance(),
                preferences.isWillingToWaitForPriceDrops(),
                preferences.getCreatedAt(),
                preferences.getUpdatedAt());
    }

    private BuyerPreferencesDto emptyPreferences(User user) {
        return new BuyerPreferencesDto(
                null,
                user.getId(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                false,
                null,
                RiskTolerance.BEST_VALUE,
                false,
                null,
                null);
    }

    private boolean hasAnyPreference(BuyerPreferencesDto preferences) {
        return !preferences.preferredArtists().isEmpty()
                || !preferences.preferredCategories().isEmpty()
                || !preferences.preferredCities().isEmpty()
                || !preferences.preferredVenues().isEmpty()
                || !preferences.preferredSections().isEmpty()
                || !preferences.dislikedVenues().isEmpty()
                || !preferences.dislikedCategories().isEmpty()
                || preferences.maxTotalPrice() != null
                || preferences.maxServiceFeePercent() != null
                || preferences.allInPriceOnly()
                || !isBlank(preferences.accessibilityNeeds())
                || preferences.riskTolerance() != RiskTolerance.BEST_VALUE
                || preferences.willingToWaitForPriceDrops();
    }

    private User resolveUser(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("User email is required");
        }
        return userRepository.findByEmail(email.strip())
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, EMAIL_FIELD, email.strip()));
    }

    private List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (String value : values) {
            if (!isBlank(value)) {
                String stripped = value.strip();
                normalized.putIfAbsent(stripped.toLowerCase(Locale.ROOT), stripped);
            }
            if (normalized.size() >= MAX_LIST_ITEMS) {
                break;
            }
        }
        return List.copyOf(normalized.values());
    }

    private List<String> copyList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String normalizeText(String value) {
        return isBlank(value) ? null : value.strip();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePercent(BigDecimal percent) {
        return percent == null ? null : percent.setScale(2, RoundingMode.HALF_UP);
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private void addListRationale(List<String> rationale, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            rationale.add(label + ": " + String.join(", ", values));
        }
    }

    private void appendList(StringBuilder context, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            context.append(label).append(": ").append(String.join(", ", values)).append(".\n");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
