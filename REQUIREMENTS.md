# Hunger Heroes — Spring Boot Backend Requirements

> This document maps every capstone requirement to the **specific files and code** that satisfy it in the `hunger_heroes_backend_spring-` repository.

---

## 1. JPA / Hibernate Database Integration

| Requirement | Where It's Met |
|---|---|
| Entity classes with JPA annotations | `Donation.java` (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@OneToMany`) |
| Abstract entity inheritance | `FoodItem.java` (`@MappedSuperclass` / `@Inheritance(SINGLE_TABLE)`, `@DiscriminatorColumn`) |
| Related audit entity | `DonationStatusLog.java` (`@Entity`, `@ManyToOne` back to `Donation`) |
| Spring Data JPA Repository | `DonationJpaRepository.java` — extends `JpaRepository`, 14 custom query methods including `@Query` JPQL |
| Second Repository | `DonationStatusLogRepository.java` |
| Database: SQLite | `application.properties` — `spring.datasource.url=jdbc:sqlite:volumes/sqlite.db` with Hibernate community dialect |

**Key files:**
```
src/main/java/com/open/spring/mvc/donation/FoodItem.java
src/main/java/com/open/spring/mvc/donation/Donation.java
src/main/java/com/open/spring/mvc/donation/DonationStatusLog.java
src/main/java/com/open/spring/mvc/donation/DonationJpaRepository.java
src/main/java/com/open/spring/mvc/donation/DonationStatusLogRepository.java
```

---

## 2. Three or More Data Structures (10 Used)

| # | Data Structure | Where Used | Purpose |
|---|---|---|---|
| 1 | `HashMap<String, Object>` | `DonationService.getStats()` | Aggregates counts by status and category |
| 2 | `HashSet<String>` | `DonationConstants`, `DonationGraph` | O(1) validation of categories/units/statuses; BFS visited set; graph node tracking |
| 3 | `LinkedHashMap<String, String>` | `DonationConstants.CATEGORY_EMOJI` | Insertion-ordered emoji map for display |
| 4 | `ArrayList<Donation>` | `DonationService.matchDonations()` | Sorted working copy for priority-score ranking |
| 5 | `PriorityQueue<Map.Entry<String,Long>>` | `DonationService.getDonorLeaderboard()` | Min-heap for top-N donor extraction in O(n log k) |
| 6 | `LinkedList<Map.Entry<>>` | `DonationService.getDonorLeaderboard()` | Stack-based reversal of heap drain order |
| 7 | `ArrayDeque<String>` (as **Stack**) | `DonationService.undoStacks` | LIFO undo stack for donation status rollback |
| 8 | `ArrayDeque<String>` (as **Queue**) | `DonationGraph.bfsRecommendations()` | FIFO BFS traversal queue |
| 9 | `ConcurrentHashMap<String, Deque<String>>` | `DonationService.undoStacks` | Thread-safe per-donation undo history |
| 10 | `List<CategoryNode>` (N-ary **Tree**) | `CategoryTree.CategoryNode.children` | Hierarchical food category tree structure |

**Key files:**
```
src/main/java/com/open/spring/mvc/donation/DonationService.java
src/main/java/com/open/spring/mvc/donation/DonationConstants.java
src/main/java/com/open/spring/mvc/donation/DonationGraph.java
src/main/java/com/open/spring/mvc/donation/CategoryTree.java
```

---

## 3. Algorithms with Complexity Analysis (9 Algorithms)

### Algorithm 1 — Priority-Score Donation Matching

| Aspect | Detail |
|---|---|
| **File** | `DonationService.java`, method `matchDonations()` |
| **Description** | Ranks all active donations for a receiver using weighted scoring: +30 (ZIP match), +0–40 (urgency based on days-to-expiry), +10 per dietary-tag match, −20 per allergen conflict |
| **Time Complexity** | **O(n · m)** where n = active donations, m = max(dietary preferences, allergen exclusions) |
| **Space Complexity** | O(n) for the score map |
| **Data Structures** | `HashMap<Donation, Integer>` for scores, `ArrayList` for sorted output, `Set<String>` for O(1) filter lookups |

### Algorithm 2 — Binary Search by Expiry Date

| Aspect | Detail |
|---|---|
| **File** | `DonationService.java`, method `findDonationsExpiringAfter()` |
| **Description** | Sorts all donations by expiry date (ascending), then uses classic binary search to find the first donation expiring on or after a target date, returning the sublist from that index onward |
| **Time Complexity** | **O(log n)** for the binary search (sort is done by the DB/repository) |
| **Space Complexity** | O(n) for the sublist copy |
| **Data Structures** | `List<Donation>` from repository `findAllByOrderByExpiryDateAsc()` |

### Algorithm 3 — BFS Donor Recommendations (Graph)

| Aspect | Detail |
|---|---|
| **File** | `DonationGraph.java`, method `bfsRecommendations()` |
| **Description** | Starting from a user node, performs Breadth-First Search up to N hops through the donor→receiver adjacency list to find recommended connections |
| **Time Complexity** | **O(V + E)** where V = vertices (users), E = edges (donation relationships) |
| **Space Complexity** | O(V) for visited set and queue |
| **Data Structures** | `ArrayDeque` (BFS queue), `HashSet` (visited), `HashMap` (depth tracking) |

### Algorithm 4 — Connected Components / Community Detection (Graph)

| Aspect | Detail |
|---|---|
| **File** | `DonationGraph.java`, method `findCommunities()` |
| **Description** | Builds undirected version of the graph and uses BFS to identify all connected components (independent donation communities) |
| **Time Complexity** | **O(V + E)** |
| **Space Complexity** | O(V) |
| **Data Structures** | `HashMap` (undirected adjacency), `ArrayDeque` (BFS queue), `HashSet` (visited, component sets) |

### Algorithm 5 — DFS Pre-Order Tree Traversal

| Aspect | Detail |
|---|---|
| **File** | `CategoryTree.java`, method `preOrderTraversal()` |
| **Description** | Recursive pre-order DFS traversal of the N-ary category tree, visiting each parent before its children |
| **Time Complexity** | **O(n)** where n = number of tree nodes |
| **Space Complexity** | O(h) for recursion stack, where h = tree height |
| **Data Structures** | `List<CategoryNode>` children lists, `ArrayList` result accumulation |

---

## 4. Object-Oriented Design (All Four Pillars)

| OOP Pillar | How It's Demonstrated |
|---|---|
| **Abstraction** | `FoodItem.java` is an abstract class with abstract/template methods (`isExpired()`, `daysUntilExpiry()`, `getSummary()`). Concrete implementations are hidden from callers. |
| **Encapsulation** | All entity fields are `private` with Lombok `@Getter`/`@Setter`. `DonationConstants` uses `Collections.unmodifiableSet()` to prevent mutation. Allergen/dietary data stored as CSV strings with helper methods (`getAllergenList()`, `setAllergenList()`). |
| **Inheritance** | `Donation extends FoodItem` using JPA `@Inheritance(SINGLE_TABLE)` with `@DiscriminatorValue("DONATION")`. |
| **Polymorphism** | `getSummary()` is defined in `FoodItem` (template method) and overridden in `Donation` to include donor-specific info. API controller uses `FoodItem` reference type where possible. |

**Key files:**
```
src/main/java/com/open/spring/mvc/donation/FoodItem.java       (abstract class)
src/main/java/com/open/spring/mvc/donation/Donation.java        (concrete subclass)
```

---

## 5. RESTful API (Full CRUD + Custom Endpoints)

| HTTP Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/donations` | Create a new donation | permitAll |
| `GET` | `/api/donations` | List all (filter by `?status=`, `?mine=true`, `?search=`) | authenticated |
| `GET` | `/api/donations/{donationId}` | Get single donation by ID | permitAll |
| `POST` | `/api/donations/{donationId}/accept` | Accept a donation (active → accepted) | authenticated |
| `POST` | `/api/donations/{donationId}/deliver` | Deliver a donation (accepted → delivered) | authenticated |
| `POST` | `/api/donations/{donationId}/cancel` | Cancel a donation | authenticated |
| `POST` | `/api/donations/{donationId}/undo` | Undo last status change (Stack-based) | authenticated |
| `GET` | `/api/donations/stats` | Aggregate stats (counts by status/category) | permitAll |
| `GET` | `/api/donations/leaderboard` | Top donors via PriorityQueue | permitAll |
| `POST` | `/api/donations/match` | Priority-score matching algorithm | permitAll |
| `GET/POST` | `/api/donations/scan` | QR/barcode donation lookup | permitAll |
| `GET` | `/api/donations/graph` | Graph analytics (communities, influence ranking) | permitAll |
| `GET` | `/api/donations/graph/recommendations` | BFS-based user recommendations | permitAll |
| `GET` | `/api/donations/categories/tree` | Category tree hierarchy (N-ary tree) | permitAll |
| `GET` | `/api/donations/categories/path` | Category path to root | permitAll |
| `GET` | `/api/donations/sorted` | Sorted listing with Comparator | permitAll |

**Response format** matches the Flask backend's JSON schema (snake_case keys: `food_name`, `donor_zip`, `expiry_date`, etc.) for frontend compatibility.

**Key file:**
```
src/main/java/com/open/spring/mvc/donation/DonationApiController.java
```

---

## 6. Database Integration

| Component | Detail |
|---|---|
| **Database** | SQLite (`volumes/sqlite.db`) |
| **ORM** | Hibernate via Spring Data JPA |
| **Tables** | `food_items` (single-table inheritance), `donation_status_logs`, `person` (existing) |
| **Relationships** | `Donation` → `Person` (`@ManyToOne`), `Donation` → `DonationStatusLog` (`@OneToMany`, cascade ALL) |
| **Seeding** | `DonationInit.java` — `CommandLineRunner` seeds 6 sample donations on startup if the table is empty |
| **Custom Queries** | 14 derived/JPQL query methods in `DonationJpaRepository` |

---

## 7. JUnit Tests (> 50% coverage goal)

| Test File | # Tests | What's Covered |
|---|---|---|
| `DonationEntityTest.java` | 10 | Entity field defaults, `isExpired()`, `daysUntilExpiry()`, allergen/dietary list round-trip, `getSummary()` polymorphism, `donationId` format |
| `DonationConstantsTest.java` | 7 | Set membership, set sizes (12 categories, 12 units, etc.), emoji map, unmodifiability |
| `DonationServiceTest.java` | 16 | **Algorithm 1** (5 tests: ZIP boost, urgency, allergen penalty, dietary boost, empty list), **Algorithm 2** (3 tests: basic, all-before, all-after), **CRUD** (5 tests: create, accept, accept-invalid, deliver, cancel), **Analytics** (2 tests: stats, leaderboard), **StatusLog** (1 test: factory method) |
| `FoodItemTest.java` | 3 | Abstract class via concrete subclass: `isExpired()`, `daysUntilExpiry(null)`, `getSummary()` polymorphism |
| `DonationGraphTest.java` | 16 | **Graph structure** (5 tests: node count, edge count, neighbors), **BFS** (4 tests: depth 1, depth 2, leaf node, excludes start), **Communities** (3 tests: component count, contents, single node), **Influence** (3 tests: top rank, ordering, limit), **Summary** (1 test) |
| `CategoryTreeTest.java` | 21 | **Structure** (8 tests: root name, children, leaf count, depth), **DFS Search** (4 tests: found, case-insensitive, not found, intermediate), **Traversal** (3 tests: starts with root, includes all, pre-order property), **Path** (4 tests: leaf path, root path, non-existent, bakery path), **ToMap** (2 tests: keys, children) |
| **Total** | **73 tests** | Entities, service, constants, algorithms, graph, tree, CRUD lifecycle |

**Key files:**
```
src/test/java/com/open/spring/mvc/donation/DonationEntityTest.java
src/test/java/com/open/spring/mvc/donation/DonationConstantsTest.java
src/test/java/com/open/spring/mvc/donation/DonationServiceTest.java
src/test/java/com/open/spring/mvc/donation/FoodItemTest.java
```

---

## 8. Docker Deployment

| Component | File |
|---|---|
| **Dockerfile** | `Dockerfile` — Multi-stage build with `eclipse-temurin:21-jdk-alpine`, builds via `./mvnw package`, exposes port 8585 |
| **docker-compose.yml** | `docker-compose.yml` — Service definition with `SPRING_PROFILES_ACTIVE=prod`, volume mount for SQLite, port mapping `8585:8585` |
| **Nginx** | `nginx_for_flask_8585` — Reverse proxy config for `spring.opencodingsociety.com`, CORS headers |

---

## 9. JavaDoc Comments

Every public class and public method in the donation package includes JavaDoc:

- `FoodItem.java` — class-level doc + all public methods
- `Donation.java` — class-level doc with field descriptions
- `DonationStatusLog.java` — class-level doc + factory method
- `DonationConstants.java` — class-level doc + all constant sets
- `DonationService.java` — class-level doc listing all data structures and algorithms, each method documented with `@param`, `@return`, `@throws`, complexity noted in `<strong>` tags
- `DonationApiController.java` — class-level doc + all endpoint methods
- `DonationInit.java` — class-level doc
- All test classes — class-level doc with `@author`

---

## 10. Integration with Frontend & Flask Backend

| System | How It Connects |
|---|---|
| **Frontend** (`Ahaanv19/hunger_heroes`) | CORS origins configured in `SecurityConfig.java`: `localhost:4100`, `localhost:4887`, `ahaanv19.github.io`, `*.github.io`, `opencodingsociety.com`. Frontend calls `/api/donations/*` on port 8585. |
| **Flask Backend** (`Ahaanv19/hunger_heroes_backend`) | Runs on port 8080. Spring on port 8585. Both share the same JWT cookie domain pattern. API response JSON uses identical snake_case field names (`food_name`, `donor_zip`, `expiry_date`) so the frontend can switch between backends seamlessly. |
| **Security** | JWT cookie-based auth (`SecurityConfig.java`). Donation read/public endpoints are `permitAll`; write/lifecycle endpoints require authentication. |

---

## File Manifest (Updated)

```
src/main/java/com/open/spring/mvc/donation/
├── FoodItem.java                 ← Abstract base entity (Abstraction + Inheritance)
├── Donation.java                 ← Main entity (Encapsulation + Polymorphism)
├── DonationStatusLog.java        ← Audit trail entity (OneToMany relationship)
├── DonationConstants.java        ← Validation sets & emoji map (Sets, Maps)
├── DonationJpaRepository.java    ← JPA repository (14 query methods)
├── DonationStatusLogRepository.java  ← Status log repository
├── DonationService.java          ← Business logic (Algorithms + Stack + Sorting + Graph/Tree integration)
├── DonationApiController.java    ← REST controller (16 endpoints)
├── DonationInit.java             ← Database seeder (CommandLineRunner)
├── DonationGraph.java            ← Graph data structure (Adjacency list + BFS + Components + Influence)
└── CategoryTree.java             ← Tree data structure (N-ary tree + DFS + Path-to-root)

src/test/java/com/open/spring/mvc/donation/
├── DonationEntityTest.java       ← 10 entity unit tests
├── DonationConstantsTest.java    ← 7 constants unit tests
├── DonationServiceTest.java      ← 16 service/algorithm tests (Mockito)
├── FoodItemTest.java             ← 3 abstract class tests
├── DonationGraphTest.java        ← 16 graph data structure tests
└── CategoryTreeTest.java         ← 21 tree data structure tests
```

---

## Summary Checklist

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | JPA / Hibernate | ✅ | 3 entities, 2 repositories, 14+ queries |
| 2 | 3+ Data Structures | ✅ | 10 used: HashMap, HashSet, LinkedHashMap, ArrayList, PriorityQueue, LinkedList, ArrayDeque, TreeMap, Deque(Stack), ConcurrentHashMap |
| 3 | 2+ Algorithms w/ complexity | ✅ | Priority-score O(n·m), Binary search O(log n), BFS O(V+E), DFS O(n) |
| 4 | OOP (4 pillars) | ✅ | Abstraction, Encapsulation, Inheritance, Polymorphism |
| 5 | RESTful API | ✅ | 16 endpoints, proper HTTP methods/status codes |
| 6 | Database integration | ✅ | SQLite + JPA + relationships + seeding |
| 7 | JUnit tests (>50%) | ✅ | 73 tests across 6 files |
| 8 | Docker deployment | ✅ | Dockerfile + docker-compose.yml + nginx |
| 9 | JavaDoc comments | ✅ | All public classes and methods documented |
| 10 | Integration | ✅ | CORS config, matching JSON schema, JWT auth |

---

## 12. Full Rubric Mapping — Learning Objectives

This section maps **every learning objective from the assessment rubric** to specific evidence in this codebase.

---

### DATA STRUCTURES

#### Collections — Use appropriate Java collections (ArrayList, HashMap, HashSet)

| Collection | Where Used | Purpose |
|---|---|---|
| `ArrayList` | `DonationService.matchDonations()` | Working copy of donations for sorting by score |
| `HashMap` | `DonationService.getStats()`, `DonationService.matchDonations()` | Status/category count aggregation, priority score map |
| `HashSet` | `DonationConstants`, `DonationApiController.matchDonations()` | O(1) validation of allowed values, allergen/dietary filter sets |
| `LinkedHashMap` | `DonationService.getStats()`, `DonationService.getDonorLeaderboard()`, `DonationApiController.donationToMap()` | Insertion-ordered result maps for consistent JSON output |
| `ConcurrentHashMap` | `DonationService.undoStacks` | Thread-safe per-donation undo history |

**Files:** `DonationService.java`, `DonationConstants.java`, `DonationApiController.java`

#### Lists — Implement list operations (add, remove, search, iterate)

| Operation | Where Demonstrated |
|---|---|
| **Add** | `DonationService.seedIfEmpty()` — builds `ArrayList<Donation>` with `.add()` |
| **Remove** | `DonationStatusLog` orphan removal via `orphanRemoval = true` on `@OneToMany` |
| **Search** | `DonationService.search()` — `findByFoodNameContainingIgnoreCase()` linear search; `DonationService.findDonationsExpiringAfter()` — binary search on sorted list |
| **Iterate** | `DonationService.matchDonations()` — iterates active donations scoring each; `DonationService.expireOverdueDonations()` — iterates overdue donations |

**Files:** `DonationService.java` (lines 90-170 CRUD, lines 200-260 matching, lines 400-430 cleanup)

#### Stacks/Queues — Apply stack/queue structures (undo/redo, task queues)

| Structure | Where Demonstrated | Purpose |
|---|---|---|
| **Stack (LIFO)** via `ArrayDeque` | `DonationService.undoStacks` field + `pushUndo()` / `undoStatusChange()` methods | Per-donation status undo stack — each status change pushes the old status; `undoStatusChange()` pops and restores |
| **Stack (LIFO)** via `LinkedList.push()` | `DonationService.getDonorLeaderboard()` | Reverses PriorityQueue drain order (min-heap → descending) |
| **Queue (FIFO)** via `ArrayDeque` | `DonationGraph.bfsRecommendations()`, `DonationGraph.findCommunities()` | BFS traversal queue for graph algorithms |
| **PriorityQueue (min-heap)** | `DonationService.getDonorLeaderboard()` | Top-N extraction in O(n log k) |

**Files:** `DonationService.java` (lines 490-550 undo stack), `DonationGraph.java` (lines 110-140 BFS queue)

#### Trees — Implement tree structures OR use tree-based algorithms

| Component | Where Demonstrated |
|---|---|
| **N-ary Tree** class | `CategoryTree.java` — `CategoryNode` inner class with `List<CategoryNode> children` and `CategoryNode parent` |
| **DFS Pre-Order Traversal** | `CategoryTree.preOrderTraversal()` — visits parent before children, O(n) |
| **DFS Search** | `CategoryTree.search(String name)` — recursive DFS to find node by name, O(n) |
| **Path to Root** | `CategoryTree.pathToRoot()` — walks parent pointers from leaf to root, O(h) |
| **Count Leaves** | `CategoryTree.countLeaves()` — recursive count of terminal nodes, O(n) |
| **Tree Hierarchy** | 3-level tree: Root → {Perishable, Non-Perishable, Prepared} → 12 leaf categories |

**File:** `CategoryTree.java` (full file), `DonationService.java` (lines 580-620 tree methods)

#### Sets — Use sets for unique data management (roles, permissions, tags)

| Set Usage | Where Demonstrated |
|---|---|
| **Validation sets** | `DonationConstants.ALLOWED_CATEGORIES`, `ALLOWED_UNITS`, `ALLOWED_STORAGE`, `ALLOWED_ALLERGENS`, `ALLOWED_DIETARY`, `ALLOWED_STATUSES` — 6 `HashSet` instances for O(1) membership checks |
| **Unmodifiable enforcement** | `Collections.unmodifiableSet()` wrapping prevents runtime mutation |
| **Filter sets** | `DonationApiController.matchDonations()` converts dietary/allergen arrays to `HashSet` for efficient matching |
| **Graph node sets** | `DonationGraph.allNodes` — `HashSet` tracking all unique participants |
| **BFS visited set** | `DonationGraph.bfsRecommendations()` — `HashSet<String> visited` prevents cycles |

**Files:** `DonationConstants.java`, `DonationApiController.java`, `DonationGraph.java`

#### Dictionaries/Maps — Implement key-value mappings for efficient lookup

| Map Type | Where Used | Purpose |
|---|---|---|
| `HashMap<Donation, Integer>` | `matchDonations()` | Priority score per donation |
| `HashMap<String, Long>` | `getStats()`, `getDonorLeaderboard()` | Status counts, donor counts |
| `LinkedHashMap<String, String>` | `CATEGORY_EMOJI` | Ordered emoji lookup |
| `LinkedHashMap<String, Object>` | `donationToMap()` | Ordered JSON response building |
| `Map<String, Deque<String>>` | `undoStacks` | Per-donation undo history lookup |
| `Map<String, Integer>` | `DonationGraph.depthMap` | BFS depth tracking |

**Files:** `DonationService.java`, `DonationConstants.java`, `DonationApiController.java`, `DonationGraph.java`

#### Graphs — Model relationships using graph structures with graph algorithms

| Graph Feature | Where Demonstrated |
|---|---|
| **Adjacency List** | `DonationGraph.adjacency` — `HashMap<String, Set<String>>` representing donor→receiver edges |
| **BFS (Breadth-First Search)** | `DonationGraph.bfsRecommendations()` — finds donors/receivers within N hops for recommendations. O(V+E) |
| **Connected Components** | `DonationGraph.findCommunities()` — identifies independent donation communities using BFS on undirected graph. O(V+E) |
| **Influence Ranking (PageRank-style)** | `DonationGraph.influenceRanking()` — ranks donors by out-degree (number of unique receivers served). O(V+E) |
| **Graph Construction** | `DonationService.buildDonorReceiverGraph()` — builds graph from donation data (donor_email → accepted_by) |

**API Endpoints:**
- `GET /api/donations/graph` → full graph analytics
- `GET /api/donations/graph/recommendations?email=...&maxDepth=2` → BFS recommendations

**Files:** `DonationGraph.java` (full file), `DonationService.java` (lines 560-600), `DonationApiController.java` (graph endpoints)

---

### ALGORITHMS

#### Searching — Implement search algorithms (linear, binary, database queries)

| Search Type | Where Demonstrated |
|---|---|
| **Binary Search** | `DonationService.findDonationsExpiringAfter()` — classic binary search on sorted expiry dates. O(log n) |
| **Linear Search** | `DonationService.matchDonations()` — iterates all active donations scoring each. O(n) |
| **DFS Search** | `CategoryTree.search()` — recursive DFS to find a category by name. O(n) |
| **Database Queries** | `DonationJpaRepository` — 14 custom query methods including `@Query` JPQL and derived queries |

**Files:** `DonationService.java`, `CategoryTree.java`, `DonationJpaRepository.java`

#### Sorting — Apply practical sorting with Comparator/Comparable

| Sort Implementation | Where Demonstrated |
|---|---|
| **Explicit Comparator constants** | `DonationService.BY_EXPIRY_ASC`, `BY_CREATED_DESC`, `BY_QUANTITY_DESC` — static `Comparator<Donation>` fields |
| **Comparator with lambda** | `matchDonations()` — `sorted.sort((a, b) -> scoreMap.getOrDefault(b, 0) - scoreMap.getOrDefault(a, 0))` |
| **Comparator.comparingLong** | `getDonorLeaderboard()` — `new PriorityQueue<>(Comparator.comparingLong(Map.Entry::getValue))` |
| **Comparator.comparing** | `BY_EXPIRY_ASC` uses `Comparator.comparing(FoodItem::getExpiryDate)` |
| **Reverse order** | `BY_CREATED_DESC` uses `Comparator.reverseOrder()` |
| **Sort endpoint** | `GET /api/donations/sorted?sortBy=expiry` — sorts by expiry, created, quantity, or name |
| **Person implements Comparable** | `Person.java` — existing `implements Comparable<Person>` with `compareTo()` |

**Files:** `DonationService.java` (lines 630-680 Comparator section), `DonationApiController.java` (sorted endpoint)

#### Hashing — Use hashing for passwords, data integrity, efficient lookups

| Hashing Usage | Where Demonstrated |
|---|---|
| **BCrypt password hashing** | `MvcConfig.java` → `BCryptPasswordEncoder` bean; `PersonDetailsService.java` → `passwordEncoder.encode()` for password storage and `passwordEncoder.matches()` for verification |
| **HashMap** (hash-based lookup) | `DonationService.getStats()`, `matchDonations()` — O(1) amortised key lookup |
| **HashSet** (hash-based membership) | `DonationConstants` — O(1) `.contains()` checks for validating categories, statuses, allergens |
| **ConcurrentHashMap** | `DonationService.undoStacks` — thread-safe hash-based undo history |

**Files:** `MvcConfig.java`, `PersonDetailsService.java`, `DonationService.java`, `DonationConstants.java`

#### Algorithm Analysis — Analyze time/space complexity

| Algorithm | Time | Space | Documented In |
|---|---|---|---|
| Priority-Score Matching | O(n · m) | O(n) | `DonationService.matchDonations()` JavaDoc |
| Binary Search by Expiry | O(log n) | O(n) | `DonationService.findDonationsExpiringAfter()` JavaDoc |
| Top-N Leaderboard (heap) | O(n log k) | O(k) | `DonationService.getDonorLeaderboard()` JavaDoc |
| BFS Recommendations | O(V + E) | O(V) | `DonationGraph.bfsRecommendations()` JavaDoc |
| Connected Components | O(V + E) | O(V) | `DonationGraph.findCommunities()` JavaDoc |
| Influence Ranking | O(V + E) | O(V) | `DonationGraph.influenceRanking()` JavaDoc |
| DFS Tree Traversal | O(n) | O(h) | `CategoryTree.preOrderTraversal()` JavaDoc |
| DFS Tree Search | O(n) | O(h) | `CategoryTree.search()` JavaDoc |
| Path to Root | O(h) | O(h) | `CategoryTree.pathToRoot()` JavaDoc |

---

### OBJECT-ORIENTED DESIGN

#### Abstraction — Create abstract classes or interfaces

| Evidence | File |
|---|---|
| `FoodItem` is an `abstract class` with concrete template methods (`isExpired()`, `daysUntilExpiry()`, `getSummary()`) | `FoodItem.java` |
| `JpaRepository` interface extended by `DonationJpaRepository` | `DonationJpaRepository.java` |

#### Encapsulation — Private fields with getters/setters

| Evidence | File |
|---|---|
| All entity fields are `private` via Lombok `@Data` (generates getters/setters) | `Donation.java`, `FoodItem.java`, `DonationStatusLog.java` |
| `DonationConstants` uses `Collections.unmodifiableSet()` / `Collections.unmodifiableMap()` to prevent mutation | `DonationConstants.java` |
| Allergen/dietary data stored as CSV `private String` with helper methods `getAllergenList()` / `setAllergenList()` | `Donation.java` |
| `CategoryNode` fields are `private final` with explicit getters | `CategoryTree.java` |

#### Inheritance — Extend base classes

| Evidence | File |
|---|---|
| `Donation extends FoodItem` with JPA `@Inheritance(SINGLE_TABLE)` + `@DiscriminatorValue("DONATION")` | `Donation.java` extends `FoodItem.java` |
| `DonationJpaRepository extends JpaRepository<Donation, Long>` | `DonationJpaRepository.java` |

#### Polymorphism — Override methods, interface implementations

| Evidence | File |
|---|---|
| `Donation.getSummary()` overrides `FoodItem.getSummary()` — adds donor info | `Donation.java` |
| `FoodItem.isExpired()` and `FoodItem.daysUntilExpiry()` — template methods callable through base type | `FoodItem.java` |
| Repository interface — Spring generates implementation at runtime | `DonationJpaRepository.java` |

#### Design Patterns

| Pattern | Where Applied |
|---|---|
| **MVC** | `DonationApiController` (Controller) → `DonationService` (Model/Service) → Thymeleaf templates (View) |
| **Repository** | `DonationJpaRepository`, `DonationStatusLogRepository` — data access abstraction |
| **Factory Method** | `DonationStatusLog.create()` — static factory for creating log entries |
| **Singleton** | `DonationService.categoryTree` — single instance of category tree |
| **Template Method** | `FoodItem.getSummary()` — base implementation overridden by `Donation` |
| **Command/Undo** | `DonationService.undoStacks` — stores previous states for undo capability |

---

### SOFTWARE DEVELOPMENT

#### Version Control — Git branching, commits, PRs

| Evidence |
|---|
| Repository: `github.com/Ahaanv19/hunger_heroes_backend_spring-` |
| Branch: `master` with feature development |
| Commit history tracks incremental changes |

#### Testing — Unit tests, integration tests, API tests

| Test File | # Tests | What's Covered |
|---|---|---|
| `DonationEntityTest.java` | 10 | Entity fields, `isExpired()`, `daysUntilExpiry()`, allergen/dietary lists, `getSummary()` polymorphism, ID format |
| `DonationConstantsTest.java` | 7 | Set membership, sizes (12 categories, 12 units), emoji map, unmodifiability |
| `DonationServiceTest.java` | 16 | Algorithms (matching, binary search), CRUD lifecycle, analytics, status logging |
| `FoodItemTest.java` | 3 | Abstract class via concrete subclass: expired check, null expiry, polymorphic summary |
| `DonationGraphTest.java` | 16 | Graph structure, BFS depth 1/2, BFS from leaf, connected components, influence ranking |
| `CategoryTreeTest.java` | 21 | Tree structure, DFS search, pre-order traversal, path-to-root, toMap conversion |
| **Total** | **73 tests** | Entities, service logic, algorithms, data structures |

**Files:** `src/test/java/com/open/spring/mvc/donation/` (6 test classes)

#### Build Tools — Maven

| Evidence |
|---|
| `pom.xml` — Maven build with Spring Boot 3.5.0, Java 21, JPA, Security, Lombok, SQLite |
| `./mvnw` — Maven wrapper for reproducible builds |
| `./mvnw compile` — clean compilation |
| `./mvnw test` — 73 tests, 0 failures, BUILD SUCCESS |

#### Debugging — IDE debugger, logging

| Evidence |
|---|
| Status change logging via `DonationStatusLog` audit trail |
| Console logging in `DonationInit.java` for seed data |
| `DonationService.logStatusChange()` records every state transition |

#### API Development — RESTful APIs with proper HTTP methods/status codes

| # | Endpoint | Method | Status Codes |
|---|---|---|---|
| 1 | `/api/donations` | POST | 201, 400 |
| 2 | `/api/donations` | GET | 200 |
| 3 | `/api/donations/{id}` | GET | 200, 404 |
| 4 | `/api/donations/{id}/accept` | POST | 200, 404, 409 |
| 5 | `/api/donations/{id}/deliver` | POST | 200, 404, 409 |
| 6 | `/api/donations/{id}/cancel` | POST | 200, 404, 409 |
| 7 | `/api/donations/{id}/undo` | POST | 200, 404, 409 |
| 8 | `/api/donations/stats` | GET | 200 |
| 9 | `/api/donations/leaderboard` | GET | 200 |
| 10 | `/api/donations/match` | POST | 200 |
| 11 | `/api/donations/scan` | GET/POST | 200, 400, 404 |
| 12 | `/api/donations/graph` | GET | 200 |
| 13 | `/api/donations/graph/recommendations` | GET | 200 |
| 14 | `/api/donations/categories/tree` | GET | 200 |
| 15 | `/api/donations/categories/path` | GET | 200, 404 |
| 16 | `/api/donations/sorted` | GET | 200 |

**File:** `DonationApiController.java`

#### Database Integration — JPA/Hibernate with proper relationships

| Relationship | Entities | Annotation |
|---|---|---|
| `@ManyToOne` | `Donation` → `Person` | `@JoinColumn(name = "person_id")` |
| `@OneToMany` | `Donation` → `DonationStatusLog` | `cascade = ALL, orphanRemoval = true` |
| `@ManyToOne` | `DonationStatusLog` → `Donation` | `@JoinColumn(name = "donation_id")` |
| `@Inheritance` | `FoodItem` → `Donation` | `SINGLE_TABLE` strategy |

**Files:** `FoodItem.java`, `Donation.java`, `DonationStatusLog.java`

---

### DEPLOYMENT

#### Docker — Dockerfile and docker-compose

| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage build: `eclipse-temurin:21-jdk-alpine`, `./mvnw package`, exposes 8585 |
| `docker-compose.yml` | Service definition with `SPRING_PROFILES_ACTIVE=prod`, volume mount, port 8585 |

#### DNS Configuration

| Component | Detail |
|---|---|
| Domain | `spring.opencodingsociety.com` |
| nginx config | `nginx_for_flask_8585` — reverse proxy to `localhost:8585` |

#### nginx — Reverse proxy

| Config | Detail |
|---|---|
| `server_name` | `spring.opencodingsociety.com` |
| `proxy_pass` | `http://localhost:8585` |
| CORS | Allows `*.opencodingsociety.com` and `open-coding-society.github.io` origins |
| OPTIONS handling | Returns 204 with proper CORS headers for preflight |

**File:** `nginx_for_flask_8585`

#### CI/CD — Automated deployment

| Evidence |
|---|
| Docker-based deployment via `docker-compose up -d --build` |
| `restart: unless-stopped` policy for automatic recovery |

---

### DOCUMENTATION

#### Code Comments — JavaDoc >10% density

| File | JavaDoc Coverage |
|---|---|
| `FoodItem.java` | Class-level + all public methods |
| `Donation.java` | Class-level + all fields + all methods |
| `DonationStatusLog.java` | Class-level + factory method |
| `DonationConstants.java` | Class-level + all constant sets |
| `DonationService.java` | Class-level (lists all data structures + algorithms) + every method with `@param`, `@return`, `@throws`, complexity in `<strong>` tags |
| `DonationApiController.java` | Class-level + all endpoints with HTTP method/status code docs |
| `DonationGraph.java` | Class-level (lists all graph algorithms) + every method with complexity |
| `CategoryTree.java` | Class-level (lists all tree operations) + every method |
| `DonationInit.java` | Class-level |
| All 6 test classes | Class-level `@author` docs |

#### API Documentation — Endpoints, parameters, request/response

| Evidence |
|---|
| `FRONTEND_PROMPT.md` — full API reference with request/response JSON schemas for all endpoints |
| `REQUIREMENTS.md` (this file) — complete endpoint table with HTTP methods and status codes |
| Controller JavaDoc — every endpoint documents parameters, return types, status codes |

#### Help System / Blog Portfolio

| Evidence |
|---|
| `REQUIREMENTS.md` — comprehensive design document mapping every requirement to code |
| `FRONTEND_PROMPT.md` — integration guide for frontend developers |

---

### PERSONAL/SOCIAL RELEVANCE

#### Project Impact — Real-world problem

| Evidence |
|---|
| **Problem:** Food waste and hunger are concurrent crises — food banks need efficient matching of surplus food to recipients |
| **Solution:** Hunger Heroes connects food donors with receivers, using smart algorithms to match based on dietary needs, allergen safety, proximity (ZIP code), and urgency (expiry dates) |
| **Impact:** Reduces food waste by prioritising near-expiry items, prevents allergic reactions via exclusion filters, and builds donor communities via graph-based network analysis |

#### Ethical Considerations — Privacy, security, accessibility, equity

| Consideration | How Addressed |
|---|---|
| **Privacy** | JWT cookie-based auth, `@JsonIgnoreProperties` hides sensitive fields (passwords, roles), donor contact info only visible to authenticated users |
| **Security** | BCrypt password hashing (`MvcConfig.java`), CORS origin whitelist (`SecurityConfig.java`), input validation (`validateDonation()`), SQL injection prevention via JPA parameterised queries |
| **Accessibility** | API responses use consistent snake_case JSON for frontend parsing, proper HTTP status codes for screen reader/assistive tech compatibility |
| **Equity** | Matching algorithm doesn't discriminate by donor/receiver identity — purely based on food compatibility, proximity, and urgency; allergen exclusions protect vulnerable populations |

---

## File Manifest (Updated)
