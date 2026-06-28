package com.zlt.aps.maindata.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mp.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanMonitorMapper.java
 * 描    述：月度硫化监控Mapper接口
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpMonthPlanMonitorEntityMapper extends CommBaseMapper<MpMonthPlanMonitor> {
    /**
     * 月度硫化监控报表
     * @param dto
     * @return
     */
    List<MpMonthPlanMonitor> listReport(MpMonthPlanMonitor queryVo);

    /**
     * 更新监控表完成量
     * @param queryVo
     * @return
     */
    int updateByDayFinish(MpMonthPlanMonitor paramVo);

    /**
     * 查询当月存在正规计划(productStatus=S)的物料及其月计划监控记录。
     * <p>用于量试合格品充抵正规订单场景：同物料在月计划中只存在一条正规记录。</p>
     *
     * @param factoryCode 工厂编码
     * @param year 年
     * @param month 月
     * @param materialCodes 物料编码列表
     * @return 正规计划记录列表
     */
    List<MpMonthPlanMonitor> selectFormalPlanByMaterials(@Param("factoryCode") String factoryCode,
                                                        @Param("year") Integer year,
                                                        @Param("month") Integer month,
                                                        @Param("materialCodes") List<String> materialCodes);
}
