package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.DayProductionTotalVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProdFinalMapper.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Mapper
public interface FactoryMonthPlanProdFinalMapper extends CommBaseMapper<FactoryMonthPlanProdFinal> {

    /**
     * 将排产结果表复制到最终明细表
     *
     * @param param
     * @return
     */
    int copyDetailByResult(FactoryMonthPlanProdFinal param);

    /**
     * 查询对应定稿列表
     *
     * @param finalList
     * @return
     */
    List<FactoryMonthPlanProdFinal> selectParamList(List<FactoryMonthPlanProdFinal> finalList);

    /**
     * 获取日排产统计信息，日排产规格数及日排产总量
     *
     * @param productionVersion 排产版本号
     * @param days              获取的日集合
     * @return
     */
    List<DayProductionTotalVo> getStatisticsDay(@Param("productionVersion") String productionVersion, @Param("days") List<Integer> days);

    /**
     * 查询月定稿列表For周程滚动调整
     *
     * @param year 年度
     * @param month 月度
     * @param factoryCode 工厂
     * @return 月定稿列表
     */
    List<FactoryMonthPlanFinalAdjustVo> selectMpFinalList(@Param("year") Integer year, @Param("month") Integer month,
                                                          @Param("factoryCode") String factoryCode);
}
