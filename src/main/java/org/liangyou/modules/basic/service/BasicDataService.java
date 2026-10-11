package org.liangyou.modules.basic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.util.StringUtils;

import java.util.List;

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
        LambdaQueryWrapper<BaseProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getKeyword()), BaseProduct::getProductName, request.getKeyword())
                .like(StringUtils.hasText(request.getProductCode()), BaseProduct::getProductCode, request.getProductCode())
                .like(StringUtils.hasText(request.getProductName()), BaseProduct::getProductName, request.getProductName())
                .eq(request.getCategoryId() != null, BaseProduct::getCategoryId, request.getCategoryId())
                .eq(request.getStatus() != null, BaseProduct::getStatus, request.getStatus())
                .orderByDesc(BaseProduct::getId);
        Page<BaseProduct> result = baseProductMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toProductResponse).toList(), result.getTotal());
    }

    public ProductResponse productDetail(Long id) {
        BaseProduct product = baseProductMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        return toProductResponse(product);
    }

    public ProductResponse createProduct(ProductSaveRequest request) {
        BaseProduct product = new BaseProduct();
        applyProductRequest(product, request);
        product.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        baseProductMapper.insert(product);
        return toProductResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductSaveRequest request) {
        BaseProduct product = baseProductMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        applyProductRequest(product, request);
        product.setStatus(request.getStatus() == null ? product.getStatus() : request.getStatus());
        baseProductMapper.updateById(product);
        return toProductResponse(product);
    }

    public void updateProductStatus(Long id, StatusUpdateRequest request) {
        BaseProduct product = baseProductMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(400, "status-invalid");
        }
        product.setStatus(request.getStatus());
        baseProductMapper.updateById(product);
    }

    public List<BasicOptionResponse> productOptions(String keyword) {
        LambdaQueryWrapper<BaseProduct> wrapper = new LambdaQueryWrapper<BaseProduct>()
                .eq(BaseProduct::getStatus, 1)
                .like(StringUtils.hasText(keyword), BaseProduct::getProductName, keyword)
                .orderByAsc(BaseProduct::getProductCode);
        return baseProductMapper.selectList(wrapper).stream().map(this::toProductOption).toList();
    }

    public void deleteProduct(Long id) {
        BaseProduct product = baseProductMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        if (hasProductReference(id)) {
            throw new BusinessException(400, "basic-data-in-use");
        }
        baseProductMapper.deleteById(id);
    }

    public PageResponse<BaseProductCategory> queryCategories(BasicQueryRequest request) {
        Page<BaseProductCategory> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<BaseProductCategory> result = categoryMapper.selectPage(page, new LambdaQueryWrapper<BaseProductCategory>()
                .like(StringUtils.hasText(request.getKeyword()), BaseProductCategory::getCategoryName, request.getKeyword())
                .eq(request.getStatus() != null, BaseProductCategory::getStatus, request.getStatus())
                .orderByAsc(BaseProductCategory::getSortNo));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseProductCategory categoryDetail(Long id) {
        return require(categoryMapper.selectById(id));
    }

    public BaseProductCategory createCategory(CategorySaveRequest request) {
        BaseProductCategory entity = new BaseProductCategory();
        entity.setCategoryName(request.getCategoryName());
        entity.setParentId(request.getParentId());
        entity.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
        categoryMapper.insert(entity);
        return entity;
    }

    public BaseProductCategory updateCategory(Long id, CategorySaveRequest request) {
        BaseProductCategory entity = categoryDetail(id);
        entity.setCategoryName(request.getCategoryName());
        entity.setParentId(request.getParentId());
        entity.setSortNo(request.getSortNo() == null ? entity.getSortNo() : request.getSortNo());
        entity.setStatus(request.getStatus() == null ? entity.getStatus() : request.getStatus());
        entity.setRemark(request.getRemark());
        categoryMapper.updateById(entity);
        return entity;
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    public void updateCategoryStatus(Long id, StatusUpdateRequest request) {
        BaseProductCategory entity = categoryDetail(id);
        entity.setStatus(validStatus(request));
        categoryMapper.updateById(entity);
    }

    public List<BasicOptionResponse> categoryOptions(String keyword) {
        return categoryMapper.selectList(new LambdaQueryWrapper<BaseProductCategory>()
                        .eq(BaseProductCategory::getStatus, 1)
                        .like(StringUtils.hasText(keyword), BaseProductCategory::getCategoryName, keyword))
                .stream().map(item -> option(item.getId(), null, item.getCategoryName(), item.getStatus())).toList();
    }

    public PageResponse<BaseUnit> queryUnits(BasicQueryRequest request) {
        Page<BaseUnit> result = unitMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<BaseUnit>()
                        .like(StringUtils.hasText(request.getKeyword()), BaseUnit::getUnitName, request.getKeyword())
                        .eq(request.getStatus() != null, BaseUnit::getStatus, request.getStatus()));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseUnit unitDetail(Long id) {
        return require(unitMapper.selectById(id));
    }

    public BaseUnit createUnit(UnitSaveRequest request) {
        BaseUnit entity = new BaseUnit();
        entity.setUnitCode(request.getUnitCode());
        entity.setUnitName(request.getUnitName());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
        unitMapper.insert(entity);
        return entity;
    }

    public BaseUnit updateUnit(Long id, UnitSaveRequest request) {
        BaseUnit entity = unitDetail(id);
        entity.setUnitCode(request.getUnitCode());
        entity.setUnitName(request.getUnitName());
        entity.setStatus(request.getStatus() == null ? entity.getStatus() : request.getStatus());
        entity.setRemark(request.getRemark());
        unitMapper.updateById(entity);
        return entity;
    }

    public void deleteUnit(Long id) {
        unitMapper.deleteById(id);
    }

    public void updateUnitStatus(Long id, StatusUpdateRequest request) {
        BaseUnit entity = unitDetail(id);
        entity.setStatus(validStatus(request));
        unitMapper.updateById(entity);
    }

    public List<BasicOptionResponse> unitOptions(String keyword) {
        return unitMapper.selectList(new LambdaQueryWrapper<BaseUnit>()
                        .eq(BaseUnit::getStatus, 1)
                        .like(StringUtils.hasText(keyword), BaseUnit::getUnitName, keyword))
                .stream().map(item -> option(item.getId(), item.getUnitCode(), item.getUnitName(), item.getStatus())).toList();
    }

    public PageResponse<BaseWarehouse> queryWarehouses(BasicQueryRequest request) {
        Page<BaseWarehouse> result = warehouseMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<BaseWarehouse>()
                        .like(StringUtils.hasText(request.getKeyword()), BaseWarehouse::getWarehouseName, request.getKeyword())
                        .eq(request.getStatus() != null, BaseWarehouse::getStatus, request.getStatus()));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseWarehouse warehouseDetail(Long id) {
        return require(warehouseMapper.selectById(id));
    }

    public BaseWarehouse createWarehouse(WarehouseSaveRequest request) {
        BaseWarehouse entity = new BaseWarehouse();
        applyWarehouse(entity, request);
        warehouseMapper.insert(entity);
        return entity;
    }

    public BaseWarehouse updateWarehouse(Long id, WarehouseSaveRequest request) {
        BaseWarehouse entity = warehouseDetail(id);
        applyWarehouse(entity, request);
        warehouseMapper.updateById(entity);
        return entity;
    }

    public void deleteWarehouse(Long id) {
        if (hasWarehouseReference(id)) {
            throw new BusinessException(400, "basic-data-in-use");
        }
        warehouseMapper.deleteById(id);
    }

    public void updateWarehouseStatus(Long id, StatusUpdateRequest request) {
        BaseWarehouse entity = warehouseDetail(id);
        entity.setStatus(validStatus(request));
        warehouseMapper.updateById(entity);
    }

    public List<BasicOptionResponse> warehouseOptions(String keyword) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<BaseWarehouse>()
                        .eq(BaseWarehouse::getStatus, 1)
                        .like(StringUtils.hasText(keyword), BaseWarehouse::getWarehouseName, keyword))
                .stream().map(item -> option(item.getId(), item.getWarehouseCode(), item.getWarehouseName(), item.getStatus())).toList();
    }

    public PageResponse<BaseSupplier> querySuppliers(BasicQueryRequest request) {
        Page<BaseSupplier> result = supplierMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<BaseSupplier>()
                        .like(StringUtils.hasText(request.getKeyword()), BaseSupplier::getSupplierName, request.getKeyword())
                        .eq(request.getStatus() != null, BaseSupplier::getStatus, request.getStatus()));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseSupplier supplierDetail(Long id) {
        return require(supplierMapper.selectById(id));
    }

    public BaseSupplier createSupplier(SupplierSaveRequest request) {
        BaseSupplier entity = new BaseSupplier();
        applySupplier(entity, request);
        supplierMapper.insert(entity);
        return entity;
    }

    public BaseSupplier updateSupplier(Long id, SupplierSaveRequest request) {
        BaseSupplier entity = supplierDetail(id);
        applySupplier(entity, request);
        supplierMapper.updateById(entity);
        return entity;
    }

    public void deleteSupplier(Long id) {
        supplierMapper.deleteById(id);
    }

    public void updateSupplierStatus(Long id, StatusUpdateRequest request) {
        BaseSupplier entity = supplierDetail(id);
        entity.setStatus(validStatus(request));
        supplierMapper.updateById(entity);
    }

    public List<BasicOptionResponse> supplierOptions(String keyword) {
        return supplierMapper.selectList(new LambdaQueryWrapper<BaseSupplier>()
                        .eq(BaseSupplier::getStatus, 1)
                        .like(StringUtils.hasText(keyword), BaseSupplier::getSupplierName, keyword))
                .stream().map(item -> option(item.getId(), item.getSupplierCode(), item.getSupplierName(), item.getStatus())).toList();
    }

    public PageResponse<BaseCustomer> queryCustomers(BasicQueryRequest request) {
        Page<BaseCustomer> result = customerMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<BaseCustomer>()
                        .like(StringUtils.hasText(request.getKeyword()), BaseCustomer::getCustomerName, request.getKeyword())
                        .eq(request.getStatus() != null, BaseCustomer::getStatus, request.getStatus()));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseCustomer customerDetail(Long id) {
        return require(customerMapper.selectById(id));
    }

    public BaseCustomer createCustomer(CustomerSaveRequest request) {
        BaseCustomer entity = new BaseCustomer();
        applyCustomer(entity, request);
        customerMapper.insert(entity);
        return entity;
    }

    public BaseCustomer updateCustomer(Long id, CustomerSaveRequest request) {
        BaseCustomer entity = customerDetail(id);
        applyCustomer(entity, request);
        customerMapper.updateById(entity);
        return entity;
    }

    public void deleteCustomer(Long id) {
        customerMapper.deleteById(id);
    }

    public void updateCustomerStatus(Long id, StatusUpdateRequest request) {
        BaseCustomer entity = customerDetail(id);
        entity.setStatus(validStatus(request));
        customerMapper.updateById(entity);
    }

    public List<BasicOptionResponse> customerOptions(String keyword) {
        return customerMapper.selectList(new LambdaQueryWrapper<BaseCustomer>()
                        .eq(BaseCustomer::getStatus, 1)
                        .like(StringUtils.hasText(keyword), BaseCustomer::getCustomerName, keyword))
                .stream().map(item -> option(item.getId(), item.getCustomerCode(), item.getCustomerName(), item.getStatus())).toList();
    }

    public PageResponse<BaseProductUnitConvert> queryUnitConverts(BasicQueryRequest request) {
        Page<BaseProductUnitConvert> result = unitConvertMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<BaseProductUnitConvert>()
                        .eq(request.getStatus() != null, BaseProductUnitConvert::getStatus, request.getStatus()));
        return new PageResponse<>(result.getRecords(), result.getTotal());
    }

    public BaseProductUnitConvert unitConvertDetail(Long id) {
        return require(unitConvertMapper.selectById(id));
    }

    public BaseProductUnitConvert createUnitConvert(UnitConvertSaveRequest request) {
        BaseProductUnitConvert entity = new BaseProductUnitConvert();
        applyUnitConvert(entity, request);
        unitConvertMapper.insert(entity);
        return entity;
    }

    public BaseProductUnitConvert updateUnitConvert(Long id, UnitConvertSaveRequest request) {
        BaseProductUnitConvert entity = unitConvertDetail(id);
        applyUnitConvert(entity, request);
        unitConvertMapper.updateById(entity);
        return entity;
    }

    public void deleteUnitConvert(Long id) {
        unitConvertMapper.deleteById(id);
    }

    public void updateUnitConvertStatus(Long id, StatusUpdateRequest request) {
        BaseProductUnitConvert entity = unitConvertDetail(id);
        entity.setStatus(validStatus(request));
        unitConvertMapper.updateById(entity);
    }

    public List<BasicOptionResponse> unitConvertOptions(String keyword) {
        return unitConvertMapper.selectList(new LambdaQueryWrapper<BaseProductUnitConvert>()
                        .eq(BaseProductUnitConvert::getStatus, 1))
                .stream().map(item -> option(item.getId(), String.valueOf(item.getProductId()),
                        item.getFromUnitId() + "->" + item.getToUnitId(), item.getStatus())).toList();
    }

    public void assertWarehouseEnabled(Long warehouseId) {
        if (warehouseId == null) {
            return;
        }
        Long count = warehouseMapper.selectCount(new LambdaQueryWrapper<>(
        ));
        if (count == null || count == 0) {
            throw new BusinessException(400, "basic-data-disabled");
        }
    }

    private boolean hasProductReference(Long productId) {
        return countPurchaseItems(productId) > 0
                || countSaleItems(productId) > 0
                || invStockMapper.selectCount(new LambdaQueryWrapper<InvStock>().eq(InvStock::getProductId, productId)) > 0
                || invBatchMapper.selectCount(new LambdaQueryWrapper<InvBatch>().eq(InvBatch::getProductId, productId)) > 0
                || invStockFlowMapper.selectCount(new LambdaQueryWrapper<InvStockFlow>().eq(InvStockFlow::getProductId, productId)) > 0;
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
                || saleOutMapper.selectCount(new LambdaQueryWrapper<BizSaleOut>().eq(BizSaleOut::getWarehouseId, warehouseId)) > 0;
    }

    private int validStatus(StatusUpdateRequest request) {
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(400, "status-invalid");
        }
        return request.getStatus();
    }

    private <T> T require(T entity) {
        if (entity == null) {
            throw new BusinessException(404, "basic-data-not-found");
        }
        return entity;
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
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
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
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
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
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
    }

    private void applyUnitConvert(BaseProductUnitConvert entity, UnitConvertSaveRequest request) {
        entity.setProductId(request.getProductId());
        entity.setFromUnitId(request.getFromUnitId());
        entity.setToUnitId(request.getToUnitId());
        entity.setConvertRate(request.getConvertRate());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
    }
}
