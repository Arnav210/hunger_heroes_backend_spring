package com.open.spring.mvc.hungerheroes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link HungerHeroScore} entities.
 *
 * Provides standard CRUD plus custom queries for leaderboard display.
 * Pattern reference: gamespring2 leaderboard/LeaderboardRepository.java
 */
@Repository
public interface HungerHeroScoreRepository extends JpaRepository<HungerHeroScore, Long> {

    /**
     * All scores for a specific user, newest first.
     */
    List<HungerHeroScore> findByPersonIdOrderByCreatedAtDesc(Long personId);

    /**
     * All scores for a level, highest score first.
     */
    List<HungerHeroScore> findByLevelIdOrderByScoreDesc(String levelId);

    /**
     * Top N scores across all levels.
     */
    @Query("SELECT s FROM HungerHeroScore s ORDER BY s.score DESC")
    List<HungerHeroScore> findAllOrderByScoreDesc();

    /**
     * Top scores for a specific level.
     */
    @Query("SELECT s FROM HungerHeroScore s WHERE s.levelId = ?1 ORDER BY s.score DESC")
    List<HungerHeroScore> findByLevelOrderByScoreDesc(String levelId);
}
