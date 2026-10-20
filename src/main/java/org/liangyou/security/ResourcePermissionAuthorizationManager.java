package org.liangyou.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Enforces the same operation codes exposed in the menu/role editor. */
public final class ResourcePermissionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final Map<String, String> BASIC = Map.of(
            "products", "product", "categories", "category", "units", "unit",
            "unit-converts", "unit-convert", "warehouses", "warehouse",
            "suppliers", "supplier", "customers", "customer");
    private static final Map<String, String> STATISTICS = Map.ofEntries(
            Map.entry("dashboard", "dashboard"), Map.entry("trends", "trend"),
            Map.entry("sale-products", "sale-product"), Map.entry("purchase-products", "purchase-product"),
            Map.entry("inventory-structure", "inventory-structure"), Map.entry("customer-ranks", "customer-rank"),
            Map.entry("supplier-ranks", "supplier-rank"), Map.entry("category-shares", "category-share"),
            Map.entry("daily", "daily"), Map.entry("monthly", "monthly"),
            Map.entry("amount-by-customer", "customer"), Map.entry("amount-by-supplier", "supplier"),
            Map.entry("amount-by-category", "category"));

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication user = authentication.get();
        if (user == null || !user.isAuthenticated()) return new AuthorizationDecision(false);
        Set<String> permissions = user.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
        if (permissions.contains("ROLE_SUPER_ADMIN") || permissions.contains("*:*:*")) return new AuthorizationDecision(true);
        String path = context.getRequest().getRequestURI().substring(context.getRequest().getContextPath().length());
        String method = context.getRequest().getMethod();
        String[] parts = path.split("/");
        if (parts.length < 4 || !"api".equals(parts[1]) || !"v1".equals(parts[2])) return new AuthorizationDecision(false);
        String namespace = parts[3];
        if ("inventories".equals(namespace)) {
            String code = inventoryPermission(method, path);
            boolean saleAvailabilityLookup = "GET".equals(method) && parts.length == 4
                    && (permissions.contains("sale:order:create") || permissions.contains("sale:order:update"))
                    && positiveIdParameter(context.getRequest(), "warehouseId")
                    && positiveIdParameter(context.getRequest(), "productId");
            return new AuthorizationDecision(code != null && permissions.contains(code) || saleAvailabilityLookup);
        }
        if ("purchase-ins".equals(namespace) || "sale-outs".equals(namespace)) {
            if ("sale-outs".equals(namespace) && parts.length >= 6 && parts[4].matches("[1-9]\\d*")) {
                String code = saleAction(method, parts);
                if (code == null) return new AuthorizationDecision(false);
                boolean allowed = permissions.contains(code);
                if ((parts.length == 7 && "export".equals(parts[5])) || "print".equals(parts[5]) || "delivery-pdf".equals(parts[5]))
                    allowed &= permissions.contains("sale:order:view");
                return new AuthorizationDecision(allowed);
            }
            String action = crudAction(method, parts.length == 4,
                    parts.length == 5 && parts[4].matches("[1-9]\\d*"));
            String domain = "purchase-ins".equals(namespace) ? "purchase" : "sale";
            return new AuthorizationDecision(action != null && permissions.contains(domain + ":order:" + action));
        }
        if (Set.of("users", "roles", "menus").contains(namespace)) {
            String code = systemPermission(method, path, parts);
            return new AuthorizationDecision(code != null && permissions.contains(code));
        }
        if (parts.length < 5) return new AuthorizationDecision(false);
        String resource = parts[4];
        if ("basic".equals(parts[3])) {
            String prefix = BASIC.get(resource);
            if (prefix == null) return new AuthorizationDecision(false);
            if ("GET".equals(method) && (path.endsWith("/options") || path.endsWith("/search") || path.matches(".*/products/\\d+/units"))) {
                return new AuthorizationDecision(permissions.contains("basic:" + prefix + ":list")
                        || permissions.stream().anyMatch(ResourcePermissionAuthorizationManager::isOptionConsumer));
            }
            String action = switch (method) {
                case "POST" -> path.endsWith("/import") ? "import" : path.endsWith("/export") ? "export" : "create";
                case "PUT" -> "update";
                case "DELETE" -> "delete";
                case "PATCH" -> "status";
                case "GET" -> path.endsWith("/import/template") ? "import" : path.endsWith("/export") ? "export" : parts.length == 5 || path.endsWith("/tree") ? "list" : "view";
                default -> null;
            };
            // Import creates records; honor existing create grants as well as the dedicated import grant.
            boolean allowed = action != null && permissions.contains("basic:" + prefix + ":" + action);
            if ("import".equals(action)) allowed |= permissions.contains("basic:" + prefix + ":create");
            return new AuthorizationDecision(allowed);
        }
        if ("statistics".equals(parts[3])) {
            String prefix = STATISTICS.get(resource);
            return new AuthorizationDecision("GET".equals(method) && prefix != null
                    && permissions.contains("statistics:" + prefix + (path.endsWith("/export") ? ":export" : ":view")));
        }
        return new AuthorizationDecision(false);
    }

    private static String saleAction(String method, String[] parts) {
        String action = parts[5];
        if (parts.length == 7 && "GET".equals(method) && "export".equals(action)
                && Set.of("pdf", "excel").contains(parts[6])) return "sale:order:export";
        if (parts.length != 6) return null;
        if ("GET".equals(method) && "delivery-pdf".equals(action)) return "sale:order:print";
        if (!"POST".equals(method)) return null;
        return switch (action) {
            case "confirm" -> "sale:order:confirm";
            case "handoff" -> "sale:delivery:complete";
            case "start-delivery" -> "sale:delivery:start";
            case "complete-delivery" -> "sale:delivery:complete";
            case "delivery-failed", "return" -> "sale:delivery:return";
            case "cancel" -> "sale:order:update";
            case "print" -> "sale:order:print";
            default -> null;
        };
    }

    private static String inventoryPermission(String method, String path) {
        if ("GET".equals(method)) {
            return switch (path) {
                case "/api/v1/inventories", "/api/v1/inventories/" -> "inventory:stock:list";
                case "/api/v1/inventories/export" -> "inventory:stock:export";
                case "/api/v1/inventories/batches" -> "inventory:batch:list";
                case "/api/v1/inventories/batches/export" -> "inventory:batch:export";
                case "/api/v1/inventories/flows" -> "inventory:flow:list";
                case "/api/v1/inventories/flows/export" -> "inventory:flow:export";
                default -> path.matches("/api/v1/inventories/[1-9]\\d*/batches") ? "inventory:batch:list" : null;
            };
        }
        if (!path.matches("/api/v1/inventories/[1-9]\\d*")) return null;
        return switch (method) {
            case "PUT" -> "inventory:stock:update";
            case "DELETE" -> "inventory:stock:delete";
            default -> null;
        };
    }

    private static String systemPermission(String method, String path, String[] parts) {
        if ("menus".equals(parts[3])) {
            return "GET".equals(method) && "/api/v1/menus/tree".equals(path) ? "system:menu:view" : null;
        }
        String resource = "users".equals(parts[3]) ? "user" : "role";
        boolean item = parts.length >= 5 && parts[4].matches("[1-9]\\d*");
        if (item && parts.length == 6) {
            String action = null;
            if ("roles".equals(parts[5]) && "user".equals(resource) && "POST".equals(method)) action = "grant-role";
            if ("permissions".equals(parts[5])) {
                if ("POST".equals(method)) action = "user".equals(resource) ? "grant-permission" : "grant";
                if ("GET".equals(method) && "user".equals(resource)) action = "view-permission";
            }
            return action == null ? null : "system:" + resource + ":" + action;
        }
        String action = crudAction(method, parts.length == 4, item && parts.length == 5);
        if ("role".equals(resource) && "view".equals(action)) return null;
        return action == null ? null : "system:" + resource + ":" + action;
    }

    private static String crudAction(String method, boolean collection, boolean item) {
        return switch (method) {
            case "GET" -> collection ? "list" : item ? "view" : null;
            case "POST" -> collection ? "create" : null;
            case "PUT" -> item ? "update" : null;
            case "DELETE" -> item ? "delete" : null;
            default -> null;
        };
    }

    private static boolean positiveIdParameter(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length != 1) return false;
        try {
            return Long.parseLong(values[0]) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isOptionConsumer(String permission) {
        return permission.matches("(?:purchase|sale):order:(?:list|view|create|update)")
                || permission.matches("inventory:(?:stock|batch|flow):(?:list|update|export)")
                || permission.matches("statistics:[a-z-]+:(?:view|export)")
                || permission.startsWith("basic:") && permission.matches(".*:(?:list|create|update)");
    }
}
