-- 库存表：库存服务专用数据库 stock_db
CREATE TABLE IF NOT EXISTS stock (
    id          BIGINT PRIMARY KEY COMMENT '库存ID（雪花算法生成）',
    product_id  BIGINT NOT NULL UNIQUE COMMENT '商品ID（一个商品一条库存记录）',
    quantity    INT NOT NULL COMMENT '库存数量',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT = '库存表';

-- 库存预占记录：一笔订单对应一条预占（主键为业务传入的订单ID）
-- ⚠ 老库升级：已有 stock_db 需手动执行 docs/sql/2026-11-25-stock-add-stock-hold.sql
CREATE TABLE IF NOT EXISTS stock_hold (
    order_id    BIGINT NOT NULL COMMENT '订单ID（主键，由业务显式传入，不自动生成）',
    product_id  BIGINT NOT NULL COMMENT '商品ID',
    `count`     INT NOT NULL COMMENT '预占数量（正数校验由业务承担）',
    status      INT NOT NULL COMMENT '状态：0-预占中 1-已确认 2-已释放',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存预占记录（一单一条）';

-- Seata AT 模式回滚账本（P8）：记录每个分支事务"改之前长什么样"，TC 靠它逆向补偿
-- 官方标准建表 SQL，三个参与事务的库都要建
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '序列化方式等上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息（照片JSON）',
    log_status    INT          NOT NULL COMMENT '0:正常 1:已防御',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AT事务模式undo表';
