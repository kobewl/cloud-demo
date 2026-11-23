-- 订单幂等改造：给已存在的 orders 表补 request_id 列 + 唯一索引（只需执行一次）
-- 执行库：order_db
-- 老数据 request_id 为 NULL；MySQL 唯一索引允许多个 NULL，所以不会冲突
USE order_db;

ALTER TABLE orders
    ADD COLUMN request_id VARCHAR(64) NULL COMMENT '幂等键：前端每次"下单意图"生成一个，同一个键只会落一单' AFTER id,
    ADD UNIQUE KEY uk_request_id (request_id);
