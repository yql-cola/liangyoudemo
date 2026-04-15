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
