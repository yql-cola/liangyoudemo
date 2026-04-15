# Backend Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing `liangyoudemo` Maven project into a Spring Boot backend skeleton with Nacos Config, Swagger/OpenAPI, shared API response objects, and modular package structure.

**Architecture:** Keep a single Maven module and convert it into a Spring Boot 3 application. Add configuration, documentation, and security-ready scaffolding while keeping business modules empty but correctly structured for later implementation.

**Tech Stack:** Java 17, Maven, Spring Boot 3.2+, Spring Web, Validation, Spring Security, Spring Data Redis, MyBatis Plus, springdoc-openapi, Spring Cloud Alibaba Nacos Config, JUnit 5

---

### Task 1: Convert the existing Maven project into Spring Boot

**Files:**
- Modify: `pom.xml`
- Delete: `src/main/java/org/liangyou/Main.java`
- Create: `src/main/java/org/liangyou/LiangyouDemoApplication.java`
- Test: `src/test/java/org/liangyou/LiangyouDemoApplicationTests.java`

- [ ] **Step 1: Write the failing test**

```java
package org.liangyou;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LiangyouDemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=LiangyouDemoApplicationTests test`
Expected: FAIL because Spring Boot test dependencies and application bootstrap class are missing

- [ ] **Step 3: Write minimal implementation**

```java
package org.liangyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LiangyouDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiangyouDemoApplication.class, args);
    }
}
```

Update `pom.xml` to use `spring-boot-starter-parent`, Java 17, and add the required Spring Boot starters and plugins.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=LiangyouDemoApplicationTests test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/org/liangyou/LiangyouDemoApplication.java src/test/java/org/liangyou/LiangyouDemoApplicationTests.java
git commit -m "build: convert project to spring boot skeleton"
```

### Task 2: Add configuration files for local, dev, test, and prod environments

**Files:**
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`
- Create: `src/main/resources/application-test.yml`
- Create: `src/main/resources/application-prod.yml`

- [ ] **Step 1: Write the failing test**

Add this assertion to `src/test/java/org/liangyou/LiangyouDemoApplicationTests.java`:

```java
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Autowired
private Environment environment;

@Test
void defaultProfileIsDev() {
    Assertions.assertTrue(java.util.Arrays.asList(environment.getActiveProfiles()).contains("dev"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=LiangyouDemoApplicationTests test`
Expected: FAIL because no Spring profile configuration exists yet

- [ ] **Step 3: Write minimal implementation**

`application.yml`

```yaml
spring:
  application:
    name: liangyoudemo
  profiles:
    active: dev
  config:
    import:
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml
```

Create environment files with placeholders for server port, datasource, redis, swagger, and nacos server address.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=LiangyouDemoApplicationTests test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application*.yml src/test/java/org/liangyou/LiangyouDemoApplicationTests.java
git commit -m "config: add environment and nacos config files"
```

### Task 3: Add shared web response objects and API documentation configuration

**Files:**
- Create: `src/main/java/org/liangyou/common/api/ApiResponse.java`
- Create: `src/main/java/org/liangyou/config/OpenApiConfig.java`
- Create: `src/main/java/org/liangyou/modules/auth/controller/AuthController.java`
- Test: `src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.liangyou.modules.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void meEndpointReturnsStandardApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=AuthControllerTest test`
Expected: FAIL because controller and response wrapper do not exist

- [ ] **Step 3: Write minimal implementation**

Create `ApiResponse<T>` with `code`, `message`, and `data`.

Create `AuthController` with:

```java
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.success(Map.of("username", "mock-user"));
    }
}
```

Create `OpenApiConfig` with `BearerAuth` scheme and API metadata.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=AuthControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/common/api/ApiResponse.java src/main/java/org/liangyou/config/OpenApiConfig.java src/main/java/org/liangyou/modules/auth/controller/AuthController.java src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java
git commit -m "feat: add api response and swagger config"
```

### Task 4: Create modular package skeleton and placeholder controllers

**Files:**
- Create: `src/main/java/org/liangyou/modules/user/controller/UserController.java`
- Create: `src/main/java/org/liangyou/modules/purchase/controller/PurchaseInController.java`
- Create: `src/main/java/org/liangyou/modules/sale/controller/SaleOutController.java`
- Create: `src/main/java/org/liangyou/modules/inventory/controller/InventoryController.java`
- Create: `src/main/java/org/liangyou/modules/statistics/controller/StatisticsController.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/.gitkeep`
- Create: `src/main/java/org/liangyou/modules/user/service/.gitkeep`
- Create: `src/main/java/org/liangyou/modules/user/entity/.gitkeep`

- [ ] **Step 1: Write the failing test**

Create a smoke test:

```java
package org.liangyou;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModuleStructureTest {

    @Test
    void moduleControllersExist() throws ClassNotFoundException {
        Class.forName("org.liangyou.modules.user.controller.UserController");
        Class.forName("org.liangyou.modules.purchase.controller.PurchaseInController");
        Class.forName("org.liangyou.modules.sale.controller.SaleOutController");
        Class.forName("org.liangyou.modules.inventory.controller.InventoryController");
        Class.forName("org.liangyou.modules.statistics.controller.StatisticsController");
        Assertions.assertTrue(true);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ModuleStructureTest test`
Expected: FAIL because the controller classes do not exist

- [ ] **Step 3: Write minimal implementation**

Create controller classes with `@Tag` and `@RequestMapping` only:

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User & Permission")
public class UserController {
}
```

Repeat for purchase, sale, inventory, and statistics modules. Create empty `dto`, `entity`, and `service` directories for each module using placeholder files if needed.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ModuleStructureTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules src/test/java/org/liangyou/ModuleStructureTest.java
git commit -m "feat: add module package skeleton"
```

### Task 5: Verify the skeleton can boot and expose Swagger

**Files:**
- Modify: `src/test/java/org/liangyou/LiangyouDemoApplicationTests.java`

- [ ] **Step 1: Write the failing test**

Add:

```java
import org.springframework.beans.factory.annotation.Value;

@Value("${spring.application.name}")
private String applicationName;

@Test
void applicationNameIsConfigured() {
    org.junit.jupiter.api.Assertions.assertEquals("liangyoudemo", applicationName);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=LiangyouDemoApplicationTests test`
Expected: FAIL until the application configuration is loaded correctly

- [ ] **Step 3: Write minimal implementation**

Adjust configuration so Spring Boot starts with the application name and does not require a live Nacos server during tests.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test`
Expected: PASS with all tests green

- [ ] **Step 5: Commit**

```bash
git add src/test/java/org/liangyou/LiangyouDemoApplicationTests.java src/main/resources/application*.yml pom.xml
git commit -m "test: verify backend skeleton boots successfully"
```
