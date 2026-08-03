-- 库存表：库存服务专用数据库 stock_db
CREATE TABLE IF NOT EXISTS stock (
    id          BIGINT PRIMARY KEY COMMENT '库存ID（雪花算法生成）',
    product_id  BIGINT NOT NULL UNIQUE COMMENT '商品ID（一个商品一条库存记录）',
    quantity    INT NOT NULL COMMENT '库存数量',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT = '库存表';
