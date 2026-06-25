package com.zlt.aps.tm.engine.mapper;

import com.zlt.aps.tm.engine.domain.TmInventoryPredictQtyVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎面库存预测跨表查询 Mapper。
 *
 * <p>用于承载库存预测所需的成型早班需求量、胎面前夜班计划量 SQL，
 * 避免在服务层直接拼接原生 SQL。</p>
 */
public interface TmEngineInventoryPredictMapper {

    /**
     * 查询排程当天成型早班胎面需求量。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @return 胎面需求量行，treadCode 为胎面编码，qty 为早班需求量
     */
    List<TmInventoryPredictQtyVo> selectFirstShiftDemandRows(@Param("factoryCode") String factoryCode,
                                                             @Param("scheduleDate") Date scheduleDate,
                                                             @Param("treadCodes") List<String> treadCodes);

    /**
     * 查询排程前一天夜班胎面计划量。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程前一天日期
     * @param treadCodes   胎面编码列表
     * @return 胎面计划量行，treadCode 为胎面编码，qty 为早班计划量
     */
    List<TmInventoryPredictQtyVo> selectFirstShiftPlanRows(@Param("factoryCode") String factoryCode,
                                                           @Param("scheduleDate") Date scheduleDate,
                                                           @Param("treadCodes") List<String> treadCodes);
}
