package org.liangyou.modules.basic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.CategorySaveRequest;
import org.liangyou.modules.basic.dto.CustomerSaveRequest;
import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.liangyou.modules.basic.dto.ProductSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.dto.SupplierSaveRequest;
import org.liangyou.modules.basic.dto.UnitConvertSaveRequest;
import org.liangyou.modules.basic.dto.UnitSaveRequest;
import org.liangyou.modules.basic.dto.WarehouseSaveRequest;
import org.liangyou.modules.basic.entity.BaseCustomer;
import org.liangyou.modules.basic.entity.BaseProductCategory;
import org.liangyou.modules.basic.entity.BaseProductUnitConvert;
import org.liangyou.modules.basic.entity.BaseSupplier;
import org.liangyou.modules.basic.entity.BaseUnit;
import org.liangyou.modules.basic.entity.BaseWarehouse;
import org.liangyou.modules.basic.mapper.BaseCustomerMapper;
import org.liangyou.modules.basic.mapper.BaseProductCategoryMapper;
import org.liangyou.modules.basic.mapper.BaseProductUnitConvertMapper;
import org.liangyou.modules.basic.mapper.BaseSupplierMapper;
import org.liangyou.modules.basic.mapper.BaseUnitMapper;
import org.liangyou.modules.basic.mapper.BaseWarehouseMapper;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.vo.ProductResponse;
import org.liangyou.modules.inventory.entity.InvBatch;
import org.liangyou.modules.inventory.entity.InvStock;
import org.liangyou.modules.inventory.entity.InvStockFlow;
import org.liangyou.modules.inventory.mapper.InvBatchMapper;
import org.liangyou.modules.inventory.mapper.InvStockFlowMapper;
import org.liangyou.modules.inventory.mapper.InvStockMapper;
import org.liangyou.modules.product.entity.BaseProduct;
import org.liangyou.modules.product.mapper.BaseProductMapper;
import org.liangyou.modules.purchase.entity.BizPurchaseIn;
import org.liangyou.modules.purchase.entity.BizPurchaseInItem;
import org.liangyou.modules.purchase.mapper.BizPurchaseInItemMapper;
import org.liangyou.modules.purchase.mapper.BizPurchaseInMapper;
import org.liangyou.modules.sale.entity.BizSaleOut;
import org.liangyou.modules.sale.entity.BizSaleOutItem;
import org.liangyou.modules.sale.mapper.BizSaleOutItemMapper;
import org.liangyou.modules.sale.mapper.BizSaleOutMapper;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class BasicDataService {

    private final BaseProductMapper baseProductMapper;
    private final BaseProductCategoryMapper categoryMapper;
    private final BaseUnitMapper unitMapper;
    private final BaseProductUnitConvertMapper unitConvertMapper;
    private final BaseWarehouseMapper warehouseMapper;
    private final BaseSupplierMapper supplierMapper;
    private final BaseCustomerMapper customerMapper;
    private final BizPurchaseInItemMapper purchaseInItemMapper;
    private final BizSaleOutItemMapper saleOutItemMapper;
    private final InvStockMapper invStockMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvStockFlowMapper invStockFlowMapper;
    private final BizPurchaseInMapper purchaseInMapper;
    private final BizSaleOutMapper saleOutMapper;
    private final SysUserMapper sysUserMapper;

    public PageResponse<ProductResponse> queryProducts(ProductQueryRequest request) {
        Page<BaseProduct> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BaseProduct> wrapper = productQuery(request);
        Page<BaseProduct> result = baseProductMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toProductResponse).toList(), result.getTotal());
    }

    public ProductResponse productDetail(Long id) {
        BaseProduct product = master(baseProductMapper, id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductSaveRequest request) {
        BaseProduct product = new BaseProduct();
        validateProductRequest(product.getId(), request);
        applyProductRequest(product, request);
        product.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        write(() -> baseProductMapper.insert(product));
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductSaveRequest request) {
        BaseProduct product = master(baseProductMapper, id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        authorizeStatusChange("product", product.getStatus(), request.getStatus());
        validateProductRequest(product.getId(), request);
        applyProductRequest(product, request);
        product.setStatus(request.getStatus() == null ? product.getStatus() : request.getStatus());
        write(() -> baseProductMapper.updateById(product));
        return toProductResponse(product);
    }

    @Transactional
    public void updateProductStatus(Long id, StatusUpdateRequest request) {
        BaseProduct product = master(baseProductMapper, id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        if (validStatus(request) == 1) {
            assertCategoryEnabled(product.getCategoryId());
            requireEnabled(master(unitMapper, product.getBaseUnitId()));
        }
        product.setStatus(request.getStatus());
        write(() -> baseProductMapper.updateById(product));
    }

    public List<BasicOptionResponse> productOptions(String keyword) {
        LambdaQueryWrapper<BaseProduct> wrapper = new LambdaQueryWrapper<BaseProduct>()
                .eq(BaseProduct::getStatus, 1)
                .and(StringUtils.hasText(keyword), w -> w.like(BaseProduct::getProductName, keyword).or().like(BaseProduct::getProductCode, keyword))
                .orderByAsc(BaseProduct::getProductCode);
        return baseProductMapper.selectList(wrapper).stream()
                .filter(this::productSelectable).map(this::toProductOption).toList();
    }

    @Transactional
    public void deleteProduct(Long id) {
        BaseProduct product = master(baseProductMapper, id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        if (hasProductReference(id)) {
            throw new BusinessException(400, "basic-data-in-use");
        }
        baseProductMapper.deleteById(id);
    }

    @Transactional
    public void validatePurchaseMasterData(Long warehouseId, Long supplierId, List<Long> productIds, List<Long> unitIds) {
        assertWarehouseEnabled(warehouseId);
        requireEnabled(master(supplierMapper, supplierId));
        validateProductUnits(productIds, unitIds);
    }

    @Transactional
    public void validatePurchaseMasterData(Long warehouseId, Long supplierId, Long productId, Long unitId) {
        validatePurchaseMasterData(warehouseId, supplierId, List.of(productId), List.of(unitId));
    }

    @Transactional
    public void validateSaleMasterData(Long warehouseId, Long customerId, List<Long> productIds, List<Long> unitIds) {
        assertWarehouseEnabled(warehouseId);
        if (customerId != null) requireEnabled(master(customerMapper, customerId));
        validateProductUnits(productIds, unitIds);
    }

    @Transactional
    public void validateSaleMasterData(Long warehouseId, Long customerId, Long productId, Long unitId) {
        validateSaleMasterData(warehouseId, customerId, List.of(productId), List.of(unitId));
    }

    private void validateProductUnits(List<Long> productIds, List<Long> unitIds) {
        if (productIds == null || unitIds == null || productIds.size() != unitIds.size()) {
            throw new BusinessException(400, "product-unit-invalid");
        }
        lockProducts(productIds);
        for (int i = 0; i < productIds.size(); i++) productUnitRate(productIds.get(i), unitIds.get(i));
    }

    public PageResponse<BaseProductCategory> queryCategories(BasicQueryRequest request) {
        Page<BaseProductCategory> result = categoryMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), categoryQuery(request));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseProductCategory categoryDetail(Long id) {
        return require(master(categoryMapper, id));
    }

    @Transactional
    public BaseProductCategory createCategory(CategorySaveRequest request) {
        lockCategories();
        validateCategory(null, request);
        BaseProductCategory entity = new BaseProductCategory();
        entity.setCategoryName(request.getCategoryName());
        entity.setParentId(request.getParentId());
        entity.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
        write(() -> categoryMapper.insert(entity));
        return entity;
    }

    @Transactional
    public BaseProductCategory updateCategory(Long id, CategorySaveRequest request) {
        lockCategories();
        BaseProductCategory entity = categoryDetail(id);
        authorizeStatusChange("category", entity.getStatus(), request.getStatus());
        validateCategory(id, request);
        entity.setCategoryName(request.getCategoryName());
        entity.setParentId(request.getParentId());
        entity.setSortNo(request.getSortNo() == null ? entity.getSortNo() : request.getSortNo());
        entity.setStatus(request.getStatus() == null ? entity.getStatus() : request.getStatus());
        entity.setRemark(request.getRemark());
        write(() -> categoryMapper.updateById(entity));
        return entity;
    }

    @Transactional
    public void deleteCategory(Long id) {
        lockCategories();
        categoryDetail(id);
        inUse(categoryMapper.selectCount(new LambdaQueryWrapper<BaseProductCategory>().eq(BaseProductCategory::getParentId, id)) > 0
                || baseProductMapper.selectCount(new LambdaQueryWrapper<BaseProduct>().eq(BaseProduct::getCategoryId, id)) > 0
                || categoryMapper.countSummaryReferences(id) > 0);
        categoryMapper.deleteById(id);
    }

    @Transactional
    public void updateCategoryStatus(Long id, StatusUpdateRequest request) {
        lockCategories();
        BaseProductCategory entity = categoryDetail(id);
        if (validStatus(request) == 1 && entity.getParentId() != null && entity.getParentId() != 0) assertCategoryEnabled(entity.getParentId());
        entity.setStatus(validStatus(request));
        write(() -> categoryMapper.updateById(entity));
    }

    public List<BasicOptionResponse> categoryOptions(String keyword) {
        List<BaseProductCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<BaseProductCategory>().orderByAsc(BaseProductCategory::getSortNo));
        Map<Long, BaseProductCategory> map = new HashMap<>();
        categories.forEach(item -> map.put(item.getId(), item));
        return categories.stream().filter(item -> categorySelectable(item, map))
                .filter(item -> !StringUtils.hasText(keyword) || item.getCategoryName().contains(keyword))
                .map(item -> option(item.getId(), null, item.getCategoryName(), item.getStatus())).toList();
    }

    public List<BaseProductCategory> categoryTree() {
        List<BaseProductCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<BaseProductCategory>()
                .orderByAsc(BaseProductCategory::getSortNo).orderByAsc(BaseProductCategory::getId));
        Map<Long, BaseProductCategory> map = new LinkedHashMap<>();
        categories.forEach(item -> { item.setChildren(new ArrayList<>()); map.put(item.getId(), item); });
        List<BaseProductCategory> roots = new ArrayList<>();
        for (BaseProductCategory item : categories) {
            BaseProductCategory parent = map.get(item.getParentId());
            if (parent == null || parent == item) roots.add(item);
            else parent.getChildren().add(item);
        }
        return roots;
    }

    public PageResponse<BaseUnit> queryUnits(BasicQueryRequest request) {
        Page<BaseUnit> result = unitMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), unitQuery(request));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseUnit unitDetail(Long id) {
        return require(master(unitMapper, id));
    }

    @Transactional
    public BaseUnit createUnit(UnitSaveRequest request) {
        validateUnit(null, request);
        BaseUnit entity = new BaseUnit();
        entity.setUnitCode(request.getUnitCode());
        entity.setUnitName(request.getUnitName());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
        write(() -> unitMapper.insert(entity));
        return entity;
    }

    @Transactional
    public BaseUnit updateUnit(Long id, UnitSaveRequest request) {
        BaseUnit entity = unitDetail(id);
        authorizeStatusChange("unit", entity.getStatus(), request.getStatus());
        validateUnit(id, request);
        entity.setUnitCode(request.getUnitCode());
        entity.setUnitName(request.getUnitName());
        entity.setStatus(request.getStatus() == null ? entity.getStatus() : request.getStatus());
        entity.setRemark(request.getRemark());
        write(() -> unitMapper.updateById(entity));
        return entity;
    }

    @Transactional
    public void deleteUnit(Long id) {
        unitDetail(id);
        inUse(baseProductMapper.selectCount(new LambdaQueryWrapper<BaseProduct>().eq(BaseProduct::getBaseUnitId, id)) > 0
                || purchaseInItemMapper.selectCount(new LambdaQueryWrapper<BizPurchaseInItem>().eq(BizPurchaseInItem::getUnitId, id)) > 0
                || saleOutItemMapper.selectCount(new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getUnitId, id)) > 0
                || unitConvertMapper.selectCount(new LambdaQueryWrapper<BaseProductUnitConvert>()
                .and(w -> w.eq(BaseProductUnitConvert::getFromUnitId, id).or().eq(BaseProductUnitConvert::getToUnitId, id))) > 0);
        unitMapper.deleteById(id);
    }

    @Transactional
    public void updateUnitStatus(Long id, StatusUpdateRequest request) {
        BaseUnit entity = unitDetail(id);
        entity.setStatus(validStatus(request));
        write(() -> unitMapper.updateById(entity));
    }

    public List<BasicOptionResponse> unitOptions(String keyword) {
        return unitMapper.selectList(new LambdaQueryWrapper<BaseUnit>()
                        .eq(BaseUnit::getStatus, 1)
                        .and(StringUtils.hasText(keyword), w -> w.like(BaseUnit::getUnitName, keyword).or().like(BaseUnit::getUnitCode, keyword)))
                .stream().map(item -> option(item.getId(), item.getUnitCode(), item.getUnitName(), item.getStatus())).toList();
    }

    public PageResponse<BaseWarehouse> queryWarehouses(BasicQueryRequest request) {
        Page<BaseWarehouse> result = warehouseMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), warehouseQuery(request));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseWarehouse warehouseDetail(Long id) {
        return require(master(warehouseMapper, id));
    }

    @Transactional
    public BaseWarehouse createWarehouse(WarehouseSaveRequest request) {
        BaseWarehouse entity = new BaseWarehouse();
        validOptionalStatus(request.getStatus());
        unique(warehouseMapper.selectCount(new LambdaQueryWrapper<BaseWarehouse>().eq(BaseWarehouse::getWarehouseCode, request.getWarehouseCode())
                .ne(entity.getId() != null, BaseWarehouse::getId, entity.getId())));
        applyWarehouse(entity, request);
        write(() -> warehouseMapper.insert(entity));
        return entity;
    }

    @Transactional
    public BaseWarehouse updateWarehouse(Long id, WarehouseSaveRequest request) {
        BaseWarehouse entity = warehouseDetail(id);
        authorizeStatusChange("warehouse", entity.getStatus(), request.getStatus());
        validOptionalStatus(request.getStatus());
        unique(warehouseMapper.selectCount(new LambdaQueryWrapper<BaseWarehouse>().eq(BaseWarehouse::getWarehouseCode, request.getWarehouseCode())
                .ne(entity.getId() != null, BaseWarehouse::getId, entity.getId())));
        applyWarehouse(entity, request);
        write(() -> warehouseMapper.updateById(entity));
        return entity;
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        warehouseDetail(id);
        if (hasWarehouseReference(id)) {
            throw new BusinessException(400, "basic-data-in-use");
        }
        warehouseMapper.deleteById(id);
    }

    @Transactional
    public void updateWarehouseStatus(Long id, StatusUpdateRequest request) {
        BaseWarehouse entity = warehouseDetail(id);
        entity.setStatus(validStatus(request));
        write(() -> warehouseMapper.updateById(entity));
    }

    public List<BasicOptionResponse> warehouseOptions(String keyword) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<BaseWarehouse>()
                        .eq(BaseWarehouse::getStatus, 1)
                        .and(StringUtils.hasText(keyword), w -> w.like(BaseWarehouse::getWarehouseName, keyword).or().like(BaseWarehouse::getWarehouseCode, keyword)))
                .stream().map(item -> option(item.getId(), item.getWarehouseCode(), item.getWarehouseName(), item.getStatus())).toList();
    }

    public PageResponse<BaseSupplier> querySuppliers(BasicQueryRequest request) {
        Page<BaseSupplier> result = supplierMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), supplierQuery(request));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseSupplier supplierDetail(Long id) {
        return require(master(supplierMapper, id));
    }

    @Transactional
    public BaseSupplier createSupplier(SupplierSaveRequest request) {
        BaseSupplier entity = new BaseSupplier();
        validOptionalStatus(request.getStatus());
        unique(supplierMapper.selectCount(new LambdaQueryWrapper<BaseSupplier>().eq(BaseSupplier::getSupplierCode, request.getSupplierCode())
                .ne(entity.getId() != null, BaseSupplier::getId, entity.getId())));
        applySupplier(entity, request);
        write(() -> supplierMapper.insert(entity));
        return entity;
    }

    @Transactional
    public BaseSupplier updateSupplier(Long id, SupplierSaveRequest request) {
        BaseSupplier entity = supplierDetail(id);
        authorizeStatusChange("supplier", entity.getStatus(), request.getStatus());
        validOptionalStatus(request.getStatus());
        unique(supplierMapper.selectCount(new LambdaQueryWrapper<BaseSupplier>().eq(BaseSupplier::getSupplierCode, request.getSupplierCode())
                .ne(entity.getId() != null, BaseSupplier::getId, entity.getId())));
        applySupplier(entity, request);
        write(() -> supplierMapper.updateById(entity));
        return entity;
    }

    @Transactional
    public void deleteSupplier(Long id) {
        supplierDetail(id);
        inUse(purchaseInMapper.selectCount(new LambdaQueryWrapper<BizPurchaseIn>().eq(BizPurchaseIn::getSupplierId, id)) > 0
                || supplierMapper.countSummaryReferences(id) > 0);
        supplierMapper.deleteById(id);
    }

    @Transactional
    public void updateSupplierStatus(Long id, StatusUpdateRequest request) {
        BaseSupplier entity = supplierDetail(id);
        entity.setStatus(validStatus(request));
        write(() -> supplierMapper.updateById(entity));
    }

    public List<BasicOptionResponse> supplierOptions(String keyword) {
        return supplierMapper.selectList(new LambdaQueryWrapper<BaseSupplier>()
                        .eq(BaseSupplier::getStatus, 1)
                        .and(StringUtils.hasText(keyword), w -> w.like(BaseSupplier::getSupplierName, keyword).or().like(BaseSupplier::getSupplierCode, keyword)))
                .stream().map(item -> option(item.getId(), item.getSupplierCode(), item.getSupplierName(), item.getStatus())).toList();
    }

    public PageResponse<BaseCustomer> queryCustomers(BasicQueryRequest request) {
        Page<BaseCustomer> result = customerMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), customerQuery(request));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseCustomer customerDetail(Long id) {
        return require(master(customerMapper, id));
    }

    public List<BaseCustomer> searchCustomers(String keyword) {
        if (!StringUtils.hasText(keyword)) return List.of();
        return customerMapper.selectList(new LambdaQueryWrapper<BaseCustomer>()
                .eq(BaseCustomer::getStatus, 1)
                .and(w -> w.like(BaseCustomer::getCustomerName, keyword.trim())
                        .or().like(BaseCustomer::getCustomerCode, keyword.trim())
                        .or().like(BaseCustomer::getContactPhone, keyword.trim()))
                .orderByDesc(BaseCustomer::getId).last("LIMIT 20"));
    }

    @Transactional
    public BaseCustomer quickCreateCustomer(String name, String contact, String phone, String address) {
        if (!StringUtils.hasText(name) || name.length() > 128) throw new BusinessException(400, "customer-name-required");
        BaseCustomer customer = new BaseCustomer();
        customer.setCustomerCode("C" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        customer.setCustomerName(name.trim()); customer.setContactName(contact);
        customer.setContactPhone(phone); customer.setAddress(address); customer.setStatus(1);
        write(() -> customerMapper.insert(customer));
        return customer;
    }

    @Transactional
    public BaseCustomer createCustomer(CustomerSaveRequest request) {
        BaseCustomer entity = new BaseCustomer();
        validOptionalStatus(request.getStatus());
        unique(customerMapper.selectCount(new LambdaQueryWrapper<BaseCustomer>().eq(BaseCustomer::getCustomerCode, request.getCustomerCode())
                .ne(entity.getId() != null, BaseCustomer::getId, entity.getId())));
        applyCustomer(entity, request);
        write(() -> customerMapper.insert(entity));
        return entity;
    }

    @Transactional
    public BaseCustomer updateCustomer(Long id, CustomerSaveRequest request) {
        BaseCustomer entity = customerDetail(id);
        authorizeStatusChange("customer", entity.getStatus(), request.getStatus());
        validOptionalStatus(request.getStatus());
        unique(customerMapper.selectCount(new LambdaQueryWrapper<BaseCustomer>().eq(BaseCustomer::getCustomerCode, request.getCustomerCode())
                .ne(entity.getId() != null, BaseCustomer::getId, entity.getId())));
        applyCustomer(entity, request);
        write(() -> customerMapper.updateById(entity));
        return entity;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        customerDetail(id);
        inUse(saleOutMapper.selectCount(new LambdaQueryWrapper<BizSaleOut>().eq(BizSaleOut::getCustomerId, id)) > 0
                || customerMapper.countSummaryReferences(id) > 0);
        customerMapper.deleteById(id);
    }

    @Transactional
    public void updateCustomerStatus(Long id, StatusUpdateRequest request) {
        BaseCustomer entity = customerDetail(id);
        entity.setStatus(validStatus(request));
        write(() -> customerMapper.updateById(entity));
    }

    public List<BasicOptionResponse> customerOptions(String keyword) {
        return customerMapper.selectList(new LambdaQueryWrapper<BaseCustomer>()
                        .eq(BaseCustomer::getStatus, 1)
                        .and(StringUtils.hasText(keyword), w -> w.like(BaseCustomer::getCustomerName, keyword).or().like(BaseCustomer::getCustomerCode, keyword)))
                .stream().map(item -> option(item.getId(), item.getCustomerCode(), item.getCustomerName(), item.getStatus())).toList();
    }

    public PageResponse<BaseProductUnitConvert> queryUnitConverts(BasicQueryRequest request) {
        Page<BaseProductUnitConvert> result = unitConvertMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                convertQuery(request));
        result.getRecords().forEach(this::enrichConversion);
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseProductUnitConvert unitConvertDetail(Long id) {
        return enrichConversion(require(unitConvertMapper.selectById(id)));
    }

    @Transactional
    public BaseProductUnitConvert createUnitConvert(UnitConvertSaveRequest request) {
        BaseProductUnitConvert entity = new BaseProductUnitConvert();
        validateConversion(entity.getId(), request);
        applyUnitConvert(entity, request);
        write(() -> unitConvertMapper.insert(entity));
        return entity;
    }

    @Transactional
    public BaseProductUnitConvert updateUnitConvert(Long id, UnitConvertSaveRequest request) {
        BaseProductUnitConvert entity = lockConversion(id, request.getProductId());
        authorizeStatusChange("unit-convert", entity.getStatus(), request.getStatus());
        validateConversion(entity.getId(), request);
        applyUnitConvert(entity, request);
        write(() -> unitConvertMapper.updateById(entity));
        return entity;
    }

    @Transactional
    public void deleteUnitConvert(Long id) {
        lockConversion(id, null);
        unitConvertMapper.deleteById(id);
    }

    @Transactional
    public void updateUnitConvertStatus(Long id, StatusUpdateRequest request) {
        BaseProductUnitConvert entity = lockConversion(id, null);
        if (validStatus(request) == 1) {
            UnitConvertSaveRequest validation = new UnitConvertSaveRequest();
            validation.setProductId(entity.getProductId());
            validation.setFromUnitId(entity.getFromUnitId());
            validation.setToUnitId(entity.getToUnitId());
            validation.setConvertRate(entity.getConvertRate());
            validation.setStatus(1);
            validateConversion(id, validation);
        }
        entity.setStatus(validStatus(request));
        write(() -> unitConvertMapper.updateById(entity));
    }

    public List<BasicOptionResponse> unitConvertOptions(String keyword) {
        BasicQueryRequest request = new BasicQueryRequest();
        request.setKeyword(keyword);
        return unitConvertOptions(request);
    }

    public List<BasicOptionResponse> unitConvertOptions(BasicQueryRequest request) {
        request.setStatus(1);
        return unitConvertMapper.selectList(convertQuery(request)).stream()
                .filter(item -> productSelectable(master(baseProductMapper, item.getProductId()))
                        && enabledUnit(item.getFromUnitId()) && enabledUnit(item.getToUnitId()))
                .map(this::toConversionOption).toList();
    }

    @Transactional
    public void assertWarehouseEnabled(Long warehouseId) {
        requireEnabled(master(warehouseMapper, warehouseId));
    }

    @Transactional
    public void assertProductEnabled(Long productId) {
        BaseProduct product = require(master(baseProductMapper, productId));
        requireEnabled(product);
        assertCategoryEnabled(product.getCategoryId());
        lockUnits();
        requireEnabled(master(unitMapper, product.getBaseUnitId()));
    }

    private boolean hasProductReference(Long productId) {
        return countPurchaseItems(productId) > 0
                || countSaleItems(productId) > 0
                || invStockMapper.selectCount(new LambdaQueryWrapper<InvStock>().eq(InvStock::getProductId, productId)) > 0
                || invBatchMapper.selectCount(new LambdaQueryWrapper<InvBatch>().eq(InvBatch::getProductId, productId)) > 0
                || invStockFlowMapper.selectCount(new LambdaQueryWrapper<InvStockFlow>().eq(InvStockFlow::getProductId, productId)) > 0
                || unitConvertMapper.selectCount(new LambdaQueryWrapper<BaseProductUnitConvert>().eq(BaseProductUnitConvert::getProductId, productId)) > 0;
    }

    private long countPurchaseItems(Long productId) {
        Long count = purchaseInItemMapper.selectCount(
                new LambdaQueryWrapper<BizPurchaseInItem>().eq(BizPurchaseInItem::getProductId, productId));
        return count == null ? 0 : count;
    }

    private long countSaleItems(Long productId) {
        Long count = saleOutItemMapper.selectCount(
                new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getProductId, productId));
        return count == null ? 0 : count;
    }

    private BasicOptionResponse toProductOption(BaseProduct product) {
        BasicOptionResponse response = new BasicOptionResponse();
        response.setId(product.getId());
        response.setCode(product.getProductCode());
        response.setName(product.getProductName());
        response.setStatus(product.getStatus());
        ProductResponse detail = toProductResponse(product);
        response.setCategoryId(product.getCategoryId());
        response.setCategoryName(detail.getCategoryName());
        response.setBaseUnitId(product.getBaseUnitId());
        response.setBaseUnitName(detail.getBaseUnitName());
        response.setSpec(product.getSpec());
        response.setBrandName(product.getBrandName());
        return response;
    }

    private void applyProductRequest(BaseProduct product, ProductSaveRequest request) {
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setCategoryId(request.getCategoryId());
        product.setBrandName(request.getBrandName());
        product.setSpec(request.getSpec());
        product.setBarcode(request.getBarcode());
        product.setImageUrl(request.getImageUrl());
        product.setBaseUnitId(request.getBaseUnitId());
        product.setShelfLifeDays(request.getShelfLifeDays());
        product.setRemark(request.getRemark());
    }

    private ProductResponse toProductResponse(BaseProduct product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getProductName());
        response.setCategoryId(product.getCategoryId());
        BaseProductCategory category = master(categoryMapper, product.getCategoryId());
        BaseUnit baseUnit = master(unitMapper, product.getBaseUnitId());
        response.setCategoryName(category == null ? null : category.getCategoryName());
        response.setBaseUnitName(baseUnit == null ? null : baseUnit.getUnitName());
        response.setBrandName(product.getBrandName());
        response.setSpec(product.getSpec());
        response.setBarcode(product.getBarcode());
        response.setImageUrl(product.getImageUrl());
        response.setBaseUnitId(product.getBaseUnitId());
        response.setShelfLifeDays(product.getShelfLifeDays());
        response.setStatus(product.getStatus());
        response.setRemark(product.getRemark());
        response.setCreatedTime(product.getCreatedTime());
        response.setUpdatedTime(product.getUpdatedTime());
        return response;
    }

    public boolean hasWarehouseReference(Long warehouseId) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getWarehouseId, warehouseId)) > 0
                || purchaseInMapper.selectCount(new LambdaQueryWrapper<BizPurchaseIn>().eq(BizPurchaseIn::getWarehouseId, warehouseId)) > 0
                || saleOutMapper.selectCount(new LambdaQueryWrapper<BizSaleOut>().eq(BizSaleOut::getWarehouseId, warehouseId)) > 0
                || invStockMapper.selectCount(new LambdaQueryWrapper<InvStock>().eq(InvStock::getWarehouseId, warehouseId)) > 0
                || invBatchMapper.selectCount(new LambdaQueryWrapper<InvBatch>().eq(InvBatch::getWarehouseId, warehouseId)) > 0
                || invStockFlowMapper.selectCount(new LambdaQueryWrapper<InvStockFlow>().eq(InvStockFlow::getWarehouseId, warehouseId)) > 0
                || warehouseMapper.countSummaryReferences(warehouseId) > 0;
    }

    /** PUT may edit status too; retain the independent permission enforced by the PATCH route. */
    private void authorizeStatusChange(String resource, Integer current, Integer requested) {
        if (requested == null || Objects.equals(current, requested)) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Internal service calls without an authenticated caller keep their existing behavior.
        if (authentication == null || !authentication.isAuthenticated()) return;
        boolean allowed = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
                .anyMatch(permission -> permission.equals("basic:" + resource + ":status")
                        || permission.equals("ROLE_SUPER_ADMIN") || permission.equals("*:*:*"));
        if (!allowed) throw new AccessDeniedException("Status permission required");
    }

    private int validStatus(StatusUpdateRequest request) {
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new BusinessException(400, "status-invalid");
        }
        return request.getStatus();
    }

    private boolean inTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Warehouse locks come first in business services, then partners, sorted products, categories, units. */
    private <T> T master(BaseMapper<T> mapper, Long id) {
        if (id == null) return null;
        return inTransaction() ? mapper.selectOne(new QueryWrapper<T>().eq("id", id).last("FOR UPDATE"))
                : mapper.selectById(id);
    }

    private void lockProducts(Collection<Long> ids) {
        if (inTransaction()) ids.stream().filter(Objects::nonNull).distinct().sorted()
                .forEach(id -> require(master(baseProductMapper, id)));
    }

    /** A category move can change every ancestor path; acquire the graph in a stable order. */
    private void lockCategories() {
        if (inTransaction()) categoryMapper.selectList(new QueryWrapper<BaseProductCategory>().orderByAsc("id").last("FOR UPDATE"));
    }

    /** Conversions may traverse intermediate units, so lock the unit set before walking any path. */
    private void lockUnits() {
        if (inTransaction()) unitMapper.selectList(new QueryWrapper<BaseUnit>().orderByAsc("id").last("FOR UPDATE"));
    }

    private LambdaQueryWrapper<BaseProductUnitConvert> currentEdges(LambdaQueryWrapper<BaseProductUnitConvert> query) {
        return inTransaction() ? query.orderByAsc(BaseProductUnitConvert::getId).last("FOR UPDATE") : query;
    }

    private BaseProductUnitConvert lockConversion(Long id, Long newProductId) {
        BaseProductUnitConvert observed = require(unitConvertMapper.selectById(id));
        lockProducts(Arrays.asList(observed.getProductId(), newProductId));
        BaseProductUnitConvert current = require(master(unitConvertMapper, id));
        if (!Objects.equals(observed.getProductId(), current.getProductId()))
            throw new BusinessException(400, "basic-data-concurrent-change");
        return current;
    }

    private <T> T require(T entity) {
        if (entity == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        return entity;
    }

    private void requireEnabled(Object entity) {
        if (entity == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        Integer status = null;
        if (entity instanceof BaseWarehouse warehouse) {
            status = warehouse.getStatus();
        } else if (entity instanceof BaseSupplier supplier) {
            status = supplier.getStatus();
        } else if (entity instanceof BaseCustomer customer) {
            status = customer.getStatus();
        } else if (entity instanceof BaseProduct product) {
            status = product.getStatus();
        } else if (entity instanceof BaseUnit unit) {
            status = unit.getStatus();
        } else if (entity instanceof BaseProductCategory category) {
            status = category.getStatus();
        }
        if (status == null || status != 1) {
            throw new BusinessException(400, "basic-data-disabled");
        }
    }

    private BasicOptionResponse option(Long id, String code, String name, Integer status) {
        BasicOptionResponse response = new BasicOptionResponse();
        response.setId(id);
        response.setCode(code);
        response.setName(name);
        response.setStatus(status);
        return response;
    }

    private void applyWarehouse(BaseWarehouse entity, WarehouseSaveRequest request) {
        entity.setWarehouseCode(request.getWarehouseCode());
        entity.setWarehouseName(request.getWarehouseName());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setAddress(request.getAddress());
        entity.setStatus(request.getStatus() == null ? (entity.getStatus() == null ? 1 : entity.getStatus()) : request.getStatus());
        entity.setRemark(request.getRemark());
    }

    private void applySupplier(BaseSupplier entity, SupplierSaveRequest request) {
        entity.setSupplierCode(request.getSupplierCode());
        entity.setSupplierName(request.getSupplierName());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setAddress(request.getAddress());
        entity.setTaxNo(request.getTaxNo());
        entity.setBankName(request.getBankName());
        entity.setBankAccount(request.getBankAccount());
        entity.setStatus(request.getStatus() == null ? (entity.getStatus() == null ? 1 : entity.getStatus()) : request.getStatus());
        entity.setRemark(request.getRemark());
    }

    private void applyCustomer(BaseCustomer entity, CustomerSaveRequest request) {
        entity.setCustomerCode(request.getCustomerCode());
        entity.setCustomerName(request.getCustomerName());
        entity.setContactName(request.getContactName());
        entity.setContactPhone(request.getContactPhone());
        entity.setAddress(request.getAddress());
        entity.setTaxNo(request.getTaxNo());
        entity.setBankName(request.getBankName());
        entity.setBankAccount(request.getBankAccount());
        entity.setStatus(request.getStatus() == null ? (entity.getStatus() == null ? 1 : entity.getStatus()) : request.getStatus());
        entity.setRemark(request.getRemark());
    }

    private void applyUnitConvert(BaseProductUnitConvert entity, UnitConvertSaveRequest request) {
        entity.setProductId(request.getProductId());
        entity.setFromUnitId(request.getFromUnitId());
        entity.setToUnitId(request.getToUnitId());
        entity.setConvertRate(request.getConvertRate());
        entity.setStatus(request.getStatus() == null ? (entity.getStatus() == null ? 1 : entity.getStatus()) : request.getStatus());
        entity.setRemark(request.getRemark());
    }
    private void validOptionalStatus(Integer status) {
        if (status != null && status != 0 && status != 1) throw new BusinessException(400, "status-invalid");
    }

    private void write(Supplier<Integer> operation) {
        try { operation.get(); }
        catch (DuplicateKeyException exception) { throw new BusinessException(400, "basic-data-duplicate"); }
    }

    private void unique(Long count) {
        if (count != null && count > 0) throw new BusinessException(400, "basic-data-duplicate");
    }

    private void inUse(boolean referenced) {
        if (referenced) throw new BusinessException(400, "basic-data-in-use");
    }

    private void validateProductRequest(Long id, ProductSaveRequest request) {
        validOptionalStatus(request.getStatus());
        if (!StringUtils.hasText(request.getProductCode()) || !StringUtils.hasText(request.getProductName()))
            throw new BusinessException(400, "product-required");
        if (request.getShelfLifeDays() != null && request.getShelfLifeDays() < 0)
            throw new BusinessException(400, "shelf-life-invalid");
        unique(baseProductMapper.selectCount(new LambdaQueryWrapper<BaseProduct>()
                .eq(BaseProduct::getProductCode, request.getProductCode()).ne(id != null, BaseProduct::getId, id)));
        assertCategoryEnabled(request.getCategoryId());
        requireEnabled(master(unitMapper, request.getBaseUnitId()));
        if (id != null) {
            BaseProduct existing = require(master(baseProductMapper, id));
            if (!Objects.equals(existing.getBaseUnitId(), request.getBaseUnitId())) inUse(hasProductReference(id));
        }
    }

    private void validateUnit(Long id, UnitSaveRequest request) {
        validOptionalStatus(request.getStatus());
        unique(unitMapper.selectCount(new LambdaQueryWrapper<BaseUnit>()
                .eq(BaseUnit::getUnitCode, request.getUnitCode()).ne(id != null, BaseUnit::getId, id)));
    }

    private void validateCategory(Long id, CategorySaveRequest request) {
        validOptionalStatus(request.getStatus());
        if (request.getParentId() == null || request.getParentId() < 0) throw new BusinessException(400, "category-parent-invalid");
        Set<Long> visited = new HashSet<>();
        if (id != null) visited.add(id);
        Long parentId = request.getParentId();
        while (parentId != null && parentId != 0) {
            if (!visited.add(parentId)) throw new BusinessException(400, "category-cycle");
            BaseProductCategory parent = require(master(categoryMapper, parentId));
            requireEnabled(parent);
            parentId = parent.getParentId();
        }
        unique(categoryMapper.selectCount(new LambdaQueryWrapper<BaseProductCategory>()
                .eq(BaseProductCategory::getParentId, request.getParentId())
                .eq(BaseProductCategory::getCategoryName, request.getCategoryName())
                .ne(id != null, BaseProductCategory::getId, id)));
    }

    private void assertCategoryEnabled(Long id) {
        lockCategories();
        if (id == null || id == 0) throw new BusinessException(400, "category-required");
        Set<Long> visited = new HashSet<>();
        while (id != null && id != 0) {
            if (!visited.add(id)) throw new BusinessException(400, "category-cycle");
            BaseProductCategory category = require(master(categoryMapper, id));
            requireEnabled(category);
            id = category.getParentId();
        }
    }

    private boolean categorySelectable(BaseProductCategory category, Map<Long, BaseProductCategory> categories) {
        Set<Long> visited = new HashSet<>();
        while (category != null) {
            if (!Integer.valueOf(1).equals(category.getStatus()) || !visited.add(category.getId())) return false;
            if (category.getParentId() == null || category.getParentId() == 0) return true;
            category = categories.get(category.getParentId());
        }
        return false;
    }

    private boolean productSelectable(BaseProduct product) {
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) return false;
            if (product.getCategoryId() == null || product.getBaseUnitId() == null) return false;
        try { assertCategoryEnabled(product.getCategoryId()); return enabledUnit(product.getBaseUnitId()); }
        catch (BusinessException exception) { return false; }
    }

    private boolean enabledUnit(Long id) {
        BaseUnit unit = id == null ? null : master(unitMapper, id);
        return unit != null && Integer.valueOf(1).equals(unit.getStatus());
    }

    private void validateConversion(Long id, UnitConvertSaveRequest request) {
        lockProducts(Collections.singletonList(request.getProductId()));
        validOptionalStatus(request.getStatus());
        if (request.getFromUnitId() == null || request.getToUnitId() == null
                || Objects.equals(request.getFromUnitId(), request.getToUnitId()))
            throw new BusinessException(400, "convert-units-invalid");
        if (request.getConvertRate() == null || request.getConvertRate().signum() <= 0)
            throw new BusinessException(400, "convert-rate-invalid");
        unique(unitConvertMapper.selectCount(new LambdaQueryWrapper<BaseProductUnitConvert>()
                .eq(BaseProductUnitConvert::getProductId, request.getProductId())
                .eq(BaseProductUnitConvert::getFromUnitId, request.getFromUnitId())
                .eq(BaseProductUnitConvert::getToUnitId, request.getToUnitId())
                .ne(id != null, BaseProductUnitConvert::getId, id)));
        assertProductEnabled(request.getProductId());
        requireEnabled(master(unitMapper, request.getFromUnitId()));
        requireEnabled(master(unitMapper, request.getToUnitId()));
        List<BaseProductUnitConvert> existing = new ArrayList<>(unitConvertMapper.selectList(
                currentEdges(new LambdaQueryWrapper<BaseProductUnitConvert>().eq(BaseProductUnitConvert::getProductId, request.getProductId()))));
        existing.removeIf(item -> Objects.equals(item.getId(), id) && id != null);
        if (directedPath(existing, request.getToUnitId(), request.getFromUnitId(), new HashSet<>()))
            throw new BusinessException(400, "convert-cycle");
        BigDecimal previous = ratesFrom(existing, request.getFromUnitId(), false).get(request.getToUnitId());
        if (previous != null && !sameRate(previous, request.getConvertRate()))
            throw new BusinessException(400, "convert-rate-inconsistent");
    }

    private boolean directedPath(List<BaseProductUnitConvert> edges, Long from, Long to, Set<Long> visited) {
        if (Objects.equals(from, to)) return true;
        if (!visited.add(from)) return false;
        for (BaseProductUnitConvert edge : edges) {
            if (Objects.equals(edge.getFromUnitId(), from) && directedPath(edges, edge.getToUnitId(), to, visited)) return true;
        }
        return false;
    }

    private boolean sameRate(BigDecimal left, BigDecimal right) {
        BigDecimal scale = left.abs().max(right.abs()).max(BigDecimal.ONE);
        return left.subtract(right).abs().compareTo(scale.multiply(new BigDecimal("0.00000000000000000001"))) <= 0;
    }

    /** Traverses both directions; stored directed edges remain acyclic. */
    private Map<Long, BigDecimal> ratesFrom(List<BaseProductUnitConvert> edges, Long source, boolean enabledOnly) {
        Map<Long, BigDecimal> rates = new LinkedHashMap<>();
        Map<Long, Boolean> enabledUnits = new HashMap<>();
        Deque<Long> queue = new ArrayDeque<>();
        rates.put(source, BigDecimal.ONE);
        queue.add(source);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            for (BaseProductUnitConvert edge : edges) {
                if (enabledOnly && (!Integer.valueOf(1).equals(edge.getStatus())
                        || !enabledUnits.computeIfAbsent(edge.getFromUnitId(), this::enabledUnit)
                        || !enabledUnits.computeIfAbsent(edge.getToUnitId(), this::enabledUnit))) continue;
                if (edge.getConvertRate() == null || edge.getConvertRate().signum() <= 0)
                    throw new BusinessException(400, "convert-rate-invalid");
                Long next;
                BigDecimal factor;
                if (Objects.equals(current, edge.getFromUnitId())) {
                    next = edge.getToUnitId(); factor = edge.getConvertRate();
                } else if (Objects.equals(current, edge.getToUnitId())) {
                    next = edge.getFromUnitId(); factor = BigDecimal.ONE.divide(edge.getConvertRate(), MathContext.DECIMAL128);
                } else continue;
                BigDecimal rate = rates.get(current).multiply(factor, MathContext.DECIMAL128);
                if (rates.containsKey(next)) {
                    if (!sameRate(rates.get(next), rate)) throw new BusinessException(400, "convert-rate-inconsistent");
                } else { rates.put(next, rate); queue.addLast(next); }
            }
        }
        return rates;
    }

    /** Multiplier from the selected business unit to the product's inventory base unit. */
    @Transactional
    public BigDecimal productUnitRate(Long productId, Long unitId) {
        assertProductEnabled(productId);
        requireEnabled(master(unitMapper, unitId));
        BaseProduct product = require(master(baseProductMapper, productId));
        if (Objects.equals(product.getBaseUnitId(), unitId)) return BigDecimal.ONE;
        List<BaseProductUnitConvert> edges = unitConvertMapper.selectList(new LambdaQueryWrapper<BaseProductUnitConvert>()
                .eq(BaseProductUnitConvert::getProductId, productId).eq(BaseProductUnitConvert::getStatus, 1)
                .last(inTransaction() ? "FOR UPDATE" : ""));
        BigDecimal rate = ratesFrom(edges, unitId, true).get(product.getBaseUnitId());
        if (rate == null) throw new BusinessException(400, "product-unit-unreachable");
        return rate.stripTrailingZeros();
    }

    public List<BasicOptionResponse> productUnitOptions(Long productId) {
        assertProductEnabled(productId);
        BaseProduct product = require(master(baseProductMapper, productId));
        List<BaseProductUnitConvert> edges = unitConvertMapper.selectList(new LambdaQueryWrapper<BaseProductUnitConvert>()
                .eq(BaseProductUnitConvert::getProductId, productId).eq(BaseProductUnitConvert::getStatus, 1)
                .last(inTransaction() ? "FOR UPDATE" : ""));
        Map<Long, BigDecimal> fromBase = ratesFrom(edges, product.getBaseUnitId(), true);
        List<BasicOptionResponse> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : fromBase.entrySet()) {
            BaseUnit unit = master(unitMapper, entry.getKey());
            if (unit == null || !Integer.valueOf(1).equals(unit.getStatus())) continue;
            BasicOptionResponse option = option(unit.getId(), unit.getUnitCode(), unit.getUnitName(), unit.getStatus());
            option.setProductId(productId);
            option.setBaseUnitId(product.getBaseUnitId());
            option.setConvertRate(BigDecimal.ONE.divide(entry.getValue(), MathContext.DECIMAL128).stripTrailingZeros());
            result.add(option);
        }
        return result;
    }

    private BaseProductUnitConvert enrichConversion(BaseProductUnitConvert item) {
        BaseProduct product = master(baseProductMapper, item.getProductId());
        BaseUnit from = master(unitMapper, item.getFromUnitId());
        BaseUnit to = master(unitMapper, item.getToUnitId());
        item.setProductName(product == null ? null : product.getProductName());
        item.setFromUnitName(from == null ? null : from.getUnitName());
        item.setToUnitName(to == null ? null : to.getUnitName());
        return item;
    }

    private BasicOptionResponse toConversionOption(BaseProductUnitConvert item) {
        enrichConversion(item);
        BasicOptionResponse option = option(item.getId(), String.valueOf(item.getProductId()),
                item.getProductName() + ": " + item.getFromUnitName() + " → " + item.getToUnitName(), item.getStatus());
        option.setProductId(item.getProductId()); option.setProductName(item.getProductName());
        option.setFromUnitId(item.getFromUnitId()); option.setFromUnitName(item.getFromUnitName());
        option.setToUnitId(item.getToUnitId()); option.setToUnitName(item.getToUnitName());
        option.setConvertRate(item.getConvertRate());
        return option;
    }

    private LambdaQueryWrapper<BaseProductUnitConvert> convertQuery(BasicQueryRequest request) {
        LambdaQueryWrapper<BaseProductUnitConvert> query = new LambdaQueryWrapper<BaseProductUnitConvert>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseProductUnitConvert::getId, request.getIds())
                .eq(request.getProductId() != null, BaseProductUnitConvert::getProductId, request.getProductId())
                .eq(request.getFromUnitId() != null, BaseProductUnitConvert::getFromUnitId, request.getFromUnitId())
                .eq(request.getToUnitId() != null, BaseProductUnitConvert::getToUnitId, request.getToUnitId())
                .eq(request.getStatus() != null, BaseProductUnitConvert::getStatus, request.getStatus());
        if (StringUtils.hasText(request.getKeyword())) {
            List<Long> productIds = baseProductMapper.selectList(new LambdaQueryWrapper<BaseProduct>()
                    .and(w -> w.like(BaseProduct::getProductName, request.getKeyword()).or().like(BaseProduct::getProductCode, request.getKeyword())))
                    .stream().map(BaseProduct::getId).toList();
            if (productIds.isEmpty()) query.eq(BaseProductUnitConvert::getId, -1L);
            else query.in(BaseProductUnitConvert::getProductId, productIds);
        }
        return query.orderByDesc(BaseProductUnitConvert::getId);
    }

    private LambdaQueryWrapper<BaseProduct> productQuery(ProductQueryRequest request) {
        return new LambdaQueryWrapper<BaseProduct>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseProduct::getId, request.getIds())
                .and(StringUtils.hasText(request.getKeyword()), w -> w.like(BaseProduct::getProductName, request.getKeyword())
                        .or().like(BaseProduct::getProductCode, request.getKeyword()).or().like(BaseProduct::getBarcode, request.getKeyword()))
                .like(StringUtils.hasText(request.getProductCode()), BaseProduct::getProductCode, request.getProductCode())
                .like(StringUtils.hasText(request.getProductName()), BaseProduct::getProductName, request.getProductName())
                .eq(request.getCategoryId() != null, BaseProduct::getCategoryId, request.getCategoryId())
                .eq(request.getStatus() != null, BaseProduct::getStatus, request.getStatus()).orderByDesc(BaseProduct::getId);
    }

    private LambdaQueryWrapper<BaseProductCategory> categoryQuery(BasicQueryRequest request) {
        return new LambdaQueryWrapper<BaseProductCategory>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseProductCategory::getId, request.getIds())
                .and(StringUtils.hasText(request.getKeyword()), w -> w.like(BaseProductCategory::getCategoryName, request.getKeyword()))
                .like(StringUtils.hasText(request.getName()), BaseProductCategory::getCategoryName, request.getName())
                .eq(request.getStatus() != null, BaseProductCategory::getStatus, request.getStatus())
                .orderByAsc(BaseProductCategory::getSortNo).orderByAsc(BaseProductCategory::getId);
    }

    private LambdaQueryWrapper<BaseUnit> unitQuery(BasicQueryRequest request) {
        return new LambdaQueryWrapper<BaseUnit>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseUnit::getId, request.getIds())
                .and(StringUtils.hasText(request.getKeyword()), w -> w.like(BaseUnit::getUnitName, request.getKeyword()).or().like(BaseUnit::getUnitCode, request.getKeyword()))
                .like(StringUtils.hasText(request.getName()), BaseUnit::getUnitName, request.getName())
                .like(StringUtils.hasText(request.getCode()), BaseUnit::getUnitCode, request.getCode())
                .eq(request.getStatus() != null, BaseUnit::getStatus, request.getStatus()).orderByDesc(BaseUnit::getId);
    }

    private LambdaQueryWrapper<BaseWarehouse> warehouseQuery(BasicQueryRequest request) {
        return new LambdaQueryWrapper<BaseWarehouse>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseWarehouse::getId, request.getIds())
                .and(StringUtils.hasText(request.getKeyword()), w -> w.like(BaseWarehouse::getWarehouseName, request.getKeyword()).or().like(BaseWarehouse::getWarehouseCode, request.getKeyword()))
                .like(StringUtils.hasText(request.getName()), BaseWarehouse::getWarehouseName, request.getName())
                .like(StringUtils.hasText(request.getCode()), BaseWarehouse::getWarehouseCode, request.getCode())
                .eq(request.getStatus() != null, BaseWarehouse::getStatus, request.getStatus()).orderByDesc(BaseWarehouse::getId);
    }

    private LambdaQueryWrapper<BaseSupplier> supplierQuery(BasicQueryRequest request) {
        return new LambdaQueryWrapper<BaseSupplier>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseSupplier::getId, request.getIds())
                .and(StringUtils.hasText(request.getKeyword()), w -> w.like(BaseSupplier::getSupplierName, request.getKeyword()).or().like(BaseSupplier::getSupplierCode, request.getKeyword())
                        .or().like(BaseSupplier::getContactName, request.getKeyword()).or().like(BaseSupplier::getContactPhone, request.getKeyword()))
                .like(StringUtils.hasText(request.getName()), BaseSupplier::getSupplierName, request.getName())
                .like(StringUtils.hasText(request.getCode()), BaseSupplier::getSupplierCode, request.getCode())
                .eq(request.getStatus() != null, BaseSupplier::getStatus, request.getStatus()).orderByDesc(BaseSupplier::getId);
    }

    private LambdaQueryWrapper<BaseCustomer> customerQuery(BasicQueryRequest request) {
        return new LambdaQueryWrapper<BaseCustomer>()
                .in(request.getIds() != null && !request.getIds().isEmpty(), BaseCustomer::getId, request.getIds())
                .and(StringUtils.hasText(request.getKeyword()), w -> w.like(BaseCustomer::getCustomerName, request.getKeyword()).or().like(BaseCustomer::getCustomerCode, request.getKeyword())
                        .or().like(BaseCustomer::getContactName, request.getKeyword()).or().like(BaseCustomer::getContactPhone, request.getKeyword()))
                .like(StringUtils.hasText(request.getName()), BaseCustomer::getCustomerName, request.getName())
                .like(StringUtils.hasText(request.getCode()), BaseCustomer::getCustomerCode, request.getCode())
                .eq(request.getStatus() != null, BaseCustomer::getStatus, request.getStatus()).orderByDesc(BaseCustomer::getId);
    }

    /** All matching records, deliberately independent of request pagination. */
    public List<Map<String, Object>> exportProducts(ProductQueryRequest request) {
        List<Map<String, Object>> rows = exportRows(baseProductMapper.selectList(productQuery(request)).stream().map(this::toProductResponse).toList(),
                "商品编码", "productCode", "商品名称", "productName", "分类", "categoryName", "品牌", "brandName", "规格", "spec",
                "条码", "barcode", "基础单位", "baseUnitName", "保质期天数", "shelfLifeDays", "状态", "status", "备注", "remark",
                "创建时间", "createdTime", "更新时间", "updatedTime");
        verifyExportSelection(request.getIds(), rows.size());
        return rows;
    }

    public List<Map<String, Object>> exportBasic(String resource, BasicQueryRequest request) {
        List<Map<String, Object>> rows = switch (resource) {
            case "categories" -> exportRows(categoryMapper.selectList(categoryQuery(request)),
                    "分类名称", "categoryName", "父分类", "parentId", "排序", "sortNo", "状态", "status", "备注", "remark");
            case "units" -> exportRows(unitMapper.selectList(unitQuery(request)),
                    "单位编码", "unitCode", "单位名称", "unitName", "状态", "status", "备注", "remark");
            case "warehouses" -> exportRows(warehouseMapper.selectList(warehouseQuery(request)),
                    "仓库编码", "warehouseCode", "仓库名称", "warehouseName", "联系人", "contactName", "联系电话", "contactPhone",
                    "地址", "address", "状态", "status", "备注", "remark");
            case "suppliers" -> exportRows(supplierMapper.selectList(supplierQuery(request)),
                    "供应商编码", "supplierCode", "供应商名称", "supplierName", "联系人", "contactName", "联系电话", "contactPhone",
                    "地址", "address", "税号", "taxNo", "开户行", "bankName", "银行账号", "bankAccount", "状态", "status", "备注", "remark");
            case "customers" -> exportRows(customerMapper.selectList(customerQuery(request)),
                    "客户编码", "customerCode", "客户名称", "customerName", "联系人", "contactName", "联系电话", "contactPhone",
                    "地址", "address", "税号", "taxNo", "开户行", "bankName", "银行账号", "bankAccount", "状态", "status", "备注", "remark");
            case "unit-converts" -> exportRows(unitConvertMapper.selectList(convertQuery(request)).stream().map(this::enrichConversion).toList(),
                    "商品名称", "productName", "源单位", "fromUnitName", "目标单位", "toUnitName", "换算比例", "convertRate", "状态", "status", "备注", "remark");
            default -> throw new BusinessException(400, "basic-resource-invalid");
        };
        verifyExportSelection(request.getIds(), rows.size());
        return rows;
    }

    private void verifyExportSelection(List<Long> ids, int rows) {
        // Never return a silently truncated selected export when a record was removed meanwhile.
        if (ids != null && new HashSet<>(ids).size() != rows)
            throw new BusinessException(400, "basic-export-selection-stale");
    }

    private List<Map<String, Object>> exportRows(List<?> entities, String... columns) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object entity : entities) {
            org.springframework.beans.BeanWrapper bean = new org.springframework.beans.BeanWrapperImpl(entity);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < columns.length; i += 2) {
                Object value = bean.getPropertyValue(columns[i + 1]);
                if ("status".equals(columns[i + 1])) value = Integer.valueOf(1).equals(value) ? "启用" : "停用";
                if (entity instanceof BaseProductCategory && "parentId".equals(columns[i + 1])) {
                    BaseProductCategory parent = Long.valueOf(0).equals(value) ? null : categoryMapper.selectById((Long) value);
                    value = parent == null ? "根分类" : parent.getCategoryName();
                }
                row.put(columns[i], value);
            }
            rows.add(row);
        }
        return rows;
    }

}
