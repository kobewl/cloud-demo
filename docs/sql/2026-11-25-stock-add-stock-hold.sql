-- 已有 stock_db 补库存预占表（与实体 StockHold 对齐）
-- 执行库：stock_db
-- CREATE TABLE IF NOT EXISTS，表已存在时不会改动已有数据，可重复执行
USE stock_db;

CREATE TABLE IF NOT EXISTS stock_hold (
    order_id    BIGINT NOT NULL COMMENT '订单ID（主键，由业务显式传入，不自动生成）',
    product_id  BIGINT NOT NULL COMMENT '商品ID',
    `count`     INT NOT NULL COMMENT '预占数量（正数校验由业务承担）',
    status      INT NOT NULL COMMENT '状态：0-预占中 1-已确认 2-已释放',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存预占记录（一单一条）';
