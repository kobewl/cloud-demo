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
