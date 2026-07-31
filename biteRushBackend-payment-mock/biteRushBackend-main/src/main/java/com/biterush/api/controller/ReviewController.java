package com.biterush.api.controller;

import com.biterush.api.dto.ReviewDTO;
import com.biterush.api.entity.Order;
import com.biterush.api.entity.Review;
import com.biterush.api.entity.User;
import com.biterush.api.exception.ResourceNotFoundException;
import com.biterush.api.repository.OrderRepository;
import com.biterush.api.repository.ReviewRepository;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.util.PaginationUtil;
import com.biterush.api.util.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Review> createReview(
            @PathVariable Long orderId,
            @Valid @RequestBody ReviewDTO dto) {

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = new Review();
        review.setOrder(order);
        review.setRestaurant(order.getRestaurant());
        review.setReviewer(user);
        review.setRating(dto.rating);
        review.setComment(dto.comment);

        Review saved = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<PageResponse<Review>> getRestaurantReviews(
            @PathVariable Long restaurantId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Review> reviews = reviewRepository.findByRestaurantId(restaurantId, pageable);
        return ResponseEntity.ok(PaginationUtil.mapPageToResponse(reviews));
    }

    @GetMapping("/restaurants/{restaurantId}/average")
    public ResponseEntity<Double> getRestaurantAverageRating(@PathVariable Long restaurantId) {
        Page<Review> reviews = reviewRepository.findByRestaurantId(restaurantId, Pageable.unpaged());
        double average = reviews.getContent().stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);
        return ResponseEntity.ok(average);
    }
}
