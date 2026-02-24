package com.zlt.aps.maindata.service.impl;

import com.zlt.aps.maindata.mapper.ProductVulcanizingLimitMapper;
import com.zlt.aps.maindata.service.IProductVulcanizingLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductVulcanizingLimitServiceImpl.java
 * 描    述：ProductVulcanizingLimitServiceImpl基础数据-品种限制硫化机业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Service
public class ProductVulcanizingLimitServiceImpl implements IProductVulcanizingLimitService {

    private final ProductVulcanizingLimitMapper productVulcanizingLimitMapper;

    public ProductVulcanizingLimitServiceImpl(ProductVulcanizingLimitMapper productVulcanizingLimitMapper) {
        this.productVulcanizingLimitMapper = productVulcanizingLimitMapper;
    }
}
