package com.wangliang.cloud.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wangliang.cloud.product.entity.Product;
import com.wangliang.cloud.product.mapper.ProductMapper;
import com.wangliang.cloud.product.service.ProductService;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
