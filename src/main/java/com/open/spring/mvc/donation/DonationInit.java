package com.open.spring.mvc.donation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initialises the donation data on application startup.
 *
 * <p>Seeds the database with sample donations if the table is empty,
 * and runs the expired-donation cleanup job.</p>
 *
 * @author Ahaan
 * @version 1.0
 */
@Component
@Order(10) // run after core person/role init
public class DonationInit implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DonationInit.class);

    @Autowired
    private DonationService donationService;

    /**
     * Called by Spring Boot on startup.
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(String... args) {
        try {
            var seeded = donationService.seedIfEmpty();
            if (!seeded.isEmpty()) {
                log.info("🍲 Hunger Heroes: seeded {} sample donations", seeded.size());
            }
            int expired = donationService.expireOverdueDonations();
            if (expired > 0) {
                log.info("🍲 Hunger Heroes: marked {} donations as expired", expired);
            }
        } catch (Exception e) {
            log.warn("🍲 Hunger Heroes: donation init encountered an issue — {}", e.getMessage());
        }
    }
}
