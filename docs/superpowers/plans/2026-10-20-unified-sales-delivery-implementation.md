# Unified Sales and Delivery Implementation Plan

**Goal:** Replace immediate sales outbound with a unified order flow, inventory reservation, delivery, PDF/Excel export, and completion-date statistics.

**Architecture:** Keep `biz_sale_out` as the order table. A transactional order service owns state transitions and per-item reservations; `InventoryLedger` owns stock arithmetic and FIFO completion. Sales UI is split from the shared purchase form. Legacy sales data is excluded and the upgrade stops if it finds any.

**Tech stack:** Spring Boot 3.2, MyBatis Plus, MySQL, Vue 2, Element UI, Jest, JUnit 5, H2.

---

### Task 1: Schema and state contract

- [x] Add a guarded MySQL upgrade for order fields, snapshots, reservations and event history; fail when legacy sale rows exist.
- [x] Add state definitions and request/response models; remove client-controlled numeric status.
- [x] Write failing state/validation tests, then implement the contract.

### Task 2: Reservation and fulfillment

- [x] Write failing integration tests for draft, confirm, insufficient stock, edit, cancel, delivery, return and idempotent completion.
- [x] Extend the ledger with atomic reserve, release and consumption operations under warehouse locks.
- [x] Replace immediate outbound logic with guarded state transitions; preserve FIFO deductions and flows only on completion.

### Task 3: Customers and query

- [x] Write failing tests for guest orders, customer snapshots, phone search and quick customer creation.
- [x] Add search and quick-create APIs with server-generated customer codes and permission checks.
- [x] Add order filters, snapshot details and event history responses.

### Task 4: Export and print

- [x] Write failing export tests for draft rejection, snapshot fields, PDF Chinese text and Excel amounts.
- [x] Add confirmed-order sales list PDF/Excel exports and delivery-only print view/PDF, with permission and request logs.

### Task 5: Statistics, permissions and frontend

- [x] Write failing report tests proving pending orders are excluded and completion dates are used.
- [x] Update SQL facts and add pending metrics and guest grouping.
- [x] Add action permission seeds and backend authorization paths.
- [x] Write failing UI tests for guest, customer, state actions and export visibility.
- [x] Replace the sales route with dedicated entry, list, detail and print pages; preserve purchase behavior.

### Task 6: Verification

- [x] Run backend tests and frontend unit tests/build; fix regressions.
- [ ] Verify migration on an empty MySQL schema and its old-row guard.
- [ ] Walk the approved acceptance scenarios on the target printer and database; printer calibration is documented in docs/releases/2026-10-20-unified-sales-delivery.md.
