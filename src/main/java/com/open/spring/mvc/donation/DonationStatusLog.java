package com.open.spring.mvc.donation;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit-trail entity that records every status transition of a {@link Donation}.
 *
 * <p>Stored in a one-to-many relationship with {@link Donation}, this entity
 * demonstrates proper <strong>database entity relationships</strong> via JPA
 * {@code @ManyToOne} / {@code @OneToMany} mapping.</p>
 *
 * @author Ahaan
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donation_status_logs")
public class DonationStatusLog {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The donation this log entry belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false)
    @JsonIgnoreProperties("statusLogs")
    private Donation donation;

    /** Previous status value. */
    @Column(length = 20)
    private String fromStatus;

    /** New status value after the transition. */
    @Column(nullable = false, length = 20)
    private String toStatus;

    /** Who triggered this status change. */
    private String changedBy;

    /** When the change occurred. */
    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    /** Optional note explaining the change. */
    @Column(columnDefinition = "TEXT")
    private String note;

    /**
     * Convenience factory method for creating a log entry.
     *
     * @param donation  the donation being modified
     * @param from      previous status (may be {@code null} for creation)
     * @param to        new status
     * @param changedBy who made the change
     * @param note      optional note
     * @return a new {@link DonationStatusLog} instance
     */
    public static DonationStatusLog create(Donation donation, String from, String to,
                                           String changedBy, String note) {
        DonationStatusLog log = new DonationStatusLog();
        log.setDonation(donation);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setChangedBy(changedBy);
        log.setNote(note);
        log.setChangedAt(LocalDateTime.now());
        return log;
    }
}
