package com.zlt.aps.mp.demand.mapper;

import com.zlt.aps.mp.api.domain.entity.ProductStockMonth;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductStockMonthMapper.java
 * 描    述：物料月库存信息Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
@Mapper
public interface ProductStockMonthMapper extends CommBaseMapper<ProductStockMonth> {

    /**
     * 查询库存信息列表
     */
    List<ProductStockMonth> selectRelateList(ProductStockMonth queryVO);
}
