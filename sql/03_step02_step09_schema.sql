-- =====================================================
-- Step 02 ~ Step 09 数据表结构（文档用途）
-- 说明：后端 JPA ddl-auto=update 会自动建表，本脚本用于
--       数据库手工初始化 / 毕业设计文档归档。
-- =====================================================

USE supermarket;

-- ============ Step 02 供应商 ============
CREATE TABLE IF NOT EXISTS supplier (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
    name           VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50)  NULL COMMENT '联系人',
    phone          VARCHAR(32)  NULL COMMENT '联系方式',
    email          VARCHAR(100) NULL COMMENT '邮箱',
    address        VARCHAR(200) NULL COMMENT '地址',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1-合作中 0-已停止合作',
    remark         VARCHAR(255) NULL COMMENT '备注',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_name (name)
) ENGINE = InnoDB COMMENT = '供应商表';

CREATE TABLE IF NOT EXISTS supplier_product (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    supplier_id  BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    supply_price DECIMAL(10,2) NOT NULL COMMENT '供货价',
    is_primary   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否主供货商',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_product (supplier_id, product_id),
    KEY idx_sp_product (product_id)
) ENGINE = InnoDB COMMENT = '供应商-商品合作关系';

-- ============ Step 03 采购 ============
CREATE TABLE IF NOT EXISTS purchase_order (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_no     VARCHAR(64)   NOT NULL COMMENT '订单号',
    supplier_id  BIGINT        NOT NULL,
    status       TINYINT       NOT NULL DEFAULT 0 COMMENT '0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    remark       VARCHAR(255)  NULL,
    audit_remark VARCHAR(255)  NULL,
    audit_at     DATETIME      NULL,
    completed_at DATETIME      NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_po_no (order_no)
) ENGINE = InnoDB COMMENT = '采购订单';

CREATE TABLE IF NOT EXISTS purchase_order_item (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    order_id          BIGINT        NOT NULL,
    product_id        BIGINT        NOT NULL,
    quantity          INT           NOT NULL,
    purchase_price    DECIMAL(10,2) NOT NULL,
    amount            DECIMAL(12,2) NOT NULL,
    received_quantity INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_poi_order (order_id)
) ENGINE = InnoDB COMMENT = '采购订单明细';

CREATE TABLE IF NOT EXISTS purchase_receipt (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    receipt_no   VARCHAR(64)   NOT NULL,
    order_id     BIGINT        NOT NULL,
    supplier_id  BIGINT        NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    remark       VARCHAR(255)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pr_no (receipt_no)
) ENGINE = InnoDB COMMENT = '采购入库单';

CREATE TABLE IF NOT EXISTS purchase_receipt_item (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id    BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    quantity      INT    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_pri_receipt (receipt_id)
) ENGINE = InnoDB COMMENT = '采购入库单明细';

CREATE TABLE IF NOT EXISTS purchase_return (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    return_no    VARCHAR(64)   NOT NULL,
    supplier_id  BIGINT        NOT NULL,
    order_id     BIGINT        NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    reason       VARCHAR(255)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prn_no (return_no)
) ENGINE = InnoDB COMMENT = '采购退货单';

CREATE TABLE IF NOT EXISTS purchase_return_item (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    return_id      BIGINT        NOT NULL,
    product_id     BIGINT        NOT NULL,
    quantity       INT           NOT NULL,
    purchase_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_pri_return (return_id)
) ENGINE = InnoDB COMMENT = '采购退货单明细';

-- ============ Step 04 库存 ============
CREATE TABLE IF NOT EXISTS inventory (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    product_id BIGINT   NOT NULL,
    quantity   INT      NOT NULL DEFAULT 0,
    version    BIGINT   NOT NULL DEFAULT 0 COMMENT '乐观锁',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_product (product_id)
) ENGINE = InnoDB COMMENT = '当前库存';

CREATE TABLE IF NOT EXISTS inventory_transaction (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    product_id BIGINT      NOT NULL,
    type       VARCHAR(32) NOT NULL COMMENT 'PURCHASE_IN/SALE_OUT/SALE_RETURN_IN/PURCHASE_RETURN_OUT/STOCKTAKE_ADJUST/MANUAL_ADJUST/LOSS_OUT',
    change_qty INT         NOT NULL COMMENT '正增负减',
    before_qty INT         NOT NULL,
    after_qty  INT         NOT NULL,
    ref_type   VARCHAR(32) NULL,
    ref_no     VARCHAR(64) NULL,
    remark     VARCHAR(255) NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_inv_tx_product (product_id),
    KEY idx_inv_tx_created (created_at)
) ENGINE = InnoDB COMMENT = '库存流水';

-- ============ Step 05 销售 ============
CREATE TABLE IF NOT EXISTS sales_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    order_no        VARCHAR(64)   NOT NULL,
    member_id       BIGINT        NULL,
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '1已完成 2部分退货 3已退货',
    total_amount    DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payable_amount  DECIMAL(12,2) NOT NULL,
    pay_method      VARCHAR(16)   NOT NULL COMMENT 'CASH/WECHAT/ALIPAY/CARD',
    points_earned   INT           NOT NULL DEFAULT 0,
    remark          VARCHAR(255)  NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_so_no (order_no),
    KEY idx_so_created (created_at)
) ENGINE = InnoDB COMMENT = '销售订单';

CREATE TABLE IF NOT EXISTS sales_order_item (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    order_id          BIGINT        NOT NULL,
    product_id        BIGINT        NOT NULL,
    quantity          INT           NOT NULL,
    unit_price        DECIMAL(10,2) NOT NULL,
    discount_amount   DECIMAL(10,2) NOT NULL DEFAULT 0,
    amount            DECIMAL(10,2) NOT NULL,
    promotion_id      BIGINT        NULL,
    returned_quantity INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_soi_order (order_id)
) ENGINE = InnoDB COMMENT = '销售订单明细';

CREATE TABLE IF NOT EXISTS sales_return (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    return_no    VARCHAR(64)   NOT NULL,
    order_id     BIGINT        NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    reason       VARCHAR(255)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_no (return_no)
) ENGINE = InnoDB COMMENT = '销售退货单';

CREATE TABLE IF NOT EXISTS sales_return_item (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    return_id     BIGINT        NOT NULL,
    order_item_id BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    quantity      INT           NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sri_return (return_id)
) ENGINE = InnoDB COMMENT = '销售退货单明细';

-- ============ Step 06 会员 ============
CREATE TABLE IF NOT EXISTS member_level (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    name             VARCHAR(30)  NOT NULL,
    points_threshold DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计消费升级阈值',
    discount         DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    sort_order       INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_level_name (name)
) ENGINE = InnoDB COMMENT = '会员等级';

CREATE TABLE IF NOT EXISTS member (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    member_no   VARCHAR(32)   NOT NULL,
    name        VARCHAR(50)   NOT NULL,
    phone       VARCHAR(20)   NOT NULL,
    gender      TINYINT       NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    birthday    DATE          NULL,
    level_id    BIGINT        NOT NULL,
    points      INT           NOT NULL DEFAULT 0,
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0,
    status      TINYINT       NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_no (member_no),
    UNIQUE KEY uk_member_phone (phone)
) ENGINE = InnoDB COMMENT = '会员表';

CREATE TABLE IF NOT EXISTS member_points_record (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    change     INT         NOT NULL,
    type       VARCHAR(16) NOT NULL COMMENT 'EARN/DEDUCT/ADJUST',
    ref_type   VARCHAR(32) NULL,
    ref_no     VARCHAR(64) NULL,
    remark     VARCHAR(255) NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_mpr_member (member_id)
) ENGINE = InnoDB COMMENT = '积分明细';

CREATE TABLE IF NOT EXISTS member_consumption (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    member_id      BIGINT        NOT NULL,
    sales_order_id BIGINT        NOT NULL,
    order_no       VARCHAR(64)   NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    points_earned  INT           NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_mc_member (member_id)
) ENGINE = InnoDB COMMENT = '会员消费记录';

-- ============ Step 07 促销 ============
CREATE TABLE IF NOT EXISTS promotion (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100)  NOT NULL,
    type           TINYINT       NOT NULL COMMENT '1折扣 2满减 3第二件优惠 4会员专享价',
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '0停用 1启用',
    start_time     DATETIME      NOT NULL,
    end_time       DATETIME      NOT NULL,
    discount_rate  DECIMAL(3,2)  NULL,
    min_amount     DECIMAL(10,2) NULL,
    reduce_amount  DECIMAL(10,2) NULL,
    second_rate    DECIMAL(3,2)  NULL,
    member_price   DECIMAL(10,2) NULL,
    remark         VARCHAR(255)  NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '促销活动';

CREATE TABLE IF NOT EXISTS promotion_product (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    promotion_id BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_promo_product (promotion_id, product_id),
    KEY idx_pp_product (product_id)
) ENGINE = InnoDB COMMENT = '促销参与商品';

-- ============ Step 08 盘点与损耗 ============
CREATE TABLE IF NOT EXISTS stocktake (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    task_no      VARCHAR(64) NOT NULL,
    status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0进行中 1已完成 2已取消',
    remark       VARCHAR(255) NULL,
    completed_at DATETIME    NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_st_no (task_no)
) ENGINE = InnoDB COMMENT = '盘点任务';

CREATE TABLE IF NOT EXISTS stocktake_item (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    stocktake_id BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    system_qty   INT    NOT NULL,
    actual_qty   INT    NULL,
    diff         INT    NULL COMMENT '实际-系统，正盘盈负盘亏',
    PRIMARY KEY (id),
    KEY idx_sti_stocktake (stocktake_id)
) ENGINE = InnoDB COMMENT = '盘点明细';

CREATE TABLE IF NOT EXISTS loss_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    product_id  BIGINT        NOT NULL,
    quantity    INT           NOT NULL,
    loss_amount DECIMAL(12,2) NOT NULL,
    reason      VARCHAR(255)  NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_loss_created (created_at)
) ENGINE = InnoDB COMMENT = '损耗记录';
