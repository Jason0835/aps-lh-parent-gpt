package com.zlt.mix.schedule.engine.service.decompose;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.vo.GlueSendReceiveVo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 分解胶料需求量引擎接口
 */
public interface DecomposeEngineService {

    /**
     * 根据终炼胶的汇总计划分解出对应的母炼胶的日计划
     * @param planDate 计划日期，格式：yyyy-MM-dd
     * @param paramMixArea 密炼区
     */
    void decomposePlan(Date planDate, String paramMixArea);

    /**
     * 委托方因为机台为空没办法计算出生产量，所以需要跨区确定机台后，在重新计算胶料的生产量
     * @param planDate  计划日期
     * @param mixArea   委托方的密炼区
     * @param retryReceiveIdList 需要重新计算生产量的接收记录的id
     * @return
     */
    GlueSendReceiveVo retrySpanProductQty(Date planDate, String mixArea, List<Long> retryReceiveIdList);

    /**
     * 分解胶料需求--新增(可以新增终炼胶，也可以新增母炼胶。新增后要把现新增的胶料的子胶 也一起计算新增进去)
     * @param plan
     */
    void addDecomposePlan(GlueDecomposePlan plan);

    /**
     * 修改了安全库存、生产量、机台后，当前记录以及它的子胶的计划量、生产量都需要重新计算
     * @param fatherGlueDecompose  上级胶
     * @param isModifyProduceQty   是否直接修改 生产量
     * @return
     */
    List<GlueDecomposePlan> recalculateDecomposePlan(GlueDecomposePlan fatherGlueDecompose, boolean isModifyProduceQty);

    /**
     * 新增补充塑炼胶分解计划
     *
     * @param mixArea      密炼区
     * @param planDate     计划日期
     * @param gluePlanList 分解计划
     */
    void updateSLDecomposePlan(String mixArea, Date planDate, List<GlueDecomposePlan> gluePlanList, Map<String, BigDecimal> mixingMinProductMap);
}
