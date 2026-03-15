package com.open.spring.mvc.donation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.open.spring.mvc.person.Person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a food donation in the Hunger Heroes system.
 *
 * <p>Extends {@link FoodItem} (demonstrating <strong>inheritance</strong>)
 * and adds donor-specific information, lifecycle tracking, and volunteer
 * assignment capabilities.</p>
 *
 * <p><strong>Encapsulation</strong> is enforced through Lombok's
 * {@code @Data} annotation, exposing fields via getters/setters only.</p>
 *
 * <p><strong>Data structures used:</strong></p>
 * <ul>
 *   <li>{@code List<String>} for allergens and dietary tags (stored as JSON)</li>
 *   <li>{@code List<DonationStatusLog>} for audit trail (one-to-many relationship)</li>
 * </ul>
 *
 * @author Ahaan
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DiscriminatorValue("DONATION")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Donation extends FoodItem {

    /**
     * Human-readable donation ID (e.g. "HH-M3X7K9-AB2F").
     * Matches the Flask backend's ID format for cross-system compatibility.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String donationId;

    // ────────── Allergens & Dietary (JSON arrays) ──────────

    /** Comma-separated allergen tags (stored as TEXT, serialised to List). */
    @Column(columnDefinition = "TEXT")
    private String allergens;     // stored as "gluten,dairy"

    /** Comma-separated dietary tags. */
    @Column(columnDefinition = "TEXT")
    private String dietaryTags;   // stored as "vegetarian,vegan"

    // ────────── Donor Information ──────────

    /** Name of the person or organisation donating. */
    @Column(nullable = false)
    private String donorName;

    /** Contact e-mail for the donor. */
    @Column(nullable = false)
    private String donorEmail;

    /** Optional phone number. */
    private String donorPhone;

    /** Donor's ZIP/postal code, used for geo-matching. */
    @Column(nullable = false, length = 10)
    private String donorZip;

    /** Free-text handling / pickup instructions. */
    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    // ────────── Lifecycle & Tracking ──────────

    /** Current lifecycle status. One of {@link DonationConstants#ALLOWED_STATUSES}. */
    @Column(nullable = false, length = 20)
    private String status = "active";

    /** Timestamp of when the donation was created. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Timestamp of last status change. */
    private LocalDateTime updatedAt;

    /** Who accepted the donation. */
    private String acceptedBy;

    /** When the donation was accepted. */
    private LocalDateTime acceptedAt;

    /** Who delivered the donation. */
    private String deliveredBy;

    /** When the donation was delivered. */
    private LocalDateTime deliveredAt;

    // ────────── Relationships ──────────

    /**
     * The authenticated user who created this donation (optional — allows
     * unauthenticated donations from walk-ins).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    @JsonIgnoreProperties({"password", "roles", "submissions", "groups"})
    private Person person;

    /**
     * Audit trail of every status change. Demonstrates the <strong>one-to-many</strong>
     * JPA relationship pattern.
     */
    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("donation")
    private List<DonationStatusLog> statusLogs = new ArrayList<>();

    // ────────── Convenience helpers ──────────

    /**
     * Splits the allergens CSV string into a {@link List}.
     *
     * @return list of allergen tags, or empty list
     */
    public List<String> getAllergenList() {
        if (allergens == null || allergens.isBlank()) return List.of();
        return List.of(allergens.split(","));
    }

    /**
     * Sets allergens from a list by joining into CSV.
     *
     * @param list allergen tag values
     */
    public void setAllergenList(List<String> list) {
        this.allergens = (list == null || list.isEmpty()) ? null : String.join(",", list);
    }

    /**
     * Splits dietary tags CSV string into a {@link List}.
     *
     * @return list of dietary tags, or empty list
     */
    public List<String> getDietaryTagList() {
        if (dietaryTags == null || dietaryTags.isBlank()) return List.of();
        return List.of(dietaryTags.split(","));
    }

    /**
     * Sets dietary tags from a list by joining into CSV.
     *
     * @param list dietary tag values
     */
    public void setDietaryTagList(List<String> list) {
        this.dietaryTags = (list == null || list.isEmpty()) ? null : String.join(",", list);
    }

    /**
     * Produces a human-readable summary including donor info.
     * Overrides {@link FoodItem#getSummary()} — demonstrates <strong>polymorphism</strong>.
     *
     * @return formatted donation summary
     */
    @Override
    public String getSummary() {
        return String.format("[%s] %s from %s (%s) — %s",
            donationId, super.getSummary(), donorName, donorZip, status);
    }
}
