package com.open.spring.mvc.donation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

/**
 * RESTful API controller for the Hunger Heroes donation system.
 *
 * <p>Exposes endpoints that mirror the Flask backend's donation API so the
 * frontend can talk to <em>either</em> backend interchangeably.  All endpoints
 * are prefixed with {@code /api/donations}.</p>
 *
 * <p><strong>HTTP Methods &amp; Status Codes:</strong></p>
 * <ul>
 *   <li>{@code POST}   → 201 Created</li>
 *   <li>{@code GET}    → 200 OK</li>
 *   <li>{@code PUT}    → 200 OK</li>
 *   <li>{@code DELETE} → 204 No Content / 200 OK</li>
 *   <li>Validation errors → 400 Bad Request</li>
 *   <li>Not found        → 404 Not Found</li>
 *   <li>Conflict          → 409 Conflict</li>
 * </ul>
 *
 * @author Ahaan
 * @version 1.0
 */
@RestController
@RequestMapping("/api/donations")
public class DonationApiController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private PersonJpaRepository personRepo;

    // ───────────────────────────────────────────────
    //  POST /api/donations  — Create a donation
    // ───────────────────────────────────────────────

    /**
     * Creates a new food donation.
     *
     * <p>Accepts a JSON body with food details, donor info, and optional
     * allergen/dietary tags. If the user is authenticated, the donation
     * is linked to their account.</p>
     *
     * @param body        the request payload
     * @param userDetails the authenticated user (may be {@code null})
     * @return 201 with the created donation, or 400 on validation error
     */
    @PostMapping
    public ResponseEntity<?> createDonation(@RequestBody Map<String, Object> body,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Donation d = mapRequestToDonation(body);

            // Link to authenticated person if available
            if (userDetails != null) {
                Person person = personRepo.findByUid(userDetails.getUsername());
                if (person == null) {
                    person = personRepo.findByEmail(userDetails.getUsername());
                }
                d.setPerson(person);
            }

            Donation saved = donationService.createDonation(d);
            return new ResponseEntity<>(donationToMap(saved), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations  — List donations
    // ───────────────────────────────────────────────

    /**
     * Lists donations. Supports optional query parameters:
     * <ul>
     *   <li>{@code status} — filter by lifecycle status</li>
     *   <li>{@code mine=true} — list only the current user's donations</li>
     *   <li>{@code search} — search by food name</li>
     * </ul>
     *
     * @param status      optional status filter
     * @param mine        if "true", return only the user's donations
     * @param search      optional food-name search term
     * @param userDetails the authenticated user
     * @return 200 with a list of donations
     */
    @GetMapping
    public ResponseEntity<?> listDonations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mine,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<Donation> results;

        if (search != null && !search.isBlank()) {
            results = donationService.search(search);
        } else if ("true".equalsIgnoreCase(mine) && userDetails != null) {
            Person person = personRepo.findByUid(userDetails.getUsername());
            if (person == null) person = personRepo.findByEmail(userDetails.getUsername());
            results = (person != null) ? donationService.listByPerson(person.getId()) : List.of();
        } else {
            results = donationService.listDonations(status);
        }

        List<Map<String, Object>> response = results.stream()
                .map(this::donationToMap)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/{donationId}  — Get by ID
    // ───────────────────────────────────────────────

    /**
     * Retrieves a single donation by its human-readable ID.
     *
     * @param donationId the external donation ID (e.g. "HH-M3X7K9-AB2F")
     * @return 200 with the donation, or 404 if not found
     */
    @GetMapping("/{donationId}")
    public ResponseEntity<?> getDonation(@PathVariable String donationId) {
        return donationService.findByDonationId(donationId)
                .map(d -> ResponseEntity.ok(donationToMap(d)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Donation not found: " + donationId)));
    }

    // ───────────────────────────────────────────────
    //  POST /api/donations/{donationId}/accept
    // ───────────────────────────────────────────────

    /**
     * Marks a donation as accepted.
     *
     * @param donationId  the external donation ID
     * @param body        optional body with "accepted_by"
     * @param userDetails the authenticated user
     * @return 200 with the updated donation
     */
    @PostMapping("/{donationId}/accept")
    public ResponseEntity<?> acceptDonation(@PathVariable String donationId,
                                            @RequestBody(required = false) Map<String, Object> body,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String acceptedBy = "anonymous";
            if (body != null && body.containsKey("accepted_by")) {
                acceptedBy = body.get("accepted_by").toString();
            } else if (userDetails != null) {
                acceptedBy = userDetails.getUsername();
            }
            Donation updated = donationService.acceptDonation(donationId, acceptedBy);
            return ResponseEntity.ok(Map.of(
                "message", "Donation accepted",
                "donation_id", updated.getDonationId(),
                "status", updated.getStatus()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    //  POST /api/donations/{donationId}/deliver
    // ───────────────────────────────────────────────

    /**
     * Marks a donation as delivered.
     *
     * @param donationId  the external donation ID
     * @param body        optional body with "delivered_by"
     * @param userDetails the authenticated user
     * @return 200 with the updated donation
     */
    @PostMapping("/{donationId}/deliver")
    public ResponseEntity<?> deliverDonation(@PathVariable String donationId,
                                             @RequestBody(required = false) Map<String, Object> body,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String deliveredBy = "anonymous";
            if (body != null && body.containsKey("delivered_by")) {
                deliveredBy = body.get("delivered_by").toString();
            } else if (userDetails != null) {
                deliveredBy = userDetails.getUsername();
            }
            Donation updated = donationService.deliverDonation(donationId, deliveredBy);
            return ResponseEntity.ok(Map.of(
                "message", "Donation delivered",
                "donation_id", updated.getDonationId(),
                "status", updated.getStatus()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    //  POST /api/donations/{donationId}/cancel
    // ───────────────────────────────────────────────

    /**
     * Cancels a donation.
     *
     * @param donationId  the external donation ID
     * @param userDetails the authenticated user
     * @return 200 with the updated donation
     */
    @PostMapping("/{donationId}/cancel")
    public ResponseEntity<?> cancelDonation(@PathVariable String donationId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String cancelledBy = (userDetails != null) ? userDetails.getUsername() : "anonymous";
            Donation updated = donationService.cancelDonation(donationId, cancelledBy);
            return ResponseEntity.ok(Map.of(
                "message", "Donation cancelled",
                "donation_id", updated.getDonationId(),
                "status", updated.getStatus()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/stats  — Aggregate stats
    // ───────────────────────────────────────────────

    /**
     * Returns aggregate donation statistics for the dashboard.
     *
     * @return 200 with stats data
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(donationService.getStats());
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/leaderboard  — Top donors
    // ───────────────────────────────────────────────

    /**
     * Returns the top donors by donation count.
     *
     * @param limit maximum entries to return (default 10)
     * @return 200 with leaderboard data
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(donationService.getDonorLeaderboard(limit));
    }

    // ───────────────────────────────────────────────
    //  POST /api/donations/match  — Priority matching
    // ───────────────────────────────────────────────

    /**
     * Matches available donations to a receiver based on preferences.
     *
     * <p>Uses the priority-score matching algorithm (Algorithm 1).</p>
     *
     * @param body JSON with "zip", "dietary_prefs" (array), "allergen_exclusions" (array)
     * @return 200 with ranked list of donations
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/match")
    public ResponseEntity<?> matchDonations(@RequestBody Map<String, Object> body) {
        String zip = (String) body.getOrDefault("zip", "");
        List<String> dietaryList = (List<String>) body.getOrDefault("dietary_prefs", List.of());
        List<String> allergenList = (List<String>) body.getOrDefault("allergen_exclusions", List.of());

        Set<String> dietaryPrefs = new HashSet<>(dietaryList);
        Set<String> allergenExclusions = new HashSet<>(allergenList);

        List<Donation> matched = donationService.matchDonations(zip, dietaryPrefs, allergenExclusions);

        List<Map<String, Object>> response = matched.stream()
                .map(this::donationToMap)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/scan  &  POST /api/donations/scan
    // ───────────────────────────────────────────────

    /**
     * Handles QR/barcode scan lookups. Supports both GET (query param)
     * and POST (JSON body) to match the Flask backend's scan endpoint.
     *
     * @param scanData the donation ID from the scan
     * @return 200 with the donation, or 404
     */
    @GetMapping("/scan")
    public ResponseEntity<?> scanGet(@RequestParam(name = "scan_data") String scanData) {
        return lookupScan(scanData);
    }

    /**
     * POST variant of the scan endpoint.
     *
     * @param body JSON with "scan_data"
     * @return 200 with the donation, or 404
     */
    @PostMapping("/scan")
    public ResponseEntity<?> scanPost(@RequestBody Map<String, Object> body) {
        String scanData = (String) body.getOrDefault("scan_data", "");
        return lookupScan(scanData);
    }

    private ResponseEntity<?> lookupScan(String scanData) {
        if (scanData == null || scanData.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "scan_data is required"));
        }
        return donationService.findByDonationId(scanData.trim().toUpperCase())
                .map(d -> ResponseEntity.ok(donationToMap(d)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Donation not found for scan data: " + scanData)));
    }

    // ───────────────────────────────────────────────
    //  POST /api/donations/{donationId}/undo — Undo last status change (Stack)
    // ───────────────────────────────────────────────

    /**
     * Undoes the most recent status change for a donation.
     * Uses a per-donation undo <strong>Stack</strong> (LIFO / {@link java.util.Deque}).
     *
     * @param donationId the external donation ID
     * @return 200 with the restored donation status
     */
    @PostMapping("/{donationId}/undo")
    public ResponseEntity<?> undoStatus(@PathVariable String donationId) {
        try {
            var updated = donationService.undoStatusChange(donationId);
            return ResponseEntity.ok(Map.of(
                "message", "Status undone",
                "donation_id", updated.getDonationId(),
                "status", updated.getStatus()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/graph — Graph analytics (BFS, communities, influence)
    // ───────────────────────────────────────────────

    /**
     * Returns graph analytics for the donor-receiver network.
     * Uses the {@link DonationGraph} adjacency list with BFS traversal,
     * connected component detection, and influence ranking.
     *
     * @param topN max influencers to return (default 5)
     * @return 200 with graph analytics data
     */
    @GetMapping("/graph")
    public ResponseEntity<?> getGraphAnalytics(@RequestParam(defaultValue = "5") int topN) {
        return ResponseEntity.ok(donationService.getGraphAnalytics(topN));
    }

    /**
     * Returns BFS-based recommendations for a user in the donation network.
     *
     * @param email    the user's email to start BFS from
     * @param maxDepth maximum BFS hops (default 2)
     * @return 200 with list of recommended connections
     */
    @GetMapping("/graph/recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestParam String email,
            @RequestParam(defaultValue = "2") int maxDepth) {
        List<String> recs = donationService.getRecommendations(email, maxDepth);
        return ResponseEntity.ok(Map.of("email", email, "recommendations", recs));
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/categories/tree — Category tree (N-ary tree)
    // ───────────────────────────────────────────────

    /**
     * Returns the food category hierarchy as a nested tree structure.
     * Demonstrates <strong>Tree</strong> data structure (N-ary tree with DFS traversal).
     *
     * @return 200 with nested category tree JSON
     */
    @GetMapping("/categories/tree")
    public ResponseEntity<?> getCategoryTree() {
        return ResponseEntity.ok(donationService.getCategoryTree());
    }

    /**
     * Returns the path from a specific category to the root of the tree.
     *
     * @param category the category name to look up
     * @return 200 with path array, or 404 if not found
     */
    @GetMapping("/categories/path")
    public ResponseEntity<?> getCategoryPath(@RequestParam String category) {
        List<String> path = donationService.getCategoryPath(category);
        if (path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Category not found: " + category));
        }
        return ResponseEntity.ok(Map.of("category", category, "path", path));
    }

    // ───────────────────────────────────────────────
    //  GET /api/donations/sorted — Sorted listing (Comparator)
    // ───────────────────────────────────────────────

    /**
     * Returns donations sorted by a specified field using explicit
     * {@link java.util.Comparator} implementations.
     *
     * <p>Available sort fields: "expiry", "created", "quantity", "name".</p>
     *
     * @param sortBy the field to sort by (default "created")
     * @param status optional status filter
     * @return 200 with sorted list of donations
     */
    @GetMapping("/sorted")
    public ResponseEntity<?> getSorted(
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(required = false) String status) {
        List<Donation> donations = new ArrayList<>(donationService.listDonations(status));
        donationService.sortDonations(donations, sortBy);
        List<Map<String, Object>> response = donations.stream()
                .map(this::donationToMap)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════
    //  MAPPING HELPERS
    // ═══════════════════════════════════════════════════════

    /**
     * Maps a request body to a {@link Donation} entity.
     */
    @SuppressWarnings("unchecked")
    private Donation mapRequestToDonation(Map<String, Object> body) {
        Donation d = new Donation();
        d.setFoodName((String) body.get("food_name"));
        d.setCategory((String) body.get("category"));
        d.setQuantity(body.get("quantity") instanceof Number n ? n.intValue() : Integer.parseInt(body.get("quantity").toString()));
        d.setUnit((String) body.get("unit"));
        d.setDescription((String) body.getOrDefault("description", ""));

        String expiryStr = (String) body.get("expiry_date");
        d.setExpiryDate(java.time.LocalDate.parse(expiryStr));

        d.setStorage((String) body.get("storage"));

        // Allergens — accept array or comma-separated string
        Object allergenObj = body.get("allergens");
        if (allergenObj instanceof List<?> list) {
            d.setAllergenList((List<String>) list);
        } else if (allergenObj instanceof String s) {
            d.setAllergens(s);
        }

        // Dietary tags
        Object dietaryObj = body.get("dietary_tags");
        if (dietaryObj instanceof List<?> list) {
            d.setDietaryTagList((List<String>) list);
        } else if (dietaryObj instanceof String s) {
            d.setDietaryTags(s);
        }

        d.setDonorName((String) body.get("donor_name"));
        d.setDonorEmail((String) body.get("donor_email"));
        d.setDonorPhone((String) body.getOrDefault("donor_phone", ""));
        d.setDonorZip((String) body.get("donor_zip"));
        d.setSpecialInstructions((String) body.getOrDefault("special_instructions", ""));

        return d;
    }

    /**
     * Converts a {@link Donation} entity to a response map, matching
     * the Flask backend's JSON schema for frontend compatibility.
     *
     * @param d the donation entity
     * @return a map suitable for JSON serialisation
     */
    private Map<String, Object> donationToMap(Donation d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getDonationId());
        m.put("food_name", d.getFoodName());
        m.put("category", d.getCategory());
        m.put("quantity", d.getQuantity());
        m.put("unit", d.getUnit());
        m.put("description", d.getDescription());
        m.put("expiry_date", d.getExpiryDate() != null ? d.getExpiryDate().toString() : null);
        m.put("storage", d.getStorage());
        m.put("allergens", d.getAllergenList());
        m.put("dietary_tags", d.getDietaryTagList());
        m.put("donor_name", d.getDonorName());
        m.put("donor_email", d.getDonorEmail());
        m.put("donor_phone", d.getDonorPhone());
        m.put("donor_zip", d.getDonorZip());
        m.put("special_instructions", d.getSpecialInstructions());
        m.put("status", d.getStatus());
        m.put("created_at", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
        m.put("accepted_by", d.getAcceptedBy());
        m.put("accepted_at", d.getAcceptedAt() != null ? d.getAcceptedAt().toString() : null);
        m.put("delivered_by", d.getDeliveredBy());
        m.put("delivered_at", d.getDeliveredAt() != null ? d.getDeliveredAt().toString() : null);
        m.put("days_until_expiry", d.daysUntilExpiry());
        m.put("is_expired", d.isExpired());
        return m;
    }
}
