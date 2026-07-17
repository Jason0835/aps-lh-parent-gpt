package com.zlt.aps.tc.engine.mapper;

import com.zlt.aps.tc.engine.domain.TcInventoryPredictQtyVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎侧库存预测跨表查询 Mapper。
 *
 * <p>用于承载库存预测所需的成型早班需求量、胎侧前夜班计划量 SQL，
 * 避免在服务层直接拼接原生 SQL。</p>
 */
public interface TcEngineInventoryPredictMapper {

    /**
     * 查询排程当天成型早班胎侧需求量。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param sidewallCodes   胎侧编码列表
     * @return 胎侧需求量行，sidewallCode 为胎侧编码，qty 为早班需求量
     */
    List<TcInventoryPredictQtyVo> selectFirstShiftDemandRows(@Param("factoryCode") String factoryCode,
                                                             @Param("scheduleDate") Date scheduleDate,
                                                             @Param("sidewallCodes") List<String> sidewallCodes);

    /**
     * 查询排程当天成型早班胎侧需求量（RECIPE 模式）。
     *
     * <p>按 (EMBRYO_CODE, CLASS1_RECIPE_NO) 关联施工信息取胎侧标准长度，
     * 示方书为空时不匹配，该行不计入汇总。</p>
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param sidewallCodes   胎侧编码列表
     * @return 胎侧需求量行，sidewallCode 为胎侧编码，qty 为早班需求量
     */
    List<TcInventoryPredictQtyVo> selectFirstShiftDemandRowsByRecipe(@Param("factoryCode") String factoryCode,
                                                                     @Param("scheduleDate") Date scheduleDate,
                                                                     @Param("sidewallCodes") List<String> sidewallCodes);

    /**
     * 查询排程前一天夜班胎侧计划量。
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程前一天日期
     * @param sidewallCodes   胎侧编码列表
     * @return 胎侧计划量行，sidewallCode 为胎侧编码，qty 为早班计划量
     */
    List<TcInventoryPredictQtyVo> selectFirstShiftPlanRows(@Param("factoryCode") String factoryCode,
                                                           @Param("scheduleDate") Date scheduleDate,
                                                           @Param("sidewallCodes") List<String> sidewallCodes);
}
