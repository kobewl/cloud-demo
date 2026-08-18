-- 订单表：订单服务专用数据库 order_db
-- 表名用 orders（order 是 MySQL 保留字，直接用会报语法错误）
CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT PRIMARY KEY COMMENT '订单ID（雪花算法生成）',
    product_id   BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称（下单时快照，商品改名不影响历史订单）',
    price        DECIMAL(10,2) NOT NULL COMMENT '成交单价',
    count        INT NOT NULL COMMENT '购买数量',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额 = 单价 x 数量',
    status       TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-已创建 1-已支付 2-已取消',
    deleted      TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（修改时自动刷新）'
) COMMENT = '订单表';
