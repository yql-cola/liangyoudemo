# Dev Admin Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure `/dev` startup and fresh SQL initialization both provide usable administrator accounts for Swagger authentication.

**Architecture:** Add a dev-only startup seeder that idempotently creates the default admin role, users, and role bindings when missing. Mirror the same baseline data in `sql/grain_oil_ims_init.sql` so a newly imported database has the same accounts before the application starts.

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, Mockito, MySQL SQL seed script

---

### Task 1: Add Failing Tests For Dev Admin Seeding

**Files:**
- Create: `src/test/java/org/liangyou/modules/user/service/DevAdminAccountInitializerTest.java`
- Test: `src/test/java/org/liangyou/modules/user/service/DevAdminAccountInitializerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void runCreatesMissingRoleUsersAndBindings() {
    // verify missing admin users and role are inserted
}

@Test
void runSkipsExistingUsersRoleAndBindings() {
    // verify existing records are reused without duplicate inserts
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=DevAdminAccountInitializerTest test`
Expected: FAIL with missing `DevAdminAccountInitializer`

- [ ] **Step 3: Write minimal implementation**

```java
@Component
@Profile("dev")
class DevAdminAccountInitializer implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // create role, users, and relations when absent
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=DevAdminAccountInitializerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/org/liangyou/modules/user/service/DevAdminAccountInitializerTest.java src/main/java/org/liangyou/modules/user/service/DevAdminAccountInitializer.java
git commit -m "feat: seed dev admin accounts"
```

### Task 2: Mirror Seed Data In SQL Initialization Script

**Files:**
- Modify: `sql/grain_oil_ims_init.sql`
- Test: `src/test/java/org/liangyou/modules/user/service/DevAdminAccountInitializerTest.java`

- [ ] **Step 1: Write the failing assertion**

```java
assertTrue(sql.contains("INSERT INTO sys_user"));
assertTrue(sql.contains("admin"));
assertTrue(sql.contains("yaoqianglong"));
```

- [ ] **Step 2: Run targeted verification to confirm gap**

Run: `grep -n "yaoqianglong\\|admin" sql/grain_oil_ims_init.sql`
Expected: no matching seed rows

- [ ] **Step 3: Write minimal SQL seed**

```sql
INSERT INTO sys_role (...);
INSERT INTO sys_user (...);
INSERT INTO sys_user_role (...);
```

- [ ] **Step 4: Run verification**

Run: `grep -n "yaoqianglong\\|admin" sql/grain_oil_ims_init.sql`
Expected: matching role and user seed rows present

- [ ] **Step 5: Commit**

```bash
git add sql/grain_oil_ims_init.sql
git commit -m "chore: add default admin seed data"
```
