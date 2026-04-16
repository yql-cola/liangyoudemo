CREATE DATABASE IF NOT EXISTS grain_oil_ims DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE grain_oil_ims;

CREATE TABLE sys_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
    warehouse_code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    warehouse_name VARCHAR(128) NOT NULL COMMENT '仓库名称',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_warehouse_code (warehouse_code)
) COMMENT='仓库表';

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    warehouse_id BIGINT DEFAULT NULL COMMENT '所属仓库ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
    is_super_admin TINYINT NOT NULL DEFAULT 0 COMMENT '是否超级管理员',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username),
    KEY idx_user_warehouse (warehouse_id)
) COMMENT='用户表';

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code)
) COMMENT='角色表';

CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    permission_type TINYINT NOT NULL COMMENT '类型:1菜单,2按钮,3接口',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    path VARCHAR(255) DEFAULT NULL COMMENT '前端路由/资源路径',
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_permission_code (permission_code)
) COMMENT='权限表';

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) COMMENT='用户角色关联表';

CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) COMMENT='角色权限关联表';

CREATE TABLE sys_user_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    grant_type TINYINT NOT NULL COMMENT '授权类型:1直接授予,0直接撤销',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_permission (user_id, permission_id),
    KEY idx_user_permission_permission (permission_id)
) COMMENT='用户直接权限关联表';

CREATE TABLE base_product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_parent_id (parent_id)
) COMMENT='商品分类表';

CREATE TABLE base_unit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '单位ID',
    unit_code VARCHAR(32) NOT NULL COMMENT '单位编码',
    unit_name VARCHAR(32) NOT NULL COMMENT '单位名称',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_unit_code (unit_code)
) COMMENT='单位表';

CREATE TABLE base_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    brand_name VARCHAR(64) DEFAULT NULL COMMENT '品牌',
    spec VARCHAR(128) DEFAULT NULL COMMENT '规格',
    barcode VARCHAR(64) DEFAULT NULL COMMENT '条码',
    image_url VARCHAR(255) DEFAULT NULL COMMENT '图片地址',
    base_unit_id BIGINT NOT NULL COMMENT '基础单位ID',
    shelf_life_days INT DEFAULT NULL COMMENT '保质期天数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_product_name_category (product_name, category_id),
    KEY idx_category_id (category_id)
) COMMENT='商品表';

CREATE TABLE base_product_unit_convert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '单位换算ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    from_unit_id BIGINT NOT NULL COMMENT '源单位ID',
    to_unit_id BIGINT NOT NULL COMMENT '目标单位ID',
    convert_rate DECIMAL(18,6) NOT NULL COMMENT '换算比率',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_unit_convert (product_id, from_unit_id, to_unit_id)
) COMMENT='商品单位换算表';

CREATE TABLE base_supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    supplier_code VARCHAR(64) NOT NULL COMMENT '供应商编码',
    supplier_name VARCHAR(128) NOT NULL COMMENT '供应商名称',
    contact_name VARCHAR(64) DEFAULT NULL,
    contact_phone VARCHAR(32) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    tax_no VARCHAR(64) DEFAULT NULL COMMENT '税号',
    bank_name VARCHAR(128) DEFAULT NULL,
    bank_account VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_supplier_code (supplier_code),
    KEY idx_supplier_name (supplier_name)
) COMMENT='供应商表';

CREATE TABLE base_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    customer_code VARCHAR(64) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    contact_name VARCHAR(64) DEFAULT NULL,
    contact_phone VARCHAR(32) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    tax_no VARCHAR(64) DEFAULT NULL COMMENT '税号',
    bank_name VARCHAR(128) DEFAULT NULL,
    bank_account VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_customer_code (customer_code),
    KEY idx_customer_name (customer_name)
) COMMENT='客户表';

CREATE TABLE biz_purchase_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购入库单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '入库单号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    biz_date DATE NOT NULL COMMENT '业务日期',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_purchase_in_order_no (order_no),
    KEY idx_purchase_supplier_date (supplier_id, biz_date),
    KEY idx_purchase_status_date (status, biz_date),
    KEY idx_purchase_warehouse (warehouse_id)
) COMMENT='采购入库单主表';

CREATE TABLE biz_purchase_in_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购入库明细ID',
    purchase_in_id BIGINT NOT NULL COMMENT '采购入库单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    unit_id BIGINT NOT NULL COMMENT '单位ID',
    qty DECIMAL(18,4) NOT NULL COMMENT '数量',
    unit_price DECIMAL(18,4) NOT NULL COMMENT '单价',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    expiry_date DATE DEFAULT NULL COMMENT '过期日期',
    inbound_time DATETIME NOT NULL COMMENT '入库时间',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_purchase_in_id (purchase_in_id),
    KEY idx_purchase_item_product (product_id),
    KEY idx_purchase_item_warehouse (warehouse_id)
) COMMENT='采购入库单明细表';

CREATE TABLE biz_sale_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售出库单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '出库单号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    biz_date DATE NOT NULL COMMENT '业务日期',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sale_out_order_no (order_no),
    KEY idx_sale_customer_date (customer_id, biz_date),
    KEY idx_sale_status_date (status, biz_date),
    KEY idx_sale_warehouse (warehouse_id)
) COMMENT='销售出库单主表';

CREATE TABLE biz_sale_out_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售出库明细ID',
    sale_out_id BIGINT NOT NULL COMMENT '销售出库单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    unit_id BIGINT NOT NULL COMMENT '单位ID',
    qty DECIMAL(18,4) NOT NULL COMMENT '数量',
    unit_price DECIMAL(18,4) NOT NULL COMMENT '销售单价',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sale_out_id (sale_out_id),
    KEY idx_sale_item_product (product_id),
    KEY idx_sale_item_warehouse (warehouse_id)
) COMMENT='销售出库单明细表';

CREATE TABLE inv_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存汇总ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    total_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总库存数量',
    locked_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '锁定数量',
    available_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '可用数量',
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_stock_warehouse_product (warehouse_id, product_id)
) COMMENT='商品库存汇总表';

CREATE TABLE inv_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存批次ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型:purchase_in/manual_adjust等',
    source_id BIGINT NOT NULL COMMENT '来源明细ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    inbound_time DATETIME NOT NULL COMMENT '入库时间',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    expiry_date DATE DEFAULT NULL COMMENT '过期日期',
    unit_price DECIMAL(18,4) NOT NULL COMMENT '进货单价',
    init_qty DECIMAL(18,4) NOT NULL COMMENT '初始入库数量',
    remain_qty DECIMAL(18,4) NOT NULL COMMENT '剩余数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_batch_product_warehouse_batch (product_id, warehouse_id, batch_no),
    KEY idx_batch_fifo (product_id, warehouse_id, remain_qty, inbound_time),
    KEY idx_batch_source (source_type, source_id)
) COMMENT='库存批次表';

CREATE TABLE inv_stock_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存流水ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    flow_type VARCHAR(32) NOT NULL COMMENT '流水类型:purchase_in/sale_out/adjust',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    biz_id BIGINT NOT NULL COMMENT '业务单据ID',
    biz_item_id BIGINT DEFAULT NULL COMMENT '业务明细ID',
    change_qty DECIMAL(18,4) NOT NULL COMMENT '变动数量,入正出负',
    after_qty DECIMAL(18,4) NOT NULL COMMENT '变动后汇总库存',
    biz_time DATETIME NOT NULL COMMENT '业务时间',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_stock_flow_product_time (product_id, biz_time),
    KEY idx_stock_flow_ref (biz_type, biz_id),
    KEY idx_stock_flow_batch (batch_id)
) COMMENT='库存流水表';

CREATE TABLE inv_sale_batch_deduction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售批次扣减ID',
    sale_out_id BIGINT NOT NULL COMMENT '销售出库单ID',
    sale_out_item_id BIGINT NOT NULL COMMENT '销售出库明细ID',
    batch_id BIGINT NOT NULL COMMENT '扣减批次ID',
    deduct_qty DECIMAL(18,4) NOT NULL COMMENT '扣减数量',
    unit_cost DECIMAL(18,4) NOT NULL COMMENT '成本单价',
    amount DECIMAL(18,2) NOT NULL COMMENT '成本金额',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_deduction_sale_item (sale_out_item_id),
    KEY idx_deduction_batch (batch_id),
    KEY idx_deduction_sale_out (sale_out_id)
) COMMENT='销售出库批次扣减明细表';

CREATE TABLE fin_invoice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '发票ID',
    invoice_no VARCHAR(64) NOT NULL COMMENT '发票号',
    invoice_type VARCHAR(32) NOT NULL COMMENT '发票类型:purchase/sale',
    invoice_date DATE NOT NULL COMMENT '开票日期',
    invoice_amount DECIMAL(18,2) NOT NULL COMMENT '发票金额',
    partner_type VARCHAR(32) NOT NULL COMMENT '往来对象类型:supplier/customer',
    partner_id BIGINT NOT NULL COMMENT '往来对象ID',
    tax_amount DECIMAL(18,2) DEFAULT 0 COMMENT '税额',
    remark VARCHAR(255) DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_invoice_no (invoice_no),
    KEY idx_invoice_type_date (invoice_type, invoice_date)
) COMMENT='发票主表';

CREATE TABLE fin_invoice_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '发票关联ID',
    invoice_id BIGINT NOT NULL COMMENT '发票ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型:purchase_in/sale_out',
    biz_id BIGINT NOT NULL COMMENT '业务单据ID',
    relation_amount DECIMAL(18,2) NOT NULL COMMENT '关联金额',
    created_by BIGINT DEFAULT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_invoice_relation_invoice (invoice_id),
    KEY idx_invoice_relation_biz (biz_type, biz_id)
) COMMENT='发票关联表';

CREATE TABLE stat_daily_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日统计ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    stat_type VARCHAR(32) NOT NULL COMMENT '统计类型:purchase/sale/invoice',
    product_category_id BIGINT DEFAULT NULL COMMENT '商品分类ID',
    customer_id BIGINT DEFAULT NULL COMMENT '客户ID',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商ID',
    total_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_daily_summary (stat_date, warehouse_id, stat_type, product_category_id, customer_id, supplier_id),
    KEY idx_daily_type_date (stat_type, stat_date),
    KEY idx_daily_category_date (product_category_id, stat_date),
    KEY idx_daily_customer_date (customer_id, stat_date),
    KEY idx_daily_supplier_date (supplier_id, stat_date)
) COMMENT='日统计汇总表';

CREATE TABLE stat_monthly_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '月统计ID',
    stat_month CHAR(7) NOT NULL COMMENT '统计月份:YYYY-MM',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    stat_type VARCHAR(32) NOT NULL COMMENT '统计类型:purchase/sale/invoice',
    product_category_id BIGINT DEFAULT NULL COMMENT '商品分类ID',
    customer_id BIGINT DEFAULT NULL COMMENT '客户ID',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商ID',
    total_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_monthly_summary (stat_month, warehouse_id, stat_type, product_category_id, customer_id, supplier_id),
    KEY idx_monthly_type_month (stat_type, stat_month),
    KEY idx_monthly_category_month (product_category_id, stat_month),
    KEY idx_monthly_customer_month (customer_id, stat_month),
    KEY idx_monthly_supplier_month (supplier_id, stat_month)
) COMMENT='月统计汇总表';
