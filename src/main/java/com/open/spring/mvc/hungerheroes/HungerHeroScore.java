package com.open.spring.mvc.hungerheroes;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hunger Heroes Game Leaderboard Entity.
 *
 * Tracks scores from the food bank exploration game.
 * Players earn points by visiting NPCs and completing dialogues.
 *
 * Score formula (calculated server-side):
 *   score = (npcsVisited * 100) + (dialoguesCompleted * 25) + timeBonus
 *   timeBonus = max(0, 300 - timePlayedSeconds)
 *
 * Pattern reference: gamespring2 rpg/games/Game.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hunger_heroes_leaderboard")
public class HungerHeroScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Link to person/user table. */
    private Long personId;

    /** UID of the person/user. */
    private String personUid;

    /** Display name for the player. */
    @Column(length = 100)
    private String playerName;

    /** Number of NPC stations visited (0-5). */
    private Integer npcsVisited;

    /** Total dialogue lines read / interactions completed. */
    private Integer dialoguesCompleted;

    /** Server-calculated score — never trust the client. */
    private Integer score;

    /** Session duration in seconds. */
    private Integer timePlayedSeconds;

    /** Level identifier, e.g. "hunger-heroes-foodbank". */
    @Column(length = 64)
    private String levelId;

    /** Extensible JSON payload for extra game data. */
    @Lob
    private String details;

    /** When the score was recorded. */
    private LocalDateTime createdAt;

    // ────────── Score computation ──────────

    /**
     * Compute score server-side.
     * Call this before persisting.
     *
     * Formula:
     *   score = (npcsVisited * 100) + (dialoguesCompleted * 25) + timeBonus
     *   timeBonus = max(0, 300 - timePlayedSeconds)
     */
    public void computeScore() {
        int npcPoints = (npcsVisited != null ? npcsVisited : 0) * 100;
        int dialoguePoints = (dialoguesCompleted != null ? dialoguesCompleted : 0) * 25;
        int timeBonus = Math.max(0, 300 - (timePlayedSeconds != null ? timePlayedSeconds : 0));
        this.score = npcPoints + dialoguePoints + timeBonus;
    }

    // ────────── Seed data ──────────

    /**
     * Seed data for testing / first-run.
     *
     * @return array of sample scores
     */
    public static HungerHeroScore[] init() {
        HungerHeroScore s1 = new HungerHeroScore();
        s1.setPersonId(1L);
        s1.setPersonUid("uid-demo-hero");
        s1.setPlayerName("DemoHero");
        s1.setNpcsVisited(5);
        s1.setDialoguesCompleted(25);
        s1.setTimePlayedSeconds(150);
        s1.setLevelId("hunger-heroes-foodbank");
        s1.setDetails("{\"allNpcsFound\": true}");
        s1.setCreatedAt(LocalDateTime.now());
        s1.computeScore();

        HungerHeroScore s2 = new HungerHeroScore();
        s2.setPersonId(2L);
        s2.setPersonUid("uid-speed-runner");
        s2.setPlayerName("SpeedRunner");
        s2.setNpcsVisited(3);
        s2.setDialoguesCompleted(12);
        s2.setTimePlayedSeconds(60);
        s2.setLevelId("hunger-heroes-foodbank");
        s2.setDetails("{\"speedRun\": true}");
        s2.setCreatedAt(LocalDateTime.now());
        s2.computeScore();

        return new HungerHeroScore[] { s1, s2 };
    }
}
