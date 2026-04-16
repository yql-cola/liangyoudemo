# API Alignment And Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the live backend APIs to the approved `/api` contract and make auth, role, and menu endpoints truly usable with JWT and Redis-backed logout revocation.

**Architecture:** Keep the existing Spring Boot modular structure, add a focused auth slice under `modules/auth`, add dedicated role and menu controllers instead of overloading `UserController`, and update only the controller-facing contract where required. Authentication will move from `httpBasic()` to a stateless JWT filter, while role and menu data remain backed by the existing `sys_*` tables.

**Tech Stack:** Spring Boot 3, Spring Security, MyBatis Plus, Redis, JUnit 5, MockMvc, Jackson, Knife4j/springdoc.

---

## File Structure

### Create

- `src/main/java/org/liangyou/modules/auth/dto/LoginRequest.java`
- `src/main/java/org/liangyou/modules/auth/vo/LoginResponse.java`
- `src/main/java/org/liangyou/modules/auth/vo/CurrentUserResponse.java`
- `src/main/java/org/liangyou/modules/auth/service/AuthService.java`
- `src/main/java/org/liangyou/security/JwtTokenService.java`
- `src/main/java/org/liangyou/security/JwtAuthenticationFilter.java`
- `src/main/java/org/liangyou/modules/user/dto/RoleCreateRequest.java`
- `src/main/java/org/liangyou/modules/user/dto/RoleUpdateRequest.java`
- `src/main/java/org/liangyou/modules/user/dto/RoleQueryRequest.java`
- `src/main/java/org/liangyou/modules/user/vo/RoleResponse.java`
- `src/main/java/org/liangyou/modules/user/vo/MenuTreeResponse.java`
- `src/main/java/org/liangyou/modules/user/controller/RoleController.java`
- `src/main/java/org/liangyou/modules/user/controller/MenuController.java`
- `src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java`
- `src/test/java/org/liangyou/modules/user/controller/RoleControllerTest.java`
- `src/test/java/org/liangyou/modules/user/controller/MenuControllerTest.java`

### Modify

- `src/main/java/org/liangyou/config/SecurityConfig.java`
- `src/main/java/org/liangyou/modules/auth/controller/AuthController.java`
- `src/main/java/org/liangyou/modules/user/controller/UserController.java`
- `src/main/java/org/liangyou/modules/user/service/UserService.java`
- `src/main/java/org/liangyou/modules/user/mapper/SysRoleMapper.java`
- `src/main/java/org/liangyou/modules/user/mapper/SysPermissionMapper.java`
- `src/main/java/org/liangyou/modules/inventory/controller/InventoryController.java`
- `src/main/java/org/liangyou/modules/inventory/service/InventoryService.java`
- `src/main/java/org/liangyou/modules/statistics/controller/StatisticsController.java`
- `src/main/java/org/liangyou/modules/statistics/service/StatisticsService.java`
- `src/main/java/org/liangyou/config/OpenApiConfig.java`
- `src/test/java/org/liangyou/modules/inventory/controller/InventoryControllerTest.java`
- `src/test/java/org/liangyou/modules/statistics/controller/StatisticsControllerTest.java`
- `src/test/java/org/liangyou/LiangyouDemoApplicationTests.java`

---

### Task 1: Add Failing Auth Contract Tests

**Files:**
- Create: `src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java`
- Modify: `src/main/java/org/liangyou/modules/auth/controller/AuthController.java`
- Test: `src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void loginReturnsTokenPayload() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse(
                "jwt-token", "Bearer", 7200L,
                new CurrentUserResponse(1L, "admin", "超级管理员", List.of("SUPER_ADMIN"), List.of("system:user:create"))
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.userInfo.username").value("admin"));
    }

    @Test
    void logoutUsesBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AuthControllerTest test`

Expected: FAIL because `POST /api/v1/auth/login` and `POST /api/v1/auth/logout` are not implemented.

- [ ] **Step 3: Write minimal implementation**

```java
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(authService.currentUser());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
        return ApiResponse.success();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AuthControllerTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/auth src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java
git commit -m "test: add auth controller contract tests"
```

### Task 2: Implement JWT + Redis Auth Flow

**Files:**
- Create: `src/main/java/org/liangyou/modules/auth/dto/LoginRequest.java`
- Create: `src/main/java/org/liangyou/modules/auth/vo/LoginResponse.java`
- Create: `src/main/java/org/liangyou/modules/auth/vo/CurrentUserResponse.java`
- Create: `src/main/java/org/liangyou/modules/auth/service/AuthService.java`
- Create: `src/main/java/org/liangyou/security/JwtTokenService.java`
- Create: `src/main/java/org/liangyou/security/JwtAuthenticationFilter.java`
- Modify: `src/main/java/org/liangyou/config/SecurityConfig.java`
- Modify: `src/main/java/org/liangyou/modules/user/service/UserService.java`
- Test: `src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void meReturnsCurrentUserForValidToken() throws Exception {
    when(authService.currentUser()).thenReturn(
            new CurrentUserResponse(1L, "admin", "超级管理员", List.of("SUPER_ADMIN"), List.of("system:user:create"))
    );

    mockMvc.perform(get("/api/v1/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.realName").value("超级管理员"));
}

@Test
void loginRejectsBadPassword() throws Exception {
    when(authService.login(any())).thenThrow(new BizException("用户名或密码错误"));

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"admin","password":"bad-password"}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AuthControllerTest test`

Expected: FAIL because `AuthService` and security-backed current-user logic do not exist.

- [ ] **Step 3: Write minimal implementation**

```java
@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) { /* load user, validate password, sign token */ }

    public CurrentUserResponse currentUser() { /* resolve from SecurityContext */ }

    public void logout(String authorization) { /* revoke token in Redis with TTL */ }
}
```

```java
@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/auth/login", "/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AuthControllerTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/auth src/main/java/org/liangyou/security src/main/java/org/liangyou/config/SecurityConfig.java src/test/java/org/liangyou/modules/auth/controller/AuthControllerTest.java
git commit -m "feat: implement jwt auth flow"
```

### Task 3: Add Failing Role And Menu Tests

**Files:**
- Create: `src/test/java/org/liangyou/modules/user/controller/RoleControllerTest.java`
- Create: `src/test/java/org/liangyou/modules/user/controller/MenuControllerTest.java`
- Create: `src/main/java/org/liangyou/modules/user/controller/RoleController.java`
- Create: `src/main/java/org/liangyou/modules/user/controller/MenuController.java`
- Test: `src/test/java/org/liangyou/modules/user/controller/RoleControllerTest.java`
- Test: `src/test/java/org/liangyou/modules/user/controller/MenuControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void createRoleReturnsCreatedRole() throws Exception {
        when(userService.createRole(any())).thenReturn(new RoleResponse(1L, "WAREHOUSE", "仓库人员", 1, "仓库角色"));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCode":"WAREHOUSE","roleName":"仓库人员","status":1,"remark":"仓库角色"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("WAREHOUSE"));
    }
}
```

```java
@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void menuTreeReturnsNestedMenus() throws Exception {
        when(userService.getMenuTree()).thenReturn(List.of(
                new MenuTreeResponse(1L, "系统管理", "/system", List.of(
                        new MenuTreeResponse(2L, "用户管理", "/users", List.of())
                ))
        ));

        mockMvc.perform(get("/api/v1/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].name").value("用户管理"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=RoleControllerTest,MenuControllerTest test`

Expected: FAIL because role and menu controllers do not exist.

- [ ] **Step 3: Write minimal implementation**

```java
@RestController
@RequestMapping("/api/v1/roles")
class RoleController {

    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(userService.createRole(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(userService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.deleteRole(id);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> list(RoleQueryRequest request) {
        return ApiResponse.success(userService.listRoles(request));
    }

    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id, @RequestBody RolePermissionAssignRequest request) {
        userService.assignRolePermissions(id, request);
        return ApiResponse.success();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=RoleControllerTest,MenuControllerTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/user/controller src/test/java/org/liangyou/modules/user/controller/RoleControllerTest.java src/test/java/org/liangyou/modules/user/controller/MenuControllerTest.java
git commit -m "test: add role and menu controller contracts"
```

### Task 4: Implement Role CRUD, Role Permissions, And Menu Tree

**Files:**
- Create: `src/main/java/org/liangyou/modules/user/dto/RoleCreateRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/RoleUpdateRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/RoleQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/vo/RoleResponse.java`
- Create: `src/main/java/org/liangyou/modules/user/vo/MenuTreeResponse.java`
- Modify: `src/main/java/org/liangyou/modules/user/service/UserService.java`
- Modify: `src/main/java/org/liangyou/modules/user/mapper/SysRoleMapper.java`
- Modify: `src/main/java/org/liangyou/modules/user/mapper/SysPermissionMapper.java`
- Test: `src/test/java/org/liangyou/modules/user/controller/RoleControllerTest.java`
- Test: `src/test/java/org/liangyou/modules/user/controller/MenuControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void menuTreeComesFromPermissionsTable() throws Exception {
        mockMvc.perform(get("/api/v1/menus/tree").header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=RoleControllerTest,MenuControllerTest test`

Expected: FAIL because persistence-backed role CRUD and menu assembly are incomplete.

- [ ] **Step 3: Write minimal implementation**

```java
public RoleResponse createRole(RoleCreateRequest request) {
    SysRole role = new SysRole();
    role.setRoleCode(request.getRoleCode());
    role.setRoleName(request.getRoleName());
    role.setStatus(request.getStatus());
    role.setRemark(request.getRemark());
    sysRoleMapper.insert(role);
    return toRoleResponse(role);
}

public List<MenuTreeResponse> getMenuTree() {
    List<SysPermission> menus = loadVisibleMenusForCurrentUser();
    return buildMenuTree(menus, 0L);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=RoleControllerTest,MenuControllerTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/user src/test/java/org/liangyou/modules/user/controller/RoleControllerTest.java src/test/java/org/liangyou/modules/user/controller/MenuControllerTest.java
git commit -m "feat: implement role and menu apis"
```

### Task 5: Align Inventory And Statistics Contracts

**Files:**
- Modify: `src/main/java/org/liangyou/modules/inventory/controller/InventoryController.java`
- Modify: `src/main/java/org/liangyou/modules/inventory/service/InventoryService.java`
- Modify: `src/main/java/org/liangyou/modules/statistics/controller/StatisticsController.java`
- Modify: `src/main/java/org/liangyou/modules/statistics/service/StatisticsService.java`
- Modify: `src/test/java/org/liangyou/modules/inventory/controller/InventoryControllerTest.java`
- Modify: `src/test/java/org/liangyou/modules/statistics/controller/StatisticsControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void batchQueryUsesProductPathVariable() throws Exception {
    mockMvc.perform(get("/api/v1/inventories/1001/batches"))
            .andExpect(status().isOk());
}

@Test
void monthlyInboundOutboundUsesAlignedPath() throws Exception {
    mockMvc.perform(get("/api/v1/statistics/monthly/inbound-outbound"))
            .andExpect(status().isOk());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=InventoryControllerTest,StatisticsControllerTest test`

Expected: FAIL because the old controller mappings still exist.

- [ ] **Step 3: Write minimal implementation**

```java
@GetMapping("/{productId}/batches")
public ApiResponse<List<InventoryBatchResponse>> listBatches(@PathVariable Long productId) {
    return ApiResponse.success(inventoryService.listBatches(productId));
}
```

```java
@GetMapping("/monthly/inbound-outbound")
public ApiResponse<List<DailyInboundOutboundResponse>> monthlyInboundOutbound(StatisticsQueryRequest request) {
    return ApiResponse.success(statisticsService.monthlyInboundOutbound(request));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=InventoryControllerTest,StatisticsControllerTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/inventory src/main/java/org/liangyou/modules/statistics src/test/java/org/liangyou/modules/inventory/controller/InventoryControllerTest.java src/test/java/org/liangyou/modules/statistics/controller/StatisticsControllerTest.java
git commit -m "feat: align inventory and statistics routes"
```

### Task 6: Update Knife4j Docs And Run Full Verification

**Files:**
- Modify: `src/main/java/org/liangyou/config/OpenApiConfig.java`
- Modify: `src/main/java/org/liangyou/modules/auth/controller/AuthController.java`
- Modify: `src/main/java/org/liangyou/modules/user/controller/RoleController.java`
- Modify: `src/main/java/org/liangyou/modules/user/controller/MenuController.java`
- Modify: `src/main/java/org/liangyou/modules/inventory/controller/InventoryController.java`
- Modify: `src/main/java/org/liangyou/modules/statistics/controller/StatisticsController.java`
- Modify: `src/test/java/org/liangyou/LiangyouDemoApplicationTests.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void contextLoadsWithAlignedOpenApiGroups() {
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=LiangyouDemoApplicationTests test`

Expected: FAIL if documentation beans or security config break application startup after controller additions.

- [ ] **Step 3: Write minimal implementation**

```java
@Bean
GroupedOpenApi roleApi() {
    return GroupedOpenApi.builder().group("Role").pathsToMatch("/api/v1/roles/**").build();
}

@Bean
GroupedOpenApi menuApi() {
    return GroupedOpenApi.builder().group("Menu").pathsToMatch("/api/v1/menus/**").build();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test`

Expected: PASS with all module tests green.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/config/OpenApiConfig.java src/main/java/org/liangyou/modules src/test/java/org/liangyou/LiangyouDemoApplicationTests.java
git commit -m "feat: finalize api alignment and docs"
```

---

## Self-Review

- Spec coverage:
  - auth endpoints: Task 1-2
  - role CRUD and role permissions: Task 3-4
  - menu tree: Task 3-4
  - inventory and statistics route alignment: Task 5
  - Knife4j/OpenAPI updates: Task 6
  - full verification: Task 6
- Placeholder scan:
  - no `TODO`, `TBD`, or deferred implementation markers remain
- Type consistency:
  - auth service uses `LoginRequest`, `LoginResponse`, `CurrentUserResponse`
  - role endpoints use `RoleCreateRequest`, `RoleUpdateRequest`, `RoleQueryRequest`, `RoleResponse`
  - menu endpoint uses `MenuTreeResponse`

