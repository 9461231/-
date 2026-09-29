-- =====================================================
-- 中小型超市运营管理系统 —— 数据库初始化脚本
-- Step 01：商品信息管理（product_category / product）
-- 注意：商品表不维护 current_stock，库存在 Step 04 库存模块维护
-- =====================================================

CREATE DATABASE IF NOT EXISTS supermarket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE supermarket;

-- -----------------------------------------------------
-- 商品分类表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS product_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE = InnoDB COMMENT = '商品分类表';

-- -----------------------------------------------------
-- 商品主数据表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS product (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    category_id    BIGINT        NOT NULL COMMENT '分类ID',
    sku            VARCHAR(64)   NOT NULL COMMENT 'SKU 编码',
    barcode        VARCHAR(64)   NULL COMMENT '商品条码',
    name           VARCHAR(100)  NOT NULL COMMENT '商品名称',
    brand          VARCHAR(64)   NULL COMMENT '品牌',
    spec           VARCHAR(64)   NULL COMMENT '规格',
    unit           VARCHAR(16)   NOT NULL COMMENT '单位（个/瓶/袋/箱等）',
    purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '采购价',
    sale_price     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '销售价',
    min_stock      INT           NOT NULL DEFAULT 0 COMMENT '最低库存（预警下限）',
    max_stock      INT           NULL COMMENT '最高库存（预警上限）',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1-在售 0-停售',
    remark         VARCHAR(255)  NULL COMMENT '备注',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku (sku),
    UNIQUE KEY uk_product_barcode (barcode),
    KEY idx_product_name (name),
    KEY idx_product_category (category_id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (id)
) ENGINE = InnoDB COMMENT = '商品主数据表';
