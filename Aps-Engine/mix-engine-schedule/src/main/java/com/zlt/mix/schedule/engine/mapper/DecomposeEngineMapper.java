package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.vo.GlueDecomposeSpanVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 分解胶料需求量引擎mapper
 */
public interface DecomposeEngineMapper {

    /**
     * 查询出带分解的终炼胶计划列表
     * @param planDate 计划日期
     * @param mixArea 密炼区
     * @return
     */
    List<GlueDecomposePlan> listWaitForDecomposePlan(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);


    /**
     * 查询出跨区发送后，因为委托密炼区没机台，导致不能计算出生产量的胶料信息
     * @param planDate  计划日期
     * @param mixArea   委托方密炼区
     * @param retryReceiveIdList  跨区接收记录id（委托方没办法计算出生产量的记录）
     * @return
     */
    List<GlueDecomposeSpanVo> listRetrySpanProductQty(@Param("planDate") Date planDate, @Param("mixArea") String mixArea, @Param("retryReceiveIdList") List<Long> retryReceiveIdList);

    /**
     * 把胶料分解计划同步到日志表中
     * @param planDate 计划日期
     * @param mixArea 密炼区
     */
    void synclueDecomposePlanToLog(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 物理删除胶料分解计划
     * @param planDate 计划日期
     * @param mixArea 密炼区
     */
    void deleteGlueDecomposePlan(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 批量新增胶料分解计划
     * @param list
     */
    void batchInsertGlueDecomposePlan(@Param("list") List<GlueDecomposePlan> list);

    /**
     * 批量更新删除标识（逻辑删除）
     * @param list
     */
    void updateDecomposePlanDelFlag(@Param("list") List<GlueDecomposePlan> list);

    /**
     *
     * @param planDate  计划日期
     * @param mixArea  密炼区
     * @param finalGlue  终炼胶名称
     * @return
     */
    List<GlueDecomposePlan> listSameFinalGlueDecomposePlan(@Param("planDate") Date planDate, @Param("mixArea") String mixArea , @Param("finalGlue") String finalGlue);

    /**
     * 查询密炼区+计划日期 这批的分解胶料需求计划的 批次号
     * @param planDate
     * @param mixArea
     * @return
     */
    String queryDecomposePlanBatchNo(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 查询出子胶料的分解计划
     * @param planDate 计划日期
     * @param upGlue  上一级胶
     * @param finalGlueMachine  对应的终胶和机台
     * @return
     */
    GlueDecomposePlan querySonDecompose(@Param("planDate") Date planDate, @Param("mixArea") String mixArea, @Param("upGlue") String upGlue, @Param("finalGlueMachine")  String finalGlueMachine);

    /**
     * 查询出父胶料的分解计划
     * @param planDate 计划日期
     * @param upGlue  上一级胶
     * @return
     */
    GlueDecomposePlan queryDecompose(@Param("planDate") Date planDate, @Param("mixArea") String mixArea, @Param("upGlue") String upGlue);

    /**
     * 根据分解列表查询历史的记录
     */
    List<GlueDecomposePlan> selectHistoryListByPlanList(List<GlueDecomposePlan>  gluePlanList);

    /**
     * 查询对应分级计划
     */
    List<GlueDecomposePlan> selectDecomposeList(@Param("mixArea") String mixArea, @Param("planDate") Date planDate);
}
