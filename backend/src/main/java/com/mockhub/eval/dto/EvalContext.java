package com.mockhub.eval.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.cart.dto.CartDto;
import com.mockhub.event.entity.Event;
import com.mockhub.ticket.entity.Listing;

public record EvalContext(
        Event event,
        Listing listing,
        List<RecommendationDto> recommendations,
        CartDto cart,
        String aiResponse,
        String userQuery,
        BigDecimal predictedPrice,
        BigDecimal currentPrice,
        String agentId,
        String userEmail,
        BigDecimal orderTotal,
        String categorySlug
) {

    public static EvalContext forEvent(Event event) {
        return new EvalContext(event, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static EvalContext forListing(Listing listing) {
        return new EvalContext(null, listing, null, null, null, null, null, null, null, null, null, null);
    }

    public static EvalContext forEventAndListing(Event event, Listing listing) {
        return new EvalContext(event, listing, null, null, null, null, null, null, null, null, null, null);
    }

    public static EvalContext forChat(String aiResponse, String userQuery) {
        return new EvalContext(null, null, null, null, aiResponse, userQuery, null, null, null, null, null, null);
    }

    public static EvalContext forRecommendations(List<RecommendationDto> recommendations) {
        return new EvalContext(null, null, recommendations, null, null, null, null, null, null, null, null, null);
    }

    public static EvalContext forCart(CartDto cart) {
        return new EvalContext(null, null, null, cart, null, null, null, null, null, null, null, null);
    }

    public static EvalContext forPricePrediction(BigDecimal predictedPrice, BigDecimal currentPrice) {
        return new EvalContext(null, null, null, null, null, null, predictedPrice, currentPrice, null, null, null, null);
    }

    public static EvalContext forAgentAction(String agentId, String userEmail, Event event,
                                             Listing listing, BigDecimal orderTotal, String categorySlug) {
        return new EvalContext(event, listing, null, null, null, null, null, null,
                agentId, userEmail, orderTotal, categorySlug);
    }
}
