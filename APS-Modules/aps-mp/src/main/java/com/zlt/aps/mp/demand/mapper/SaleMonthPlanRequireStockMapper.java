package com.zlt.aps.mp.demand.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanRequireStock;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductStockMonthMapper.java
 * 描    述：需求计划版本库存信息Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-05-16
 */
@Mapper
public interface SaleMonthPlanRequireStockMapper extends CommBaseMapper<MonthPlanRequireStock> {
    /**
     * 更新物料信息
     *
     * @param factoryCode      分厂编码
     * @param monthPlanVersion 销售需求计划版本
     * @return
     */
    int updateProductInfo(@Param("factoryCode") String factoryCode,
                          @Param("monthPlanVersion") String monthPlanVersion);
}
