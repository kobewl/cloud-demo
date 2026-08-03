-- 商品表：商品服务专用数据库 product_db
CREATE TABLE IF NOT EXISTS product (
    id          BIGINT PRIMARY KEY COMMENT '商品ID（雪花算法生成）',
    name        VARCHAR(100) NOT NULL COMMENT '商品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    description VARCHAR(500) COMMENT '描述',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT = '商品表';
