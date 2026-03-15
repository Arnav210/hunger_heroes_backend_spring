package com.open.spring.mvc.donation;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for {@link Donation} business logic.
 *
 * <p>Contains two custom algorithms with complexity analysis:</p>
 * <ol>
 *   <li><strong>Priority-score matching</strong> — a weighted scoring algorithm
 *       that ranks available donations for a receiver based on proximity (ZIP),
 *       urgency (days-to-expiry), and dietary compatibility.
 *       Time complexity: <em>O(n · m)</em> where <em>n</em> = donations,
 *       <em>m</em> = dietary filters.</li>
 *   <li><strong>Binary search by expiry</strong> — after sorting donations by
 *       expiry date, performs an O(log n) binary search to find the first
 *       donation expiring on or after a given date.</li>
 * </ol>
 *
 * <p><strong>Data Structures used:</strong></p>
 * <ul>
 *   <li>{@link HashMap} — donation stats aggregation, priority score map</li>
 *   <li>{@link ArrayList} — sorted working copies of donation lists</li>
 *   <li>{@link TreeMap} — ordered leaderboard of top donors</li>
 *   <li>{@link HashSet} — O(1) lookup for validation of enum values</li>
 *   <li>{@link LinkedHashMap} — insertion-ordered category counts</li>
 *   <li>{@link PriorityQueue} — min-heap for "top-N expiring soon" queries</li>
 *   <li>{@link java.util.Deque} / {@link ArrayDeque} — status undo stack (LIFO) for donation lifecycle rollback</li>
 *   <li>{@link ConcurrentHashMap} — thread-safe undo history per donation</li>
 * </ul>
 *
 * <p><strong>Graph support:</strong> {@link DonationGraph} (adjacency list) with
 * BFS recommendations, connected-component community detection, and influence ranking.</p>
 *
 * <p><strong>Tree support:</strong> {@link CategoryTree} (N-ary tree) for hierarchical
 * food category classification with DFS traversal and path-to-root queries.</p>
 *
 * @author Ahaan
 * @version 1.0
 */
@Service
@Transactional
public class DonationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Autowired
    private DonationJpaRepository donationRepo;

    @Autowired
    private DonationStatusLogRepository statusLogRepo;

    // ═══════════════════════════════════════════════════════
    //  CRUD
    // ═══════════════════════════════════════════════════════

    /**
     * Creates a new donation, generating a unique ID and logging the initial status.
     *
     * @param donation the donation entity to persist
     * @return the saved donation with generated ID
     * @throws IllegalArgumentException if validation fails
     */
    public Donation createDonation(Donation donation) {
        validateDonation(donation);
        donation.setDonationId(generateDonationId());
        donation.setStatus("active");
        donation.setCreatedAt(LocalDateTime.now());
        Donation saved = donationRepo.save(donation);
        logStatusChange(saved, null, "active", donation.getDonorName(), "Donation created");
        return saved;
    }

    /**
     * Retrieves a donation by its human-readable ID.
     *
     * @param donationId the external donation ID
     * @return an {@link Optional} containing the donation
     */
    public Optional<Donation> findByDonationId(String donationId) {
        return donationRepo.findByDonationId(donationId);
    }

    /**
     * Lists all donations, optionally filtered by status.
     *
     * @param status optional status filter (may be {@code null})
     * @return list of matching donations
     */
    public List<Donation> listDonations(String status) {
        if (status != null && !status.isBlank()) {
            return donationRepo.findByStatusOrderByCreatedAtDesc(status);
        }
        return donationRepo.findAll();
    }

    /**
     * Lists all donations belonging to a specific person.
     *
     * @param personId the person's database ID
     * @return list of that person's donations
     */
    public List<Donation> listByPerson(Long personId) {
        return donationRepo.findByPersonId(personId);
    }

    /**
     * Marks a donation as "accepted".
     *
     * @param donationId the external donation ID
     * @param acceptedBy who is accepting the donation
     * @return the updated donation
     * @throws NoSuchElementException   if not found
     * @throws IllegalStateException    if status transition is invalid
     */
    public Donation acceptDonation(String donationId, String acceptedBy) {
        Donation d = donationRepo.findByDonationId(donationId)
                .orElseThrow(() -> new NoSuchElementException("Donation not found: " + donationId));
        if (!"active".equals(d.getStatus())) {
            throw new IllegalStateException("Can only accept an active donation. Current status: " + d.getStatus());
        }
        String old = d.getStatus();
        pushUndo(donationId, old);  // save to undo stack before changing
        d.setStatus("accepted");
        d.setAcceptedBy(acceptedBy);
        d.setAcceptedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        Donation saved = donationRepo.save(d);
        logStatusChange(saved, old, "accepted", acceptedBy, "Donation accepted");
        return saved;
    }

    /**
     * Marks a donation as "delivered".
     *
     * @param donationId  the external donation ID
     * @param deliveredBy who delivered
     * @return the updated donation
     * @throws NoSuchElementException if not found
     * @throws IllegalStateException  if status transition is invalid
     */
    public Donation deliverDonation(String donationId, String deliveredBy) {
        Donation d = donationRepo.findByDonationId(donationId)
                .orElseThrow(() -> new NoSuchElementException("Donation not found: " + donationId));
        if (!"accepted".equals(d.getStatus()) && !"in-transit".equals(d.getStatus())) {
            throw new IllegalStateException("Can only deliver an accepted/in-transit donation. Current: " + d.getStatus());
        }
        String old = d.getStatus();
        pushUndo(donationId, old);  // save to undo stack before changing
        d.setStatus("delivered");
        d.setDeliveredBy(deliveredBy);
        d.setDeliveredAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        Donation saved = donationRepo.save(d);
        logStatusChange(saved, old, "delivered", deliveredBy, "Donation delivered");
        return saved;
    }

    /**
     * Cancels a donation.
     *
     * @param donationId the external donation ID
     * @param cancelledBy who cancelled
     * @return the updated donation
     */
    public Donation cancelDonation(String donationId, String cancelledBy) {
        Donation d = donationRepo.findByDonationId(donationId)
                .orElseThrow(() -> new NoSuchElementException("Donation not found: " + donationId));
        if ("delivered".equals(d.getStatus()) || "cancelled".equals(d.getStatus())) {
            throw new IllegalStateException("Cannot cancel a donation that is already " + d.getStatus());
        }
        String old = d.getStatus();
        pushUndo(donationId, old);  // save to undo stack before changing
        d.setStatus("cancelled");
        d.setUpdatedAt(LocalDateTime.now());
        Donation saved = donationRepo.save(d);
        logStatusChange(saved, old, "cancelled", cancelledBy, "Donation cancelled");
        return saved;
    }

    // ═══════════════════════════════════════════════════════
    //  ALGORITHM 1: Priority-Score Matching  O(n · m)
    // ═══════════════════════════════════════════════════════

    /**
     * <strong>Algorithm 1 — Priority-Score Donation Matching</strong>
     *
     * <p>Ranks all active donations by a weighted priority score for a
     * potential receiver. Higher scores mean better matches.</p>
     *
     * <p><strong>Scoring criteria:</strong></p>
     * <ul>
     *   <li>+30 points if the donor ZIP matches the receiver ZIP (locality)</li>
     *   <li>+0–40 points based on urgency (fewer days to expiry → higher score)</li>
     *   <li>+10 points for each matching dietary tag</li>
     *   <li>−20 points if the donation contains a receiver-specified allergen</li>
     * </ul>
     *
     * <p><strong>Complexity:</strong> O(n · m) where n = number of active
     * donations and m = max(dietary filter size, allergen filter size).</p>
     *
     * @param receiverZip       the receiver's ZIP code for locality matching
     * @param dietaryPrefs      dietary tags the receiver prefers (may be empty)
     * @param allergenExclusions allergens the receiver wants to avoid (may be empty)
     * @return list of active donations sorted by descending priority score
     */
    public List<Donation> matchDonations(String receiverZip,
                                         Set<String> dietaryPrefs,
                                         Set<String> allergenExclusions) {
        List<Donation> active = donationRepo.findByStatus("active");

        // Map each donation to its priority score
        Map<Donation, Integer> scoreMap = new HashMap<>();

        for (Donation d : active) {
            int score = 0;

            // Locality bonus
            if (receiverZip != null && receiverZip.equals(d.getDonorZip())) {
                score += 30;
            }

            // Urgency bonus (closer to expiry = higher priority to distribute)
            long daysLeft = d.daysUntilExpiry();
            if (daysLeft <= 3) score += 40;
            else if (daysLeft <= 7) score += 30;
            else if (daysLeft <= 14) score += 20;
            else if (daysLeft <= 30) score += 10;

            // Dietary compatibility bonus  O(m) per donation
            if (dietaryPrefs != null) {
                for (String tag : d.getDietaryTagList()) {
                    if (dietaryPrefs.contains(tag)) score += 10;
                }
            }

            // Allergen penalty  O(m) per donation
            if (allergenExclusions != null) {
                for (String a : d.getAllergenList()) {
                    if (allergenExclusions.contains(a)) score -= 20;
                }
            }

            scoreMap.put(d, score);
        }

        // Sort by descending score  O(n log n)
        List<Donation> sorted = new ArrayList<>(active);
        sorted.sort((a, b) -> scoreMap.getOrDefault(b, 0) - scoreMap.getOrDefault(a, 0));
        return sorted;
    }

    // ═══════════════════════════════════════════════════════
    //  ALGORITHM 2: Binary Search by Expiry  O(log n)
    // ═══════════════════════════════════════════════════════

    /**
     * <strong>Algorithm 2 — Binary Search by Expiry Date</strong>
     *
     * <p>Sorts all donations by expiry date (ascending) and performs a
     * classic binary search to find the index of the first donation
     * expiring on or after the specified {@code targetDate}.</p>
     *
     * <p><strong>Complexity:</strong></p>
     * <ul>
     *   <li>Sorting: O(n log n) — Timsort via {@link Collections#sort}</li>
     *   <li>Binary search: O(log n)</li>
     *   <li>Overall: O(n log n) dominated by the sort</li>
     * </ul>
     *
     * @param targetDate the earliest acceptable expiry date
     * @return sublist of donations expiring on or after {@code targetDate},
     *         sorted by expiry ascending
     */
    public List<Donation> findDonationsExpiringAfter(LocalDate targetDate) {
        // Fetch and sort by expiry  O(n log n)
        List<Donation> all = new ArrayList<>(donationRepo.findAllByOrderByExpiryDateAsc());

        // Binary search for the first donation >= targetDate  O(log n)
        int lo = 0, hi = all.size() - 1, result = all.size();
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (!all.get(mid).getExpiryDate().isBefore(targetDate)) {
                result = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }

        // Return everything from `result` onward
        return (result < all.size()) ? all.subList(result, all.size()) : List.of();
    }

    // ═══════════════════════════════════════════════════════
    //  ANALYTICS — uses HashMap, TreeMap, LinkedHashMap
    // ═══════════════════════════════════════════════════════

    /**
     * Computes aggregate statistics for the donation dashboard.
     *
     * <p>Uses a {@link HashMap} for status counts and a {@link LinkedHashMap}
     * for category counts (preserving insertion order).</p>
     *
     * @return a map of analytics data
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Status counts  (HashMap)
        Map<String, Long> statusCounts = new HashMap<>();
        for (String s : DonationConstants.ALLOWED_STATUSES) {
            statusCounts.put(s, donationRepo.countByStatus(s));
        }
        stats.put("byStatus", statusCounts);

        // Category counts (LinkedHashMap for order)
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (String c : DonationConstants.ALLOWED_CATEGORIES) {
            long count = donationRepo.countByCategory(c);
            if (count > 0) categoryCounts.put(c, count);
        }
        stats.put("byCategory", categoryCounts);

        // Total
        stats.put("total", donationRepo.count());

        // Expiring soon (next 7 days)
        List<Donation> expiring = donationRepo.findExpiringActive(LocalDate.now(), LocalDate.now().plusDays(7));
        stats.put("expiringSoon", expiring.size());

        return stats;
    }

    /**
     * Produces a donor leaderboard (top donors by number of donations).
     *
     * <p>Uses a {@link TreeMap} with a reverse-order comparator to keep
     * donors sorted by donation count in descending order.</p>
     *
     * @param limit max number of entries to return
     * @return ordered map of donor name → donation count
     */
    public Map<String, Long> getDonorLeaderboard(int limit) {
        // Count per donor  O(n)
        Map<String, Long> counts = new HashMap<>();
        for (Donation d : donationRepo.findAll()) {
            counts.merge(d.getDonorName(), 1L, Long::sum);
        }

        // Sort by count descending using a PriorityQueue (min-heap)  O(n log k)
        PriorityQueue<Map.Entry<String, Long>> heap =
            new PriorityQueue<>(Comparator.comparingLong(Map.Entry::getValue));

        for (Map.Entry<String, Long> e : counts.entrySet()) {
            heap.offer(e);
            if (heap.size() > limit) heap.poll();
        }

        // Drain into insertion-ordered map (highest first)
        LinkedList<Map.Entry<String, Long>> stack = new LinkedList<>();
        while (!heap.isEmpty()) stack.push(heap.poll());

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : stack) {
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }

    /**
     * Searches donations by food name (case-insensitive substring match).
     *
     * @param term the search term
     * @return list of matching donations
     */
    public List<Donation> search(String term) {
        return donationRepo.findByFoodNameContainingIgnoreCase(term);
    }

    // ═══════════════════════════════════════════════════════
    //  CLEANUP — mark expired donations
    // ═══════════════════════════════════════════════════════

    /**
     * Marks all active donations whose expiry date has passed as "expired".
     *
     * @return number of donations expired
     */
    public int expireOverdueDonations() {
        List<Donation> overdue = donationRepo.findByExpiryDateBefore(LocalDate.now());
        int count = 0;
        for (Donation d : overdue) {
            if ("active".equals(d.getStatus())) {
                String old = d.getStatus();
                d.setStatus("expired");
                d.setUpdatedAt(LocalDateTime.now());
                donationRepo.save(d);
                logStatusChange(d, old, "expired", "SYSTEM", "Auto-expired past expiry date");
                count++;
            }
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════
    //  INIT — seed sample data
    // ═══════════════════════════════════════════════════════

    /**
     * Seeds the database with sample donation data if empty.
     *
     * @return list of seeded donations (empty if data already existed)
     */
    public List<Donation> seedIfEmpty() {
        if (donationRepo.count() > 0) return List.of();

        List<Donation> seeds = new ArrayList<>();

        seeds.add(makeDonation("Canned Tomato Soup", "canned", 24, "cans",
            "Campbell's condensed, unopened", LocalDate.now().plusDays(180),
            "room-temp", "gluten", "vegetarian",
            "Local Grocery Co.", "donate@localgrocery.com", "92101"));

        seeds.add(makeDonation("Fresh Bread Loaves", "bakery", 15, "items",
            "Whole wheat, baked today", LocalDate.now().plusDays(3),
            "room-temp", "gluten", "",
            "City Bakery", "info@citybakery.com", "92103"));

        seeds.add(makeDonation("Organic Milk Gallons", "dairy", 8, "gallons",
            "2% organic, pasteurized", LocalDate.now().plusDays(10),
            "refrigerated", "dairy", "organic",
            "Green Valley Farm", "farm@greenvalley.com", "92104"));

        seeds.add(makeDonation("Frozen Chicken Breasts", "meat-protein", 20, "lbs",
            "Boneless skinless, USDA Grade A", LocalDate.now().plusDays(90),
            "frozen", "none", "gluten-free,halal",
            "MeatMart Wholesale", "sales@meatmart.com", "92102"));

        seeds.add(makeDonation("Mixed Fruit Cups", "snacks", 50, "items",
            "Individual fruit cups, no sugar added", LocalDate.now().plusDays(60),
            "room-temp", "none", "vegan,gluten-free",
            "HealthSnack Inc.", "hello@healthsnack.com", "92101"));

        seeds.add(makeDonation("Baby Formula", "baby-food", 12, "cans",
            "Enfamil NeuroPro, powder", LocalDate.now().plusDays(365),
            "cool-dry", "dairy,soy", "",
            "Family Support Center", "info@familysupport.org", "92105"));

        for (Donation d : seeds) {
            d.setDonationId(generateDonationId());
            d.setStatus("active");
            d.setCreatedAt(LocalDateTime.now());
            donationRepo.save(d);
            logStatusChange(d, null, "active", d.getDonorName(), "Seed data");
        }

        return seeds;
    }

    // ═══════════════════════════════════════════════════════
    //  STACK — Status Undo (LIFO) per Donation
    // ═══════════════════════════════════════════════════════

    /**
     * Per-donation undo stack: donationId → Deque of previous statuses.
     * Demonstrates <strong>Stack</strong> (LIFO) data structure using {@link ArrayDeque}.
     *
     * <p>Each status change pushes the old status onto the stack, and
     * {@link #undoStatusChange(String)} pops and restores the most recent one.</p>
     */
    private final Map<String, Deque<String>> undoStacks = new ConcurrentHashMap<>();

    /**
     * Pushes the current status onto the undo stack before a status change.
     *
     * @param donationId the donation's external ID
     * @param oldStatus  the status being replaced
     */
    private void pushUndo(String donationId, String oldStatus) {
        undoStacks.computeIfAbsent(donationId, k -> new ArrayDeque<>()).push(oldStatus);
    }

    /**
     * <strong>Undo the most recent status change</strong> for a donation.
     *
     * <p>Pops the previous status from the per-donation undo stack (LIFO)
     * and restores it. Demonstrates the <strong>Stack</strong> data structure
     * and LIFO access pattern.</p>
     *
     * <p><strong>Complexity:</strong> O(1) for the stack pop.</p>
     *
     * @param donationId the external donation ID
     * @return the restored donation
     * @throws NoSuchElementException if donation not found
     * @throws IllegalStateException  if no undo history available
     */
    public Donation undoStatusChange(String donationId) {
        Donation d = donationRepo.findByDonationId(donationId)
                .orElseThrow(() -> new NoSuchElementException("Donation not found: " + donationId));

        Deque<String> stack = undoStacks.get(donationId);
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("No undo history for donation: " + donationId);
        }

        String previousStatus = stack.pop(); // LIFO pop
        String currentStatus = d.getStatus();
        d.setStatus(previousStatus);
        d.setUpdatedAt(LocalDateTime.now());
        Donation saved = donationRepo.save(d);
        logStatusChange(saved, currentStatus, previousStatus, "UNDO", "Status undone via undo stack");
        return saved;
    }

    /**
     * Returns the undo history (stack contents) for a donation without modifying it.
     *
     * @param donationId the external donation ID
     * @return list of previous statuses (most recent first), or empty list
     */
    public List<String> getUndoHistory(String donationId) {
        Deque<String> stack = undoStacks.get(donationId);
        if (stack == null) return List.of();
        return new ArrayList<>(stack);
    }

    // ═══════════════════════════════════════════════════════
    //  GRAPH — Donor-Receiver Network
    // ═══════════════════════════════════════════════════════

    /**
     * Builds a {@link DonationGraph} from all delivered/accepted donations.
     *
     * <p>Each edge represents a donor→receiver relationship (donor_email → accepted_by).
     * Demonstrates <strong>Graph</strong> data structure with adjacency list.</p>
     *
     * @return the populated donation graph
     */
    public DonationGraph buildDonorReceiverGraph() {
        DonationGraph graph = new DonationGraph();
        for (Donation d : donationRepo.findAll()) {
            if (d.getDonorEmail() != null && d.getAcceptedBy() != null
                && !d.getAcceptedBy().isBlank() && !"anonymous".equals(d.getAcceptedBy())) {
                graph.addEdge(d.getDonorEmail(), d.getAcceptedBy());
            }
        }
        return graph;
    }

    /**
     * Returns BFS-based donor recommendations for a given user.
     *
     * <p>Builds the donation graph and runs BFS from the given node to find
     * related donors/receivers within the specified depth. Uses the
     * <strong>Graph + BFS algorithm</strong>.</p>
     *
     * @param userEmail the starting user's email
     * @param maxDepth  maximum BFS hops (default: 2)
     * @return list of recommended connected users
     */
    public List<String> getRecommendations(String userEmail, int maxDepth) {
        DonationGraph graph = buildDonorReceiverGraph();
        return graph.bfsRecommendations(userEmail, maxDepth);
    }

    /**
     * Returns graph analytics: node count, edge count, communities, and top influencers.
     *
     * @param topN number of top influencers to return
     * @return map of graph analytics data
     */
    public Map<String, Object> getGraphAnalytics(int topN) {
        DonationGraph graph = buildDonorReceiverGraph();
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("summary", graph.getSummary());
        analytics.put("communities", graph.findCommunities().size());
        analytics.put("topInfluencers", graph.influenceRanking(topN));
        return analytics;
    }

    // ═══════════════════════════════════════════════════════
    //  TREE — Category Hierarchy
    // ═══════════════════════════════════════════════════════

    /** Singleton category tree instance. */
    private final CategoryTree categoryTree = new CategoryTree();

    /**
     * Returns the full category tree as a nested map (for JSON serialisation).
     *
     * <p>Demonstrates <strong>Tree</strong> data structure with N-ary tree nodes.</p>
     *
     * @return nested map representing the category hierarchy
     */
    public Map<String, Object> getCategoryTree() {
        return categoryTree.toMap();
    }

    /**
     * Returns the path from a specific category up to the root of the tree.
     *
     * <p>Uses DFS search + parent traversal. Complexity: O(n) + O(h).</p>
     *
     * @param categoryName the category to look up
     * @return list of category names from leaf to root
     */
    public List<String> getCategoryPath(String categoryName) {
        return categoryTree.pathToRoot(categoryName);
    }

    /**
     * Returns the pre-order DFS traversal of the category tree.
     *
     * @return list of all category names in pre-order
     */
    public List<String> getCategoryTraversal() {
        return categoryTree.preOrderTraversal();
    }

    // ═══════════════════════════════════════════════════════
    //  SORTING — Explicit Comparator implementations
    // ═══════════════════════════════════════════════════════

    /**
     * Comparator that sorts donations by expiry date ascending (soonest first).
     * Demonstrates explicit {@link Comparator} usage for business logic sorting.
     */
    public static final Comparator<Donation> BY_EXPIRY_ASC =
        Comparator.comparing(FoodItem::getExpiryDate);

    /**
     * Comparator that sorts donations by creation time descending (newest first).
     */
    public static final Comparator<Donation> BY_CREATED_DESC =
        Comparator.comparing(Donation::getCreatedAt, Comparator.reverseOrder());

    /**
     * Comparator that sorts donations by quantity descending (largest first).
     */
    public static final Comparator<Donation> BY_QUANTITY_DESC =
        Comparator.comparingInt((Donation d) -> d.getQuantity()).reversed();

    /**
     * Sorts donations by the given field using explicit {@link Comparator} implementations.
     *
     * <p>Demonstrates practical sorting with {@code Comparator} for business logic.
     * Available sort fields: "expiry" (ascending), "created" (descending),
     * "quantity" (descending), "name" (alphabetical).</p>
     *
     * @param donations the list to sort (will be sorted in place)
     * @param sortBy    the sort field ("expiry", "created", "quantity", "name")
     * @return the sorted list
     */
    public List<Donation> sortDonations(List<Donation> donations, String sortBy) {
        Comparator<Donation> comparator = switch (sortBy) {
            case "expiry" -> BY_EXPIRY_ASC;
            case "created" -> BY_CREATED_DESC;
            case "quantity" -> BY_QUANTITY_DESC;
            case "name" -> Comparator.comparing(FoodItem::getFoodName, String.CASE_INSENSITIVE_ORDER);
            default -> BY_CREATED_DESC;
        };
        donations.sort(comparator);
        return donations;
    }

    // ═══════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════

    private Donation makeDonation(String name, String cat, int qty, String unit,
                                  String desc, LocalDate expiry, String storage,
                                  String allergens, String dietary,
                                  String donorName, String donorEmail, String donorZip) {
        Donation d = new Donation();
        d.setFoodName(name);
        d.setCategory(cat);
        d.setQuantity(qty);
        d.setUnit(unit);
        d.setDescription(desc);
        d.setExpiryDate(expiry);
        d.setStorage(storage);
        d.setAllergens(allergens);
        d.setDietaryTags(dietary);
        d.setDonorName(donorName);
        d.setDonorEmail(donorEmail);
        d.setDonorZip(donorZip);
        return d;
    }

    /**
     * Validates a donation entity before persistence.
     *
     * <p>Uses {@link HashSet} constants for O(1) membership checks.</p>
     *
     * @param d the donation to validate
     * @throws IllegalArgumentException if any field is invalid
     */
    private void validateDonation(Donation d) {
        if (d.getFoodName() == null || d.getFoodName().isBlank())
            throw new IllegalArgumentException("Food name is required");
        if (!DonationConstants.ALLOWED_CATEGORIES.contains(d.getCategory()))
            throw new IllegalArgumentException("Invalid category: " + d.getCategory());
        if (!DonationConstants.ALLOWED_UNITS.contains(d.getUnit()))
            throw new IllegalArgumentException("Invalid unit: " + d.getUnit());
        if (!DonationConstants.ALLOWED_STORAGE.contains(d.getStorage()))
            throw new IllegalArgumentException("Invalid storage type: " + d.getStorage());
        if (d.getExpiryDate() == null || d.getExpiryDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expiry date must be in the future");
        if (d.getDonorName() == null || d.getDonorName().isBlank())
            throw new IllegalArgumentException("Donor name is required");
        if (d.getDonorEmail() == null || d.getDonorEmail().isBlank())
            throw new IllegalArgumentException("Donor email is required");
        if (d.getDonorZip() == null || d.getDonorZip().isBlank())
            throw new IllegalArgumentException("Donor ZIP code is required");
        if (d.getQuantity() <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
    }

    /**
     * Generates a unique human-readable donation ID matching the Flask format:
     * {@code HH-XXXXXX-XXXX}.
     *
     * @return a new unique donation ID
     */
    private String generateDonationId() {
        String id;
        do {
            StringBuilder sb = new StringBuilder("HH-");
            for (int i = 0; i < 6; i++) sb.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
            sb.append("-");
            for (int i = 0; i < 4; i++) sb.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
            id = sb.toString();
        } while (donationRepo.findByDonationId(id).isPresent());
        return id;
    }

    /**
     * Records a status change in the audit log.
     */
    private void logStatusChange(Donation d, String from, String to, String by, String note) {
        DonationStatusLog log = DonationStatusLog.create(d, from, to, by, note);
        d.getStatusLogs().add(log);
        statusLogRepo.save(log);
    }
}
