package com.mockhub.buyerpreference.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
import com.mockhub.ticket.dto.ListingSearchCriteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyerPreferenceServiceTest {

    @Mock
    private BuyerPreferencesRepository buyerPreferencesRepository;

    @Mock
    private UserRepository userRepository;

    private BuyerPreferenceService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new BuyerPreferenceService(buyerPreferencesRepository, userRepository);
        user = new User();
        user.setId(42L);
        user.setEmail("buyer@example.com");
        user.setFirstName("Buyer");
        user.setLastName("Example");
    }

    @Test
    @DisplayName("getPreferences - no stored preferences - returns empty defaults")
    void getPreferences_givenNoStoredPreferences_returnsEmptyDefaults() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.empty());

        BuyerPreferencesDto result = service.getPreferences("buyer@example.com");

        assertThat(result.id()).isNull();
        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.preferredArtists()).isEmpty();
        assertThat(result.riskTolerance()).isEqualTo(RiskTolerance.BEST_VALUE);
    }

    @Test
    @DisplayName("findStoredPreferencesByUserId - null user id - returns empty")
    void findStoredPreferencesByUserId_givenNullUserId_returnsEmpty() {
        Optional<BuyerPreferencesDto> result = service.findStoredPreferencesByUserId(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findStoredPreferencesByUserId - stored preferences - returns meaningful preferences")
    void findStoredPreferencesByUserId_givenStoredPreferences_returnsMeaningfulPreferences() {
        BuyerPreferences preferences = preferences();
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.of(preferences));

        Optional<BuyerPreferencesDto> result = service.findStoredPreferencesByUserId(42L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().preferredArtists()).containsExactly("Radiohead");
    }

    @Test
    @DisplayName("updatePreferences - new request - creates normalized preferences")
    void updatePreferences_givenNewRequest_createsNormalizedPreferences() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(buyerPreferencesRepository.saveAndFlush(any(BuyerPreferences.class)))
                .thenAnswer(invocation -> {
                    BuyerPreferences saved = invocation.getArgument(0);
                    saved.setId(7L);
                    saved.setCreatedAt(Instant.now());
                    saved.setUpdatedAt(Instant.now());
                    return saved;
                });

        UpdateBuyerPreferencesRequest request = new UpdateBuyerPreferencesRequest(
                List.of(" Radiohead ", "radiohead", "Beyonce"),
                List.of("concerts"),
                List.of(" Boston "),
                List.of("Blue Note"),
                List.of(" Orchestra "),
                List.of("Bad Venue"),
                List.of("sports"),
                new BigDecimal("150.555"),
                new BigDecimal("18.125"),
                true,
                " aisle seating ",
                RiskTolerance.LOW_RISK_ONLY,
                true);

        BuyerPreferencesDto result = service.updatePreferences("buyer@example.com", request);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.preferredArtists()).containsExactly("Radiohead", "Beyonce");
        assertThat(result.preferredCities()).containsExactly("Boston");
        assertThat(result.maxTotalPrice()).isEqualByComparingTo("150.56");
        assertThat(result.maxServiceFeePercent()).isEqualByComparingTo("18.13");
        assertThat(result.accessibilityNeeds()).isEqualTo("aisle seating");
        assertThat(result.riskTolerance()).isEqualTo(RiskTolerance.LOW_RISK_ONLY);

        ArgumentCaptor<BuyerPreferences> captor = ArgumentCaptor.forClass(BuyerPreferences.class);
        verify(buyerPreferencesRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("updatePreferences - null optional values - stores empty defaults")
    void updatePreferences_givenNullOptionalValues_storesEmptyDefaults() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(buyerPreferencesRepository.saveAndFlush(any(BuyerPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBuyerPreferencesRequest request = new UpdateBuyerPreferencesRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                "   ",
                null,
                false);

        BuyerPreferencesDto result = service.updatePreferences(" buyer@example.com ", request);

        assertThat(result.preferredArtists()).isEmpty();
        assertThat(result.maxTotalPrice()).isNull();
        assertThat(result.maxServiceFeePercent()).isNull();
        assertThat(result.accessibilityNeeds()).isNull();
        assertThat(result.riskTolerance()).isEqualTo(RiskTolerance.BEST_VALUE);
    }

    @Test
    @DisplayName("updatePreferences - concurrent create conflict - returns domain conflict")
    void updatePreferences_givenConcurrentCreateConflict_throwsConflictException() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(buyerPreferencesRepository.saveAndFlush(any(BuyerPreferences.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate user_id"));

        UpdateBuyerPreferencesRequest request = new UpdateBuyerPreferencesRequest(
                List.of("Radiohead"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                false);

        assertThatThrownBy(() -> service.updatePreferences("buyer@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Preferences already exist");
    }

    @Test
    @DisplayName("applyToSearchCriteria - stored preferences - fills missing filters")
    void applyToSearchCriteria_givenStoredPreferences_fillsMissingFilters() {
        BuyerPreferences preferences = preferences();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.of(preferences));

        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "jazz", null, null, null, null, null, null, null, 10);

        BuyerPreferenceApplication application =
                service.applyToSearchCriteria("buyer@example.com", criteria, null);

        assertThat(application.criteria().categorySlug()).isEqualTo("jazz");
        assertThat(application.criteria().city()).isEqualTo("Boston");
        assertThat(application.criteria().section()).isEqualTo("Orchestra");
        assertThat(application.criteria().maxPrice()).isEqualByComparingTo("150.00");
        assertThat(application.preferredSection()).isEqualTo("Orchestra");
        assertThat(application.context().preferencesAvailable()).isTrue();
        assertThat(application.context().appliedFilters()).contains("city=Boston", "section=Orchestra");
        assertThat(application.context().appliedFilters()).doesNotContain("preferredSection=Orchestra");
    }

    @Test
    @DisplayName("applyToSearchCriteria - existing filters - preserves criteria without marking filters applied")
    void applyToSearchCriteria_givenExistingFilters_preservesCriteriaWithoutAppliedFilters() {
        BuyerPreferences preferences = preferences();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.of(preferences));
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "jazz", "concerts", "New York", new BigDecimal("75.00"),
                new BigDecimal("300.00"), "Balcony", null, null, 10);

        BuyerPreferenceApplication application =
                service.applyToSearchCriteria("buyer@example.com", criteria, "Mezzanine");

        assertThat(application.criteria().categorySlug()).isEqualTo("concerts");
        assertThat(application.criteria().city()).isEqualTo("New York");
        assertThat(application.criteria().section()).isEqualTo("Balcony");
        assertThat(application.preferredSection()).isEqualTo("Mezzanine");
        assertThat(application.context().preferencesAvailable()).isTrue();
        assertThat(application.context().applied()).isFalse();
        assertThat(application.context().appliedFilters()).isEmpty();
    }

    @Test
    @DisplayName("applyToSearchCriteria - blank user email - returns original criteria")
    void applyToSearchCriteria_givenBlankUserEmail_returnsOriginalCriteria() {
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "jazz", "concerts", "Boston", null, null, null, null, null, 10);

        BuyerPreferenceApplication application = service.applyToSearchCriteria(" ", criteria, "Mezzanine");

        assertThat(application.criteria()).isSameAs(criteria);
        assertThat(application.preferredSection()).isEqualTo("Mezzanine");
        assertThat(application.context().preferencesAvailable()).isFalse();
    }

    @Test
    @DisplayName("applyToSearchCriteria - no meaningful preferences - returns original criteria")
    void applyToSearchCriteria_givenNoMeaningfulPreferences_returnsOriginalCriteria() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(buyerPreferencesRepository.findByUserId(42L)).thenReturn(Optional.empty());
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "jazz", "concerts", "Boston", null, null, null, null, null, 10);

        BuyerPreferenceApplication application =
                service.applyToSearchCriteria("buyer@example.com", criteria, null);

        assertThat(application.criteria()).isSameAs(criteria);
        assertThat(application.context().preferencesAvailable()).isFalse();
    }

    @Test
    @DisplayName("recommendationContext - stored preferences - summarizes without raw Spotify data")
    void recommendationContext_givenStoredPreferences_summarizesPreferences() {
        BuyerPreferencesDto preferences = new BuyerPreferencesDto(
                1L,
                42L,
                List.of("Radiohead"),
                List.of("concerts"),
                List.of("Boston"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new BigDecimal("150.00"),
                null,
                false,
                null,
                RiskTolerance.BEST_SEATS,
                false,
                Instant.now(),
                Instant.now());

        BuyerPreferenceContextDto context = service.recommendationContext(Optional.of(preferences));

        assertThat(context.preferencesAvailable()).isTrue();
        assertThat(context.rationale()).anySatisfy(reason -> assertThat(reason).contains("Radiohead"));
        assertThat(context.rationale()).anySatisfy(reason -> assertThat(reason).contains("BEST_SEATS"));
    }

    @Test
    @DisplayName("recommendationContext - empty preferences - returns no preference context")
    void recommendationContext_givenEmptyPreferences_returnsNone() {
        BuyerPreferenceContextDto context = service.recommendationContext(Optional.empty());

        assertThat(context.preferencesAvailable()).isFalse();
        assertThat(context.applied()).isFalse();
        assertThat(context.rationale()).isEmpty();
    }

    @Test
    @DisplayName("formatForPrompt - stored preferences - includes explicit preference signals")
    void formatForPrompt_givenStoredPreferences_includesExplicitPreferenceSignals() {
        BuyerPreferencesDto preferences = new BuyerPreferencesDto(
                1L,
                42L,
                List.of("Radiohead"),
                List.of("concerts"),
                List.of("Boston"),
                List.of("Blue Note"),
                List.of("Orchestra"),
                List.of("Bad Venue"),
                List.of("sports"),
                new BigDecimal("150.00"),
                new BigDecimal("18.00"),
                true,
                "aisle seating",
                RiskTolerance.LOW_RISK_ONLY,
                true,
                Instant.now(),
                Instant.now());

        String prompt = service.formatForPrompt(Optional.of(preferences));

        assertThat(prompt)
                .contains("Explicit ticket-shopping preferences")
                .contains("Preferred artists: Radiohead")
                .contains("Disliked venues: Bad Venue")
                .contains("Maximum service fee tolerance: 18.00%")
                .contains("They prefer all-in price explanations")
                .contains("Accessibility needs: aisle seating")
                .contains("Risk tolerance: LOW_RISK_ONLY")
                .contains("They are willing to wait for price drops");
    }

    @Test
    @DisplayName("formatForPrompt - no preferences - returns empty context")
    void formatForPrompt_givenNoPreferences_returnsEmptyContext() {
        String prompt = service.formatForPrompt(Optional.empty());

        assertThat(prompt).isEmpty();
    }

    private BuyerPreferences preferences() {
        BuyerPreferences preferences = new BuyerPreferences();
        preferences.setId(1L);
        preferences.setUser(user);
        preferences.setPreferredArtists(List.of("Radiohead"));
        preferences.setPreferredCategories(List.of("jazz"));
        preferences.setPreferredCities(List.of("Boston"));
        preferences.setPreferredVenues(List.of("Blue Note"));
        preferences.setPreferredSections(List.of("Orchestra"));
        preferences.setDislikedVenues(List.of());
        preferences.setDislikedCategories(List.of());
        preferences.setMaxTotalPrice(new BigDecimal("150.00"));
        preferences.setMaxServiceFeePercent(null);
        preferences.setRiskTolerance(RiskTolerance.LOW_RISK_ONLY);
        preferences.setCreatedAt(Instant.now());
        preferences.setUpdatedAt(Instant.now());
        return preferences;
    }
}
