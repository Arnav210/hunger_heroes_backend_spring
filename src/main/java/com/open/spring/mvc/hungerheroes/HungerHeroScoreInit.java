package com.open.spring.mvc.hungerheroes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the hunger_heroes_leaderboard table with demo data on first startup.
 *
 * Follows the same pattern as DonationInit — runs after core person/role init.
 */
@Component
@Order(11) // run after DonationInit (order 10)
public class HungerHeroScoreInit implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HungerHeroScoreInit.class);

    @Autowired
    private HungerHeroScoreRepository repo;

    @Override
    public void run(String... args) {
        try {
            if (repo.count() == 0) {
                HungerHeroScore[] seeds = HungerHeroScore.init();
                for (HungerHeroScore s : seeds) {
                    repo.save(s);
                }
                log.info("🎮 Hunger Heroes Game: seeded {} sample leaderboard entries", seeds.length);
            } else {
                log.info("🎮 Hunger Heroes Game: leaderboard already has {} entries, skipping seed",
                        repo.count());
            }
        } catch (Exception e) {
            log.warn("🎮 Hunger Heroes Game: leaderboard init encountered an issue — {}",
                    e.getMessage());
        }
    }
}
