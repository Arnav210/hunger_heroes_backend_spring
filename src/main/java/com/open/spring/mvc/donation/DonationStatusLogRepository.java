package com.open.spring.mvc.donation;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link DonationStatusLog} entities.
 *
 * @author Ahaan
 * @version 1.0
 */
public interface DonationStatusLogRepository extends JpaRepository<DonationStatusLog, Long> {
}
