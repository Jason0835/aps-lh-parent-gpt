package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.DayProductionTotalVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalMapper.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果-SKU Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-09-22
 */
@Mapper
public interface FactoryMonthPlanProductionFinalMapper extends CommBaseMapper<MonthPlanProductionFinalResult> {

    /**
     * 获取日排产统计信息，日排产规格数及日排产总量
     *
     * @param productionVersion 排产版本号
     * @param days              获取的日集合
     * @return
     */
    List<DayProductionTotalVo> getStatisticsDay(@Param("productionVersion") String productionVersion, @Param("days") List<Integer> days);

    /**
     * 根据排产编号批量更新排产结果
     * @param updateList 排产结果集合
     * @return 更新数量
     */
    int updateByProductionNo(@Param("list") List<MonthPlanProductionFinalResult> updateList);
    /**
     * 查询对应定稿列表
     *
     * @param finalList
     * @return
     */
    List<FactoryMonthPlanProdFinal> selectParamList(List<MonthPlanProductionFinalResult> finalList);
}
