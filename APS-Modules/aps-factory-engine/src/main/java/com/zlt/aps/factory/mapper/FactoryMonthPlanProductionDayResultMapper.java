package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionDayResult;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionDayResultMapper.java
 * 描    述：分厂月生产计划排产过程-排产结果对象-SKU-Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-22
 */
@Mapper
public interface FactoryMonthPlanProductionDayResultMapper extends CommBaseMapper<MonthPlanProductionDayResult> {

    /**
     * 获取日排产统计信息，日排产规格数及日排产总量
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    List<MonthPlanMouldingDayResult> listInsertDataByVersion(MonthPlanMouldingDayResult queryVO);

    /**
     * 根据版本号查询数据，商品编码分组汇总
     *
     * @param saleMonthPlanRequire 查询条件
     * @return 列表
     */
    List<SaleMonthPlanRequire> selectByVersionGroupProductCode(SaleMonthPlanRequire saleMonthPlanRequire);

    /**
     * 查询是否继续排产
     *
     * @param confParam 查询条件
     * @return 列表
     */
    List<ProductionMouldConfiguration> selectIsContinueList(ProductionMouldConfiguration confParam);

    /**
     * 批量插入数据
     *
     * @param monthPlanMouldingDayResultVoList 列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<MonthPlanProductionDayResult> monthPlanMouldingDayResultVoList);

    /**
     * 根据版本号删除数据
     *
     * @param monthPlanMouldingDayResult 删除条件
     * @return 影响行数
     */
    int deleteByVersion(MonthPlanMouldingDayResult monthPlanMouldingDayResult);
}
