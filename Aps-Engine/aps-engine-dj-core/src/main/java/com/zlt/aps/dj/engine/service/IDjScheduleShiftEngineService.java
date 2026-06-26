package com.zlt.aps.dj.engine.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.engine.model.CapacityValidateResult;
import com.zlt.aps.dj.engine.model.ShiftContext;
import com.zlt.aps.dj.engine.model.ShiftValidateResult;

/**
 * 垫胶排程顺延引擎接口
 * <p>
 * 处理插单/调整增量时的产能校验、同班顺位顺延、跨班顺延、末端减量等核心逻辑。
 * 对应设计文档「垫胶排程调整算法设计.md」中2~3的处理逻辑。
 * </p>
 *
 * @author zlt
 */
public interface IDjScheduleShiftEngineService {

    // ==================== 校验方法 ====================

    /**
     * 2.2 约束一校验（基于完成量查询）
     *
     * @param factoryCode   工厂编码
     * @param scheduleDate  排产日期
     * @param machineCode   机台编码
     * @param targetClass   目标班次索引（1~6）
     * @param targetSeq     目标顺位
     * @return 校验结果
     */
    ShiftValidateResult validateInsertConstraint(String factoryCode, Date scheduleDate,
                                                 String machineCode, int targetClass, int targetSeq);

    /**
     * 2.3 约束二校验 — 产能校验
     *
     * @param machineCode     机台编码
     * @param classIndex      目标班次索引（1~6）
     * @param insertPlanQty   插单计划量
     * @param currentResults  当前排程结果列表
     * @return 产能校验结果
     */
    CapacityValidateResult validateCapacity(String machineCode, int classIndex,
                                            BigDecimal insertPlanQty,
                                            List<DjScheduleResult> currentResults);

    // ==================== 顺延处理 ====================

    /**
     * 2.4 插入与顺延处理（核心顺延引擎）
     *
     * @param context 顺延上下文
     * @return 所有变更后的排程结果列表
     */
    List<DjScheduleResult> processInsertAndCascade(ShiftContext context);

    /**
     * 3.4 减量后顺位空洞整理
     *
     * @param machineResults 当前机台排程结果列表
     * @param classIndex     班次索引
     */
    void reorganizeAfterReduce(List<DjScheduleResult> machineResults, int classIndex);

    // ==================== 班次字段访问工具方法 ====================

    /**
     * 根据班次索引获取顺位
     */
    Integer getSequenceByIndex(DjScheduleResult sr, int classIndex);

    /**
     * 根据班次索引设置顺位
     */
    void setSequenceByIndex(DjScheduleResult sr, int classIndex, Integer seq);

    /**
     * 根据班次索引获取计划量
     */
    BigDecimal getPlanQtyByIndex(DjScheduleResult sr, int classIndex);

    /**
     * 根据班次索引设置计划量
     */
    void setPlanQtyByIndex(DjScheduleResult sr, int classIndex, BigDecimal qty);

    /**
     * 根据班次索引获取原因分析
     */
    String getAnalysisByIndex(DjScheduleResult sr, int classIndex);

    /**
     * 根据班次索引设置原因分析
     */
    void setAnalysisByIndex(DjScheduleResult sr, int classIndex, String analysis);
}
