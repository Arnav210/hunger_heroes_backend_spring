# Frontend Prompt — Hunger Heroes Spring Backend Integration

> **Copy-paste this prompt into the `hunger_heroes` frontend repo to build the frontend pages that connect to the Spring Boot backend.**

---

## Prompt

I need you to build frontend pages in my Jekyll GitHub Pages site (`hunger_heroes`) that integrate with my **Spring Boot backend** for the Hunger Heroes food donation system. The Spring backend runs alongside the existing Flask backend — they are **two separate servers**. The frontend already talks to Flask via `pythonURI`; it must now also talk to Spring via `javaURI`.

---

### 1. Update `config.js`

The file `assets/js/api/config.js` already exports `pythonURI` and `javaURI`, but **`javaURI` currently points to the same URL as `pythonURI`** (port 8288). Fix it so `javaURI` points to the Spring Boot server:

```js
// LOCAL
export const javaURI = "http://localhost:8585";

// PRODUCTION (deployed)
// export const javaURI = "https://spring.opencodingsociety.com";
```

`pythonURI` stays exactly as it is (Flask on port 8288 / `hungerheros.opencodingsociety.com`).  
`fetchOptions` stays exactly as it is (`credentials: 'include'`, `mode: 'cors'`).

Every new file must import from config like this:

```js
import { pythonURI, javaURI, fetchOptions } from '{{site.baseurl}}/assets/js/api/config.js';
```

---

### 2. Which Backend Serves What

| Feature | Backend | Base URL variable | Endpoint |
|---|---|---|---|
| **Create donation** | Flask | `pythonURI` | `POST /api/donations` |
| **List donations** | Flask | `pythonURI` | `GET /api/donations` |
| **My donations** | Flask | `pythonURI` | `GET /api/donations?mine=true` |
| **Get single donation** | Flask | `pythonURI` | `GET /api/donations/<id>` |
| **Update status** | Flask | `pythonURI` | `PATCH /api/donations/<id>/status` |
| **QR/Barcode scan** | Flask | `pythonURI` | `POST /api/donations/scan` |
| **Generate label** | Flask | `pythonURI` | `GET /api/donations/<id>/label` |
| **Dashboard stats** | Flask | `pythonURI` | `GET /api/donations/stats` |
| **Donation matching** ⭐ | **Spring** | `javaURI` | `POST /api/donations/match` |
| **Leaderboard** ⭐ | **Spring** | `javaURI` | `GET /api/donations/leaderboard` |
| **Accept donation** ⭐ | **Spring** | `javaURI` | `POST /api/donations/{id}/accept` |
| **Deliver donation** ⭐ | **Spring** | `javaURI` | `POST /api/donations/{id}/deliver` |
| **Cancel donation** ⭐ | **Spring** | `javaURI` | `POST /api/donations/{id}/cancel` |
| **Spring stats** ⭐ | **Spring** | `javaURI` | `GET /api/donations/stats` |
| **Spring scan** ⭐ | **Spring** | `javaURI` | `GET /api/donations/scan?scan_data=<id>` |
| **Undo status change** ⭐ | **Spring** | `javaURI` | `POST /api/donations/{id}/undo` |
| **Graph analytics** ⭐ | **Spring** | `javaURI` | `GET /api/donations/graph` |
| **Graph recommendations** ⭐ | **Spring** | `javaURI` | `GET /api/donations/graph/recommendations?email=...&maxDepth=2` |
| **Category tree** ⭐ | **Spring** | `javaURI` | `GET /api/donations/categories/tree` |
| **Category path** ⭐ | **Spring** | `javaURI` | `GET /api/donations/categories/path?category=...` |
| **Sorted donations** ⭐ | **Spring** | `javaURI` | `GET /api/donations/sorted?sortBy=expiry&status=active` |

The ⭐ endpoints are **Spring-exclusive** features or Spring-side duplicates. Existing Flask pages (create, history, scan, barcode) should keep using `pythonURI`. **New** pages for matching, leaderboard, and lifecycle actions use `javaURI`.

---

### 3. Spring API Reference (JSON Schemas)

#### `POST /api/donations/match` — Smart Matching (Spring-only)

**Request:**
```json
{
  "zip": "92127",
  "dietary_prefs": ["vegetarian", "halal"],
  "allergen_exclusions": ["peanuts", "dairy"]
}
```

**Response:** `200 OK` — array of matched donations ranked by score:
```json
[
  {
    "id": "HH-M3X7K9-AB2F",
    "food_name": "Veggie Pasta",
    "category": "prepared",
    "quantity": 5,
    "unit": "servings",
    "expiry_date": "2026-03-20",
    "storage": "refrigerated",
    "allergens": ["gluten"],
    "dietary_tags": ["vegetarian"],
    "status": "active",
    "donor_name": "Jane Doe",
    "donor_zip": "92127",
    "days_until_expiry": 7,
    "is_expired": false
  }
]
```

#### `GET /api/donations/leaderboard?limit=10` — Donor Leaderboard (Spring-only)

**Response:** `200 OK`
```json
{
  "jane@example.com": 12,
  "john@example.com": 8,
  "alice@example.com": 5
}
```

#### `POST /api/donations/{donationId}/accept`

**Request (optional body):**
```json
{ "accepted_by": "receiver@example.com" }
```

**Response:** `200 OK`
```json
{
  "message": "Donation accepted",
  "donation_id": "HH-M3X7K9-AB2F",
  "status": "accepted"
}
```

#### `POST /api/donations/{donationId}/deliver`

**Request (optional body):**
```json
{ "delivered_by": "volunteer@example.com" }
```

**Response:** `200 OK`
```json
{
  "message": "Donation delivered",
  "donation_id": "HH-M3X7K9-AB2F",
  "status": "delivered"
}
```

#### `POST /api/donations/{donationId}/cancel`

No body required. **Response:** `200 OK`
```json
{
  "message": "Donation cancelled",
  "donation_id": "HH-M3X7K9-AB2F",
  "status": "cancelled"
}
```

#### `GET /api/donations/stats` (Spring version)

**Response:** `200 OK`
```json
{
  "total": 42,
  "byStatus": { "active": 15, "accepted": 10, "delivered": 12, "cancelled": 3, "expired": 2 },
  "byCategory": { "prepared": 10, "produce": 8, "dairy": 6, "bakery": 5 },
  "expiringSoon": 4
}
```

#### `POST /api/donations/{donationId}/undo` — Undo Last Status Change (Spring-only)

Reverts the most recent status change for a donation using an internal stack.

**Response:** `200 OK`
```json
{
  "message": "Status reverted",
  "donation_id": "HH-M3X7K9-AB2F",
  "previous_status": "accepted",
  "restored_status": "active"
}
```

**Error (nothing to undo):** `400 Bad Request`
```json
{ "error": "No status history to undo for donation HH-M3X7K9-AB2F" }
```

#### `GET /api/donations/graph` — Donor-Receiver Network Graph (Spring-only)

Returns graph analytics about donor ↔ receiver connections: communities of connected users and influence ranking.

**Response:** `200 OK`
```json
{
  "summary": { "nodes": 8, "edges": 12, "communities": 2 },
  "communities": [
    ["alice@example.com", "bob@example.com", "charlie@example.com"],
    ["dave@example.com", "eve@example.com"]
  ],
  "influenceRanking": {
    "alice@example.com": 5,
    "bob@example.com": 3
  }
}
```

#### `GET /api/donations/graph/recommendations?email=alice@example.com&maxDepth=2` — BFS Recommendations (Spring-only)

Uses breadth-first search on the donor-receiver graph to find users within `maxDepth` hops.

**Response:** `200 OK`
```json
["bob@example.com", "charlie@example.com", "dave@example.com"]
```

#### `GET /api/donations/categories/tree` — Food Category Tree (Spring-only)

Returns the full hierarchical category tree as nested JSON.

**Response:** `200 OK`
```json
{
  "name": "All Food",
  "children": [
    {
      "name": "Perishable",
      "children": [
        { "name": "produce", "children": [] },
        { "name": "dairy", "children": [] },
        { "name": "meat", "children": [] },
        { "name": "seafood", "children": [] },
        { "name": "prepared", "children": [] }
      ]
    },
    {
      "name": "Shelf-Stable",
      "children": [
        { "name": "canned", "children": [] },
        { "name": "dry-goods", "children": [] },
        { "name": "bakery", "children": [] },
        { "name": "snacks", "children": [] }
      ]
    },
    {
      "name": "Specialty",
      "children": [
        { "name": "beverages", "children": [] },
        { "name": "frozen", "children": [] },
        { "name": "baby-food", "children": [] }
      ]
    }
  ]
}
```

#### `GET /api/donations/categories/path?category=dairy` — Category Path to Root (Spring-only)

Returns the path from a leaf category up to the root.

**Response:** `200 OK`
```json
["dairy", "Perishable", "All Food"]
```

**Error (not found):** `404 Not Found`
```json
{ "error": "Category 'xyz' not found in tree" }
```

#### `GET /api/donations/sorted?sortBy=expiry&status=active` — Sorted Donation List (Spring-only)

Returns donations sorted by a specified criterion. Both query params are optional.

**Query params:**
- `sortBy` — `expiry` (soonest first), `created` (newest first), `quantity` (most first). Default: `created`
- `status` — filter by status (e.g., `active`, `accepted`). If omitted, returns all.

**Response:** `200 OK` — array of donation objects (same schema as match results)

---

### 4. Status Names — Flask vs Spring

| Lifecycle Stage | Flask Status | Spring Status |
|---|---|---|
| Newly posted | `posted` | `active` |
| Someone claimed it | `claimed` | `accepted` |
| Being transported | `in_transit` | `in-transit` |
| Delivered to receiver | `delivered` | `delivered` |
| Past expiry | `expired` | `expired` |
| Cancelled by donor | `cancelled` | `cancelled` |
| Receiver confirmed receipt | `confirmed` | *(not used)* |

When displaying status from Spring data, use the Spring names. When displaying status from Flask data, use the Flask names. The frontend should handle both.

---

### 5. Pages to Build

#### Page A: `navigation/donate/match.md` — Smart Donation Matching

A form where a **receiver** enters their zip code, selects dietary preferences (checkboxes), and selects allergens to exclude (checkboxes). On submit, call:

```js
const response = await fetch(`${javaURI}/api/donations/match`, {
  ...fetchOptions,
  method: 'POST',
  body: JSON.stringify({
    zip: zipInput,
    dietary_prefs: selectedDietary,        // array of strings
    allergen_exclusions: selectedAllergens  // array of strings
  }),
  headers: { 'Content-Type': 'application/json' }
});
const matched = await response.json();
```

Display results as cards showing: food name, category, quantity + unit, expiry date, days until expiry (color-coded: green > 5 days, yellow 2-5, red < 2), allergens, dietary tags, donor zip, and an "Accept" button.

**Available dietary options:** `vegetarian`, `vegan`, `halal`, `kosher`, `gluten-free`, `dairy-free`, `nut-free`, `organic`

**Available allergen options:** `peanuts`, `tree-nuts`, `dairy`, `eggs`, `soy`, `wheat`, `fish`, `shellfish`

When the user clicks "Accept" on a matched donation, call:

```js
await fetch(`${javaURI}/api/donations/${donationId}/accept`, {
  ...fetchOptions,
  method: 'POST',
  headers: { 'Content-Type': 'application/json' }
});
```

#### Page B: `navigation/donate/leaderboard.md` — Donor Leaderboard

Fetch the leaderboard on page load:

```js
const response = await fetch(`${javaURI}/api/donations/leaderboard?limit=10`, {
  ...fetchOptions,
  method: 'GET'
});
const leaders = await response.json();
```

Display as a ranked table or podium with:
- Rank (1st, 2nd, 3rd with 🥇🥈🥉 emojis)
- Donor email/name
- Number of donations
- A visual bar chart or progress bar showing relative donation counts

Include a stat summary at the top pulling from `${javaURI}/api/donations/stats` showing total donations, active count, and delivered count.

#### Page C: `navigation/donate/manage.md` — Donation Lifecycle Manager

A page for donors/volunteers to manage donation status. On load, fetch all donations from Spring:

```js
const response = await fetch(`${javaURI}/api/donations`, {
  ...fetchOptions,
  method: 'GET'
});
const donations = await response.json();
```

Display as a table with columns: ID, Food Name, Status (as colored badge), Expiry, Actions.

**Action buttons per status:**
- `active` → Show "Accept" and "Cancel" buttons
- `accepted` → Show "Mark Delivered" button
- `in-transit` → Show "Mark Delivered" button
- `delivered` / `expired` / `cancelled` → No actions (greyed out)

Button handlers:

```js
// Accept
await fetch(`${javaURI}/api/donations/${id}/accept`, { ...fetchOptions, method: 'POST', headers: { 'Content-Type': 'application/json' } });

// Deliver
await fetch(`${javaURI}/api/donations/${id}/deliver`, { ...fetchOptions, method: 'POST', headers: { 'Content-Type': 'application/json' } });

// Cancel
await fetch(`${javaURI}/api/donations/${id}/cancel`, { ...fetchOptions, method: 'POST', headers: { 'Content-Type': 'application/json' } });
```

After each action, refresh the table.

Also add an **"Undo"** button next to each donation that has been accepted, delivered, or cancelled. When clicked:

```js
await fetch(`${javaURI}/api/donations/${id}/undo`, { ...fetchOptions, method: 'POST', headers: { 'Content-Type': 'application/json' } });
```

This reverts the donation to its previous status. Refresh the table after.

#### Page D: `navigation/donate/network.md` — Donation Network & Graph Analytics

A page that visualizes the donor-receiver network. On load, fetch graph analytics:

```js
const response = await fetch(`${javaURI}/api/donations/graph`, {
  ...fetchOptions,
  method: 'GET'
});
const graphData = await response.json();
```

Display:
- **Network summary** — total nodes, total edges, number of communities
- **Communities** — list each community as a group of user emails (use colored tags/badges so each community has a distinct color)
- **Influence Ranking** — a ranked table of top donors by connections (like a mini-leaderboard)

Also add a **recommendation search**: an email input + a "Find Connections" button that calls:

```js
const recs = await apiFetch(`${javaURI}/api/donations/graph/recommendations?email=${encodeURIComponent(email)}&maxDepth=2`);
```

Display the returned list as a simple list of recommended users within 2 hops in the network.

#### Page E: `navigation/donate/categories.md` — Food Category Explorer

A page showing the hierarchical food category tree. On load:

```js
const tree = await apiFetch(`${javaURI}/api/donations/categories/tree`);
```

Render the tree as an **expandable/collapsible nested list** (use `<details>/<summary>` HTML elements or a simple accordion). Each group (Perishable, Shelf-Stable, Specialty) expands to show its leaf categories.

Add a **"Find Path" input**: the user types a category name and clicks "Trace Path". Call:

```js
const path = await apiFetch(`${javaURI}/api/donations/categories/path?category=${encodeURIComponent(categoryName)}`);
```

Display the path as a breadcrumb trail: `dairy → Perishable → All Food`.

#### Page F: `navigation/donate/browse.md` — Browse & Sort Donations

A page to browse all donations with sorting controls. Provide a dropdown to pick sort order and an optional status filter:

```js
const sortBy = sortDropdown.value;  // "expiry", "created", or "quantity"
const status = statusFilter.value;  // "active", "accepted", etc., or "" for all
let url = `${javaURI}/api/donations/sorted?sortBy=${sortBy}`;
if (status) url += `&status=${status}`;

const donations = await apiFetch(url);
```

Display as a card grid or table. Each card shows: food name, category (with emoji from the category constants), quantity + unit, expiry date (color-coded), status badge, and donor info. Re-fetch when the user changes the sort dropdown or status filter.

---

### 6. Fetch Pattern to Follow

Every fetch call must use `fetchOptions` from config and include proper error handling:

```js
async function apiFetch(url, options = {}) {
  try {
    const response = await fetch(url, {
      ...fetchOptions,
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}
```

---

### 7. Jekyll Front Matter

Each new `.md` page needs Jekyll front matter. Example for `match.md`:

```yaml
---
layout: post
title: Find Food Near You
description: Smart donation matching based on your dietary preferences
permalink: /donate/match
menu: nav/donate.html
---
```

All JavaScript goes inside `<script type="module">` tags so ES module imports work.

---

### 8. Styling

Match the existing site's look and feel. Use the same CSS classes already used in `navigation/donate/create.md` and `history.md`. Keep dark theme compatibility. Use the existing card and table styles from the site.

---

### 9. Important Notes

- **Do NOT modify any existing Flask donation pages** (`create.md`, `history.md`, `scan.md`, `barcode.md`). Those stay as-is and keep calling `pythonURI`.
- **Spring runs on port 8585 locally**, Flask on 8288. Both are separate servers.
- Spring returns `donation_id` field as `id` in JSON. Flask returns it as `id` too. They are compatible.
- Spring uses snake_case in JSON responses to match Flask's format.
- All Spring endpoints are under `/api/donations/` — same path prefix as Flask, just different host.
- If a Spring request fails (server down), show a friendly error message, don't crash the page.
- The Spring CORS config already allows `localhost:4100`, `localhost:4887`, `*.github.io`, and `127.0.0.1` origins.

---

### 10. Summary of New Files to Create

| File | Purpose |
|---|---|
| `navigation/donate/match.md` | Smart matching form + results |
| `navigation/donate/leaderboard.md` | Donor leaderboard + stats |
| `navigation/donate/manage.md` | Donation lifecycle management (accept/deliver/cancel/undo) |
| `navigation/donate/network.md` | Donor-receiver graph analytics & BFS recommendations |
| `navigation/donate/categories.md` | Hierarchical food category tree explorer |
| `navigation/donate/browse.md` | Browse & sort donations with filters |

Update only:
| File | Change |
|---|---|
| `assets/js/api/config.js` | Fix `javaURI` to point to `localhost:8585` |
| `navigation/donate/index.md` (if it exists) | Add links to the 6 new pages |
