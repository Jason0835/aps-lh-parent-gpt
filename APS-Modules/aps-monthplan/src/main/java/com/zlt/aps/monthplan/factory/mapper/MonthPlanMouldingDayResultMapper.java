package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.vo.DayProductionTotalVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanDayResultStatisticsVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanMouldingDayResultVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanMouldingDayResultMapper.java
 * 描    述：分厂月生产计划排产过程-模具排产结果汇总Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-13
 */
@Mapper
public interface MonthPlanMouldingDayResultMapper extends CommBaseMapper<MonthPlanMouldingDayResult> {

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     */
    List<String> productionVersionList(MonthPlanMouldingDayResult query);

    /**
     * 获取日排产统计信息，日排产规格数及日排产总量
     *
     * @param productionVersion 排产版本号
     * @param days              获取的日集合
     * @return
     */
    List<DayProductionTotalVo> getStatisticsDay(@Param("productionVersion") String productionVersion, @Param("days") List<Integer> days);

    /**
     * 获取日排产统计信息，日排产规格数及日排产总量
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    List<MonthPlanMouldingDayResultVo> listFacProduct(MonthPlanMouldingDayResult queryVO);

    /**
     * 获取月计划排产统计页面
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    List<MonthPlanDayResultStatisticsVo> listFacProductStatistics(MonthPlanMouldingDayResult queryVO);
}
