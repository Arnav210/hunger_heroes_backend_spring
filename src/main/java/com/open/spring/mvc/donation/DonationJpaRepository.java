package com.open.spring.mvc.donation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link Donation} entities.
 *
 * <p>Provides standard CRUD plus custom finder methods. Spring Data JPA
 * auto-generates the implementation at runtime based on method naming
 * conventions and {@code @Query} annotations.</p>
 *
 * @author Ahaan
 * @version 1.0
 */
public interface DonationJpaRepository extends JpaRepository<Donation, Long> {

    /**
     * Find a donation by its human-readable ID (e.g. "HH-M3X7K9-AB2F").
     *
     * @param donationId the external donation ID
     * @return an {@link Optional} containing the donation if found
     */
    Optional<Donation> findByDonationId(String donationId);

    /**
     * Find all donations with a given status.
     *
     * @param status lifecycle status (e.g. "active", "accepted")
     * @return list of matching donations
     */
    List<Donation> findByStatus(String status);

    /**
     * Find all donations for a specific person (by person entity ID).
     *
     * @param personId the database ID of the person
     * @return list of donations belonging to that person
     */
    List<Donation> findByPersonId(Long personId);

    /**
     * Find all donations by donor ZIP code.
     *
     * @param donorZip the ZIP code
     * @return list of matching donations
     */
    List<Donation> findByDonorZip(String donorZip);

    /**
     * Find all donations by category.
     *
     * @param category the food category
     * @return list of matching donations
     */
    List<Donation> findByCategory(String category);

    /**
     * Find donations expiring before a given date.
     *
     * @param date the cutoff expiry date
     * @return list of donations expiring before the date
     */
    List<Donation> findByExpiryDateBefore(LocalDate date);

    /**
     * Find active donations expiring within a given number of days.
     *
     * @param today today's date
     * @param deadline the last acceptable expiry date
     * @return list of expiring active donations
     */
    @Query("SELECT d FROM Donation d WHERE d.status = 'active' AND d.expiryDate BETWEEN :today AND :deadline")
    List<Donation> findExpiringActive(@Param("today") LocalDate today, @Param("deadline") LocalDate deadline);

    /**
     * Count donations by status.
     *
     * @param status lifecycle status
     * @return the count
     */
    long countByStatus(String status);

    /**
     * Count donations by category.
     *
     * @param category food category
     * @return the count
     */
    long countByCategory(String category);

    /**
     * Find donations containing a search term in food name (case-insensitive).
     *
     * @param term the search term
     * @return list of matching donations
     */
    List<Donation> findByFoodNameContainingIgnoreCase(String term);

    /**
     * Find all donations ordered by expiry date ascending (soonest first).
     *
     * @return sorted list of donations
     */
    List<Donation> findAllByOrderByExpiryDateAsc();

    /**
     * Find active donations ordered by creation time descending (newest first).
     *
     * @return sorted list of active donations
     */
    List<Donation> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find donations by donor ZIP code and status.
     *
     * @param donorZip the ZIP code
     * @param status   lifecycle status
     * @return list of matching donations
     */
    List<Donation> findByDonorZipAndStatus(String donorZip, String status);
}
