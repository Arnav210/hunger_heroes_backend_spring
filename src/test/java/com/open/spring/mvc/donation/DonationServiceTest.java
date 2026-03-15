package com.open.spring.mvc.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DonationService}.
 *
 * <p>Covers the two core algorithms (priority-score matching and binary
 * search on expiry), CRUD lifecycle, analytics, and leaderboard
 * (PriorityQueue usage).</p>
 *
 * @author Ahaan
 */
@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationJpaRepository donationRepo;

    @Mock
    private DonationStatusLogRepository logRepo;

    @InjectMocks
    private DonationService service;

    /* ── helpers ── */

    private Donation makeDonation(String id, String foodName, String category,
                                  String zip, String status,
                                  LocalDate expiry, String allergens,
                                  String dietary) {
        Donation d = new Donation();
        d.setDonationId(id);
        d.setFoodName(foodName);
        d.setCategory(category);
        d.setDonorName("Donor-" + id);
        d.setDonorEmail("donor" + id + "@test.com");
        d.setDonorZip(zip);
        d.setStatus(status);
        d.setQuantity(5);
        d.setUnit("items");
        d.setStorage("room-temp");
        d.setExpiryDate(expiry);
        d.setAllergens(allergens);
        d.setDietaryTags(dietary);
        d.setCreatedAt(LocalDateTime.now());
        d.setStatusLogs(new ArrayList<>());
        return d;
    }

    /* ─────────────────────── Algorithm 1: matchDonations ─────────────────── */

    @Nested
    @DisplayName("Algorithm 1 – Priority-score matching O(n·m)")
    class MatchDonationsTests {

        @Test
        @DisplayName("ZIP match ranks donation higher")
        void testZipMatchBoost() {
            Donation d1 = makeDonation("D1", "Rice", "grains", "92127",
                    "active", LocalDate.now().plusDays(30), "", "");
            Donation d2 = makeDonation("D2", "Beans", "canned", "00000",
                    "active", LocalDate.now().plusDays(30), "", "");

            when(donationRepo.findByStatus("active")).thenReturn(List.of(d1, d2));

            List<Donation> results = service.matchDonations(
                    "92127", Set.of(), Set.of());

            // d1 should rank first because ZIP matches
            assertEquals("D1", results.get(0).getDonationId());
        }

        @Test
        @DisplayName("Urgency boost for near-expiry donations")
        void testUrgencyBoost() {
            Donation urgent = makeDonation("U1", "Milk", "dairy", "00000",
                    "active", LocalDate.now().plusDays(1), "", "");
            Donation relaxed = makeDonation("R1", "Juice", "beverages", "00000",
                    "active", LocalDate.now().plusDays(30), "", "");

            when(donationRepo.findByStatus("active")).thenReturn(List.of(urgent, relaxed));

            List<Donation> results = service.matchDonations(
                    "99999", Set.of(), Set.of());

            assertEquals("U1", results.get(0).getDonationId(),
                    "Near-expiry donation should rank first due to urgency");
        }

        @Test
        @DisplayName("Allergen conflict pushes donation lower")
        void testAllergenPenalty() {
            Donation glutenItem = makeDonation("G1", "Bread", "bakery", "00000",
                    "active", LocalDate.now().plusDays(20), "gluten,dairy", "");
            Donation safeItem = makeDonation("S1", "Apple", "produce", "00000",
                    "active", LocalDate.now().plusDays(20), "", "");

            when(donationRepo.findByStatus("active")).thenReturn(
                    List.of(glutenItem, safeItem));

            List<Donation> results = service.matchDonations(
                    "00000", Set.of(), Set.of("gluten"));

            assertEquals("S1", results.get(0).getDonationId(),
                    "Allergen-free donation should rank above conflicting one");
        }

        @Test
        @DisplayName("Dietary tag match boosts donation ranking")
        void testDietaryBoost() {
            Donation veganItem = makeDonation("V1", "Tofu", "produce", "00000",
                    "active", LocalDate.now().plusDays(20), "", "vegan,gluten-free");
            Donation plainItem = makeDonation("P1", "Ham", "meat", "00000",
                    "active", LocalDate.now().plusDays(20), "", "");

            when(donationRepo.findByStatus("active")).thenReturn(
                    List.of(veganItem, plainItem));

            List<Donation> results = service.matchDonations(
                    "00000", Set.of("vegan"), Set.of());

            assertEquals("V1", results.get(0).getDonationId(),
                    "Vegan donation should rank first when dietary preference matches");
        }

        @Test
        @DisplayName("Empty active list returns empty results")
        void testNoActiveDonations() {
            when(donationRepo.findByStatus("active")).thenReturn(List.of());

            List<Donation> results = service.matchDonations(
                    "92127", Set.of(), Set.of());

            assertTrue(results.isEmpty());
        }
    }

    /* ───────────── Algorithm 2: binary search on sorted expiry ───────────── */

    @Nested
    @DisplayName("Algorithm 2 – Binary search O(log n)")
    class BinarySearchTests {

        @Test
        @DisplayName("Returns donations expiring on or after the cut-off date")
        void testBinarySearchBasic() {
            LocalDate today = LocalDate.now();
            List<Donation> sorted = List.of(
                    makeDonation("A", "A", "grains", "00000", "active",
                            today.plusDays(2), "", ""),
                    makeDonation("B", "B", "grains", "00000", "active",
                            today.plusDays(5), "", ""),
                    makeDonation("C", "C", "grains", "00000", "active",
                            today.plusDays(10), "", ""),
                    makeDonation("D", "D", "grains", "00000", "active",
                            today.plusDays(20), "", "")
            );

            when(donationRepo.findAllByOrderByExpiryDateAsc()).thenReturn(sorted);

            List<Donation> result = service.findDonationsExpiringAfter(
                    today.plusDays(5));

            // Should include B(5), C(10), D(20)
            assertTrue(result.size() >= 2);
            assertTrue(result.stream().allMatch(
                    d -> !d.getExpiryDate().isBefore(today.plusDays(5))));
        }

        @Test
        @DisplayName("All donations before cut-off → empty result")
        void testAllBeforeCutoff() {
            LocalDate today = LocalDate.now();
            List<Donation> sorted = List.of(
                    makeDonation("X", "X", "grains", "00000", "active",
                            today.minusDays(3), "", ""),
                    makeDonation("Y", "Y", "grains", "00000", "active",
                            today.minusDays(1), "", "")
            );

            when(donationRepo.findAllByOrderByExpiryDateAsc()).thenReturn(sorted);

            List<Donation> result = service.findDonationsExpiringAfter(today);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("All donations after cut-off → returns all")
        void testAllAfterCutoff() {
            LocalDate today = LocalDate.now();
            List<Donation> sorted = List.of(
                    makeDonation("P", "P", "grains", "00000", "active",
                            today.plusDays(1), "", ""),
                    makeDonation("Q", "Q", "grains", "00000", "active",
                            today.plusDays(5), "", "")
            );

            when(donationRepo.findAllByOrderByExpiryDateAsc()).thenReturn(sorted);

            List<Donation> result = service.findDonationsExpiringAfter(today);
            assertEquals(2, result.size());
        }
    }

    /* ───────────────────── CRUD lifecycle ────────────────────────── */

    @Nested
    @DisplayName("CRUD – create / accept / deliver / cancel")
    class CrudTests {

        @Test
        @DisplayName("createDonation persists and returns the entity")
        void testCreateDonation() {
            Donation d = makeDonation("NEW1", "Pasta", "grains", "11111",
                    "active", LocalDate.now().plusDays(14), "", "");

            when(donationRepo.save(any(Donation.class))).thenReturn(d);

            Donation saved = service.createDonation(d);
            assertNotNull(saved);
            assertEquals("Pasta", saved.getFoodName());
            verify(donationRepo, times(1)).save(d);
        }

        @Test
        @DisplayName("acceptDonation transitions active → accepted")
        void testAcceptDonation() {
            Donation d = makeDonation("ACC1", "Bread", "bakery", "22222",
                    "active", LocalDate.now().plusDays(7), "", "");

            when(donationRepo.findByDonationId("ACC1"))
                    .thenReturn(Optional.of(d));
            when(donationRepo.save(any(Donation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(logRepo.save(any(DonationStatusLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Donation result = service.acceptDonation("ACC1", "receiver1");
            assertNotNull(result);
            assertEquals("accepted", result.getStatus());
            assertEquals("receiver1", result.getAcceptedBy());
        }

        @Test
        @DisplayName("acceptDonation on non-active throws IllegalStateException")
        void testAcceptNonActive() {
            Donation d = makeDonation("ACC2", "Milk", "dairy", "33333",
                    "delivered", LocalDate.now().plusDays(3), "", "");

            when(donationRepo.findByDonationId("ACC2"))
                    .thenReturn(Optional.of(d));

            assertThrows(IllegalStateException.class,
                    () -> service.acceptDonation("ACC2", "r"));
        }

        @Test
        @DisplayName("deliverDonation transitions accepted → delivered")
        void testDeliverDonation() {
            Donation d = makeDonation("DEL1", "Soup", "canned", "44444",
                    "accepted", LocalDate.now().plusDays(5), "", "");

            when(donationRepo.findByDonationId("DEL1"))
                    .thenReturn(Optional.of(d));
            when(donationRepo.save(any(Donation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(logRepo.save(any(DonationStatusLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Donation result = service.deliverDonation("DEL1", "vol1");
            assertNotNull(result);
            assertEquals("delivered", result.getStatus());
        }

        @Test
        @DisplayName("cancelDonation transitions active → cancelled")
        void testCancelDonation() {
            Donation d = makeDonation("CAN1", "Juice", "beverages", "55555",
                    "active", LocalDate.now().plusDays(10), "", "");

            when(donationRepo.findByDonationId("CAN1"))
                    .thenReturn(Optional.of(d));
            when(donationRepo.save(any(Donation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(logRepo.save(any(DonationStatusLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Donation result = service.cancelDonation("CAN1", "donor1");
            assertNotNull(result);
            assertEquals("cancelled", result.getStatus());
        }
    }

    /* ─────────────────── Analytics & Leaderboard ─────────────────── */

    @Nested
    @DisplayName("Analytics & Leaderboard")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class AnalyticsTests {

        @Test
        @DisplayName("getStats returns correct aggregate counts")
        void testGetStats() {
            when(donationRepo.count()).thenReturn(10L);
            when(donationRepo.countByStatus("active")).thenReturn(4L);
            when(donationRepo.countByStatus("accepted")).thenReturn(2L);
            when(donationRepo.countByStatus("in-transit")).thenReturn(0L);
            when(donationRepo.countByStatus("delivered")).thenReturn(3L);
            when(donationRepo.countByStatus("expired")).thenReturn(1L);
            when(donationRepo.countByStatus("cancelled")).thenReturn(0L);
            when(donationRepo.countByCategory("produce")).thenReturn(3L);
            when(donationRepo.countByCategory("dairy")).thenReturn(2L);
            when(donationRepo.countByCategory("grains")).thenReturn(1L);
            when(donationRepo.countByCategory("meat")).thenReturn(0L);
            when(donationRepo.countByCategory("canned")).thenReturn(1L);
            when(donationRepo.countByCategory("bakery")).thenReturn(1L);
            when(donationRepo.countByCategory("snacks")).thenReturn(1L);
            when(donationRepo.countByCategory("beverages")).thenReturn(1L);
            when(donationRepo.countByCategory("frozen")).thenReturn(0L);
            when(donationRepo.countByCategory("baby-food")).thenReturn(0L);
            when(donationRepo.countByCategory("prepared")).thenReturn(0L);
            when(donationRepo.countByCategory("other")).thenReturn(0L);
            when(donationRepo.findExpiringActive(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            Map<String, Object> stats = service.getStats();
            assertEquals(10L, stats.get("total"));

            @SuppressWarnings("unchecked")
            Map<String, Long> byStatus = (Map<String, Long>) stats.get("byStatus");
            assertEquals(4L, byStatus.get("active"));
            assertEquals(3L, byStatus.get("delivered"));
        }

        @Test
        @DisplayName("getDonorLeaderboard returns top N donors via PriorityQueue")
        void testLeaderboard() {
            // 3 donors with different counts
            List<Donation> all = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Donation d = makeDonation("LB-A-" + i, "Item", "grains",
                        "00000", "delivered",
                        LocalDate.now().plusDays(10), "", "");
                d.setDonorName("Alice");
                all.add(d);
            }
            for (int i = 0; i < 3; i++) {
                Donation d = makeDonation("LB-B-" + i, "Item", "grains",
                        "00000", "delivered",
                        LocalDate.now().plusDays(10), "", "");
                d.setDonorName("Bob");
                all.add(d);
            }
            Donation single = makeDonation("LB-C-0", "Item", "grains",
                    "00000", "delivered",
                    LocalDate.now().plusDays(10), "", "");
            single.setDonorName("Charlie");
            all.add(single);

            when(donationRepo.findAll()).thenReturn(all);

            Map<String, Long> board = service.getDonorLeaderboard(2);
            assertEquals(2, board.size());
            // Alice (5) should be first, Bob (3) second
            assertTrue(board.containsKey("Alice"));
            assertEquals(5L, board.get("Alice"));
            assertTrue(board.containsKey("Bob"));
            assertEquals(3L, board.get("Bob"));
        }
    }

    /* ─────────────────── DonationStatusLog ─────────────────── */

    @Test
    @DisplayName("DonationStatusLog.create produces correct audit record")
    void testStatusLogFactory() {
        Donation d = makeDonation("LOG1", "Test", "other", "00000",
                "active", LocalDate.now(), "", "");

        DonationStatusLog log = DonationStatusLog.create(
                d, "active", "accepted", "admin", "Approved");

        assertEquals("active", log.getFromStatus());
        assertEquals("accepted", log.getToStatus());
        assertEquals("admin", log.getChangedBy());
        assertEquals("Approved", log.getNote());
        assertNotNull(log.getChangedAt());
        assertEquals(d, log.getDonation());
    }
}
