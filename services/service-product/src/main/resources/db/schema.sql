-- 商品表：商品服务专用数据库 product_db
CREATE TABLE IF NOT EXISTS product (
    id          BIGINT PRIMARY KEY COMMENT '商品ID（雪花算法生成）',
    name        VARCHAR(100) NOT NULL COMMENT '商品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    description VARCHAR(500) COMMENT '描述',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT = '商品表';

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
