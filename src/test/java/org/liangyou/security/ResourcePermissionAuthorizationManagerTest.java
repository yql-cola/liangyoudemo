package org.liangyou.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePermissionAuthorizationManagerTest {
    private final ResourcePermissionAuthorizationManager manager = new ResourcePermissionAuthorizationManager();

    @Test void saleActionsAndExportsRequireDedicatedPermissions() {
        assertTrue(granted(user("sale:order:confirm"), "POST", "/api/v1/sale-outs/2/confirm"));
        assertFalse(granted(user("sale:order:create"), "POST", "/api/v1/sale-outs/2/confirm"));
        assertTrue(granted(user("sale:delivery:complete"), "POST", "/api/v1/sale-outs/2/complete-delivery"));
        assertFalse(granted(user("sale:delivery:start"), "POST", "/api/v1/sale-outs/2/complete-delivery"));
        assertTrue(granted(user("sale:order:view", "sale:order:export"), "GET", "/api/v1/sale-outs/2/export/excel"));
        assertFalse(granted(user("sale:order:export"), "GET", "/api/v1/sale-outs/2/export/excel"));
        assertFalse(granted(user("sale:order:view"), "GET", "/api/v1/sale-outs/2/export/pdf"));
        assertTrue(granted(user("sale:order:view", "sale:order:print"), "GET", "/api/v1/sale-outs/2/delivery-pdf"));
        assertFalse(granted(user("sale:order:print"), "GET", "/api/v1/sale-outs/2/delivery-pdf"));
        assertFalse(granted(user("sale:order:view"), "POST", "/api/v1/sale-outs/2/print"));
    }

    @Test void listPermissionDoesNotGrantExportOrMutation() {
        Authentication user = user("basic:product:list");
        assertTrue(granted(user, "GET", "/api/v1/basic/products"));
        assertFalse(granted(user, "GET", "/api/v1/basic/products/export"));
        assertFalse(granted(user, "DELETE", "/api/v1/basic/products/2"));
        assertFalse(granted(user, "PATCH", "/api/v1/basic/products/2/status"));
    }

    @Test void exportRequiresSpecificExportPermission() {
        assertTrue(granted(user("basic:product:export"), "GET", "/api/v1/basic/products/export"));
        assertFalse(granted(user("basic:unit:export"), "GET", "/api/v1/basic/products/export"));
        assertTrue(granted(user("statistics:sale-product:export"), "GET", "/api/v1/statistics/sale-products/export"));
        assertFalse(granted(user("statistics:sale-product:view"), "GET", "/api/v1/statistics/sale-products/export"));
    }

    @Test void importUsesImportOrCreatePermissionAndSelectedExportNeedsExportPermission() {
        assertTrue(granted(user("basic:product:import"), "GET", "/api/v1/basic/products/import/template"));
        assertTrue(granted(user("basic:product:import"), "POST", "/api/v1/basic/products/import"));
        assertTrue(granted(user("basic:product:create"), "POST", "/api/v1/basic/products/import"));
        assertTrue(granted(user("basic:product:create"), "GET", "/api/v1/basic/products/import/template"));
        assertFalse(granted(user("basic:product:list"), "POST", "/api/v1/basic/products/import"));
        assertTrue(granted(user("basic:product:export"), "POST", "/api/v1/basic/products/export"));
        assertFalse(granted(user("basic:product:create"), "POST", "/api/v1/basic/products/export"));
    }

    @Test void businessUserCanSelectMasterDataWithoutMaintainingIt() {
        Authentication user = user("purchase:order:create");
        assertTrue(granted(user, "GET", "/api/v1/basic/products/options"));
        assertTrue(granted(user, "GET", "/api/v1/basic/products/2/units"));
        assertTrue(granted(user, "GET", "/api/v1/basic/suppliers/options"));
        assertFalse(granted(user, "POST", "/api/v1/basic/products"));
        assertFalse(granted(user(), "GET", "/api/v1/basic/products/options"));
    }

    @Test void categoryTreeRequiresListPermission() {
        assertTrue(granted(user("basic:category:list"), "GET", "/api/v1/basic/categories/tree"));
        assertFalse(granted(user("basic:unit:list"), "GET", "/api/v1/basic/categories/tree"));
    }

    @Test void superAdminAndWildcardHaveAccess() {
        assertTrue(granted(user("ROLE_SUPER_ADMIN"), "DELETE", "/api/v1/basic/products/2"));
        assertTrue(granted(user("*:*:*"), "GET", "/api/v1/statistics/dashboard/export"));
    }

    @Test void unknownResourceAndAnonymousAreDenied() {
        assertFalse(granted(user("basic:product:list"), "GET", "/api/v1/basic/unknown"));
        assertFalse(granted(null, "GET", "/api/v1/basic/products"));
    }

    @Test void inventoryListAndExportAreIndependentForEachResource() {
        for (String resource : new String[]{"stock", "batch", "flow"}) {
            String path = "/api/v1/inventories" + switch (resource) {
                case "batch" -> "/batches";
                case "flow" -> "/flows";
                default -> "";
            };
            assertTrue(granted(user("inventory:" + resource + ":list"), "GET", path));
            assertFalse(granted(user("inventory:" + resource + ":list"), "GET", path + "/export"));
            assertTrue(granted(user("inventory:" + resource + ":export"), "GET", path + "/export"));
            assertFalse(granted(user("inventory:" + resource + ":export"), "GET", path));
            assertFalse(granted(user("inventory:" + resource + ":list"), "PUT", "/api/v1/inventories/2"));
            assertFalse(granted(user("inventory:" + resource + ":list"), "DELETE", "/api/v1/inventories/2"));
        }
        assertTrue(granted(user("inventory:batch:list"), "GET", "/api/v1/inventories/2/batches"));
        assertTrue(granted(user("inventory:stock:update"), "PUT", "/api/v1/inventories/2"));
        assertTrue(granted(user("inventory:stock:delete"), "DELETE", "/api/v1/inventories/2"));
        assertFalse(granted(user("inventory:stock:export"), "GET", "/api/v1/inventories/flows/export"));
    }

    @Test void orderReadOnlyUsersCannotMutateAndEachActionRequiresItsOwnCode() {
        for (String domain : new String[]{"purchase", "sale"}) {
            String path = "/api/v1/" + (domain.equals("purchase") ? "purchase-ins" : "sale-outs");
            assertTrue(granted(user(domain + ":order:list"), "GET", path));
            assertTrue(granted(user(domain + ":order:view"), "GET", path + "/2"));
            assertTrue(granted(user(domain + ":order:create"), "POST", path));
            assertTrue(granted(user(domain + ":order:update"), "PUT", path + "/2"));
            assertTrue(granted(user(domain + ":order:delete"), "DELETE", path + "/2"));
            Authentication reader = user(domain + ":order:list", domain + ":order:view");
            assertFalse(granted(reader, "POST", path));
            assertFalse(granted(reader, "PUT", path + "/2"));
            assertFalse(granted(reader, "DELETE", path + "/2"));
            assertFalse(granted(user(domain + ":order:list"), "GET", path + "/2"));
        }
    }

    @Test void salesWritersCanOnlyReadInventoryForOneWarehouseAndProduct() {
        for (String action : new String[]{"create", "update"}) {
            Authentication writer = user("sale:order:" + action);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/inventories");
            request.setParameter("warehouseId", "1");
            request.setParameter("productId", "2");
            assertTrue(manager.check(() -> writer, new RequestAuthorizationContext(request)).isGranted());
            request.removeParameter("productId");
            assertFalse(manager.check(() -> writer, new RequestAuthorizationContext(request)).isGranted());
            request.setParameter("productId", "0");
            assertFalse(manager.check(() -> writer, new RequestAuthorizationContext(request)).isGranted());
            assertFalse(granted(writer, "GET", "/api/v1/inventories"));
            assertFalse(granted(writer, "GET", "/api/v1/inventories/export"));
            assertFalse(granted(writer, "GET", "/api/v1/inventories/batches"));
            assertFalse(granted(writer, "PUT", "/api/v1/inventories/2"));
        }
    }

    @Test void systemGrantsRequireExactDocumentedPermissions() {
        assertTrue(granted(user("system:user:grant-role"), "POST", "/api/v1/users/2/roles"));
        assertTrue(granted(user("system:user:grant-permission"), "POST", "/api/v1/users/2/permissions"));
        assertTrue(granted(user("system:user:view-permission"), "GET", "/api/v1/users/2/permissions"));
        assertTrue(granted(user("system:role:grant"), "POST", "/api/v1/roles/2/permissions"));
        assertFalse(granted(user("system:user:grant"), "POST", "/api/v1/users/2/roles"));
        assertFalse(granted(user("system:user:create"), "POST", "/api/v1/users/2/permissions"));
        assertFalse(granted(user("system:user:view"), "GET", "/api/v1/users/2/permissions"));
        assertFalse(granted(user("system:role:create"), "POST", "/api/v1/roles/2/permissions"));
    }

    @Test void systemCrudAndMenuReadsRequireTheirOwnPermissions() {
        for (String resource : new String[]{"user", "role"}) {
            String path = "/api/v1/" + resource + "s";
            assertTrue(granted(user("system:" + resource + ":list"), "GET", path));
            assertTrue(granted(user("system:" + resource + ":create"), "POST", path));
            assertTrue(granted(user("system:" + resource + ":update"), "PUT", path + "/2"));
            assertTrue(granted(user("system:" + resource + ":delete"), "DELETE", path + "/2"));
            assertFalse(granted(user("system:" + resource + ":list"), "DELETE", path + "/2"));
        }
        assertTrue(granted(user("system:user:view"), "GET", "/api/v1/users/2"));
        assertTrue(granted(user("system:menu:view"), "GET", "/api/v1/menus/tree"));
        assertFalse(granted(user(), "GET", "/api/v1/menus/tree"));
        assertFalse(granted(user("system:user:list"), "GET", "/api/v1/users/2/unrecognized"));
    }

    private Authentication user(String... permissions) {
        return new UsernamePasswordAuthenticationToken("tester", "",
                Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
    }

    private boolean granted(Authentication user, String method, String path) {
        return manager.check(() -> user, new RequestAuthorizationContext(new MockHttpServletRequest(method, path))).isGranted();
    }
}
