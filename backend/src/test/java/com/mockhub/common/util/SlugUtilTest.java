package com.mockhub.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SlugUtil")
class SlugUtilTest {

    @Test
    @DisplayName("toSlug - given normal text - returns lowercase hyphenated slug")
    void toSlug_givenNormalText_returnsLowercaseHyphenatedSlug() {
        assertEquals("taylor-swift-the-eras-tour", SlugUtil.toSlug("Taylor Swift The Eras Tour"));
    }

    @Test
    @DisplayName("toSlug - given punctuation and special characters - strips non-word characters and collapses hyphens")
    void toSlug_givenPunctuationAndSpecialCharacters_stripsAndCollapsesHyphens() {
        assertEquals("rock-roll-2026-live-at-msg", SlugUtil.toSlug("Rock & Roll (2026) — Live at MSG!"));
    }

    @Test
    @DisplayName("toSlug - given accented characters and extra whitespace - normalizes and trims")
    void toSlug_givenAccentedCharactersAndExtraWhitespace_normalizesAndTrims() {
        assertEquals("beyonce-renaissance-tour", SlugUtil.toSlug("  Beyoncé   --  Renaissance Tour  "));
    }

    @Test
    @DisplayName("toSlug - given null input - returns empty string")
    void toSlug_givenNullInput_returnsEmptyString() {
        assertEquals("", SlugUtil.toSlug(null));
    }

    @Test
    @DisplayName("toSlug - given blank input - returns empty string")
    void toSlug_givenBlankInput_returnsEmptyString() {
        assertEquals("", SlugUtil.toSlug("   "));
    }
}
