package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultMapper.java
 * 描    述：工厂月生产计划-最终排产计划定稿Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Mapper
public interface FactoryMonthPlanProductionFinalResultEntityMapper extends CommBaseMapper<FactoryMonthPlanProductionFinalResult> {
    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<FactoryMonthPlanProductionFinalResult> getVersionList(FactoryMonthPlanProductionFinalResult queryVO);

    /**
     * 查询最终排产计划定稿列表-调整使用
     * @param queryVO 查询条件
     * @return 结果
     */
    List<FactoryMonthPlanFinalAdjustVo> list4Adjust(FactoryMonthPlanProductionFinalResult queryVO);

    /**
     * 计算上月超欠产并更新定稿表
     * 超欠产 = 上月硫化日完成量(合格品) - 上月计划排产量
     *
     * @param year      上月年份
     * @param month     上月月份
     * @param startDate 上月开始日期
     * @param endDate   上月结束日期
     * @return 更新记录数
     */
    int updateLastMonthOverProd(@Param("year") Integer year,
                                @Param("month") Integer month,
                                @Param("startDate") Date startDate,
                                @Param("endDate") Date endDate);
}
