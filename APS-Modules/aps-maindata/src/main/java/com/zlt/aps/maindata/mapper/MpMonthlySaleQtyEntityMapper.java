package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQtyMapper.java
 * 描    述：月均销量Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Mapper
public interface MpMonthlySaleQtyEntityMapper extends CommBaseMapper<MpMonthlySaleQty> {

    /**
     * 根据SKU+区域汇总发货量
     *
     * @param salesOrderPool 定时任务默认查上个月1号到这个月1号的数据，如果跨月查会有问题
     *                       例：2025-11-01 2025-12-01
     * @return 结果
     */
    List<SalesOrderPool> selectSumQtyByMonthAndArea(SalesOrderPool salesOrderPool);
}
