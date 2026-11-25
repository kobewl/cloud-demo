-- 已有 order_db.orders 补支付截止时间列和超时扫描索引（只需执行一次）
-- 执行库：order_db
-- 执行前请确认列尚不存在：SHOW COLUMNS FROM orders LIKE 'expire_at';
-- 若列或索引已存在，再次执行会报 Duplicate column / Duplicate key name，不要重复执行
-- 不回填历史订单的 expire_at，保持 NULL；超时扫描带 expire_at IS NOT NULL，不会关掉这些老单
USE order_db;

ALTER TABLE orders
    ADD COLUMN expire_at DATETIME NULL COMMENT '支付截止时间（下单时由后端写入，老单可为空）' AFTER status,
    ADD KEY idx_orders_timeout_scan (status, expire_at, id);
