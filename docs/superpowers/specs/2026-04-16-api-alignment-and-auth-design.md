# API Alignment And Auth Design

## Background

The current `main` branch already contains working implementations for user, purchase, sale, inventory, and statistics modules, plus Knife4j documentation. However, the implemented API contract does not fully match the previously confirmed `/api` design:

- `Auth` is missing `login` and `logout`
- role management endpoints are not exposed as independent resources
- menu tree query is missing
- some paths differ from the documented contract
- authentication is not yet a real JWT + Redis flow

This spec defines the next implementation slice: align the live backend contract to the previously approved `/api` design, and make authentication and permission endpoints actually usable.

## Goals

- Align implemented endpoints to the approved `/api` contract
- Add usable `login`, `me`, and `logout` authentication endpoints
- Implement role CRUD and role permission assignment as real database-backed APIs
- Implement menu tree query from `sys_permission`
- Update inventory and statistics endpoint paths to match the contract
- Preserve existing purchase, sale, inventory, and statistics business logic unless contract alignment requires controller-level changes
- Add tests first for the new and adjusted behavior

## Non-Goals

- No invoice module work
- No product master data module work
- No approval flow
- No frontend changes
- No broad refactor of existing purchase, sale, or inventory domain logic
- No dual-path compatibility layer for old routes

## Scope

### Auth

Implement:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

Behavior:

- login validates username/password against `sys_user`
- login rejects disabled or deleted users
- login signs JWT on success
- logout invalidates the current token through Redis-backed token revocation
- `me` returns current user information derived from the authenticated token

### User And Permission

Keep and align the existing user APIs:

- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `DELETE /api/v1/users/{id}`
- `GET /api/v1/users/{id}`
- `GET /api/v1/users`
- `POST /api/v1/users/{id}/roles`
- `POST /api/v1/users/{id}/permissions`
- `GET /api/v1/users/{id}/permissions`

Add independent role APIs:

- `POST /api/v1/roles`
- `PUT /api/v1/roles/{id}`
- `DELETE /api/v1/roles/{id}`
- `GET /api/v1/roles`
- `POST /api/v1/roles/{id}/permissions`

Add menu API:

- `GET /api/v1/menus/tree`

### Purchase

Keep existing APIs and business flow:

- `POST /api/v1/purchase-ins`
- `PUT /api/v1/purchase-ins/{id}`
- `GET /api/v1/purchase-ins`
- `GET /api/v1/purchase-ins/{id}`
- `DELETE /api/v1/purchase-ins/{id}`

No contract change is expected beyond keeping docs and tests aligned.

### Sale

Keep existing APIs and business flow:

- `POST /api/v1/sale-outs`
- `PUT /api/v1/sale-outs/{id}`
- `GET /api/v1/sale-outs`
- `GET /api/v1/sale-outs/{id}`
- `DELETE /api/v1/sale-outs/{id}`

No contract change is expected beyond keeping docs and tests aligned.

### Inventory

Keep:

- `GET /api/v1/inventories`
- `GET /api/v1/inventories/flows`
- `PUT /api/v1/inventories/{id}`
- `DELETE /api/v1/inventories/{id}`

Change:

- replace `GET /api/v1/inventories/batches` with `GET /api/v1/inventories/{productId}/batches`

### Statistics

Keep:

- `GET /api/v1/statistics/daily/inbound-outbound`
- `GET /api/v1/statistics/amount-by-customer`
- `GET /api/v1/statistics/amount-by-supplier`
- `GET /api/v1/statistics/amount-by-category`

Change:

- replace `GET /api/v1/statistics/monthly/amount-summary` with `GET /api/v1/statistics/monthly/inbound-outbound`

## Contract Alignment Rules

- The approved `/api` design is the source of truth
- Existing route names may be changed to match the approved contract
- Old routes will not be preserved as compatibility aliases
- Knife4j/OpenAPI docs must reflect only the aligned contract
- Tests must be updated to the aligned contract, not the previous implementation

## Authentication Design

### Token Model

- Use JWT for access tokens
- Use Redis to support token revocation after logout
- Token lifetime is configurable via application config

### Login Flow

1. Accept username and password
2. Query `sys_user` by username with `deleted = 0`
3. Reject when user is missing, disabled, or password is invalid
4. Load roles and final permission set
5. Issue JWT containing user identity and minimal auth claims
6. Optionally write a session summary to Redis keyed by token id or token hash
7. Return token, token type, expiry, and basic current-user payload

### Me Flow

1. Read bearer token
2. Validate JWT signature and expiration
3. Reject if token exists in Redis revocation set
4. Return current user profile plus role codes and permission codes

### Logout Flow

1. Read current bearer token
2. Parse remaining token lifetime
3. Store token revocation marker in Redis with matching TTL
4. Return success

## Authorization Design

Final permissions are computed as:

1. permissions from all assigned roles
2. apply direct user grants from `sys_user_permission` where `grant_type = 1`
3. apply direct user revocations from `sys_user_permission` where `grant_type = 0`

Super admin behavior:

- `sys_user.is_super_admin = 1` bypasses normal permission filtering
- super admin can access all APIs and all menus

## Menu Tree Design

Menu tree comes from `sys_permission`:

- include rows where `permission_type = 1`
- ignore deleted or disabled rows
- build tree by `parent_id`
- return only visible menus for normal users
- return full tree for super admin

The response should be a hierarchical tree suitable for direct frontend rendering.

## Role API Design

Role CRUD is backed by `sys_role`:

- create inserts a new active role
- update modifies name, code, status, and remark
- delete is logical delete by `deleted = 1`
- list supports basic filtering by role name, code, and status

Role permission assignment is backed by `sys_role_permission`:

- overwrite current non-deleted relations for the role
- insert the submitted permission ids as active relations

## Security Integration

Current `httpBasic()` security should be replaced by JWT-based request authentication.

Security changes:

- keep `/api/v1/auth/login` public
- keep `/doc.html`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/webjars/**` public
- all other `/api/**` endpoints require a valid bearer token
- add a JWT authentication filter before Spring Security authorization

This slice does not require per-endpoint Spring method permission enforcement. The immediate goal is a usable authenticated API contract. Permission computation is still required for `me`, menu tree, and future expansion.

## Data And Schema Assumptions

The current SQL script already contains:

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- `sys_user_permission`

No new business tables are required for this slice.

Redis is assumed to be available for logout revocation and optional session summary storage.

## Testing Strategy

Use TDD for all new and changed behavior.

### Auth Tests

- login succeeds with valid username/password
- login fails for unknown user
- login fails for invalid password
- login fails for disabled user
- `me` succeeds with valid token
- logout revokes token
- revoked token is rejected by `me`

### Role And Menu Tests

- create role succeeds
- update role succeeds
- delete role marks row deleted
- role list returns expected rows
- role permission assignment overwrites prior relations correctly
- menu tree returns nested data from `sys_permission`
- super admin sees full menu tree

### Contract Alignment Tests

- inventory batch endpoint is available only at `/{productId}/batches`
- statistics monthly endpoint is available only at `/monthly/inbound-outbound`
- existing purchase and sale tests remain green after alignment

## Implementation Notes

Expected code additions or changes:

- auth DTOs and VOs
- auth service
- JWT utility/service
- Redis-backed token revocation service
- JWT filter and security config update
- role DTOs and VOs
- role controller and service
- menu controller and tree response VO
- controller path updates for inventory and statistics
- updated Knife4j/OpenAPI docs
- new controller and service tests

## Risks

### Route Breakage

Changing routes without compatibility aliases breaks any existing callers that use the old paths. This is acceptable because the project is still in active development and the approved contract should become the only source of truth.

### Security Complexity

JWT and Redis revocation add moving parts. Tests must cover invalid, expired, and revoked token cases to avoid a false sense of security.

### Permission Drift

The project will compute permissions for user-facing responses now, but not yet enforce fine-grained endpoint permissions everywhere. This is intentional for this slice and should be documented as a follow-up item rather than silently implied as complete RBAC enforcement.

## Success Criteria

- all approved auth endpoints are implemented and usable
- role CRUD and role permission APIs are database-backed and tested
- menu tree endpoint is implemented from `sys_permission`
- inventory and statistics routes match the approved `/api` contract
- Knife4j docs reflect the aligned contract
- all new behavior is test-covered
- full `mvn test` passes
