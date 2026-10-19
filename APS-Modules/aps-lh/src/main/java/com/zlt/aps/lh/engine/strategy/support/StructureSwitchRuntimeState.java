package com.zlt.aps.lh.engine.strategy.support;

import lombok.Value;
import lombok.AllArgsConstructor;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import java.io.Serializable;
import java.util.Date;

/**
 * 同一次切换已提交的规则首班和实际首产状态，跨SKU、物理机台和业务日共享。
 *
 * @author APS
 * @date 2026/10/14
 */
@Value
@AllArgsConstructor
public class StructureSwitchRuntimeState implements Serializable {
    /**
     * 保持已有序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 配置快照中的来源记录主键。
     */
    Long sourceId;
    /**
     * 规则首班起点，避免跨日时按班别重新计数。
     */
    Date firstShiftStart;
    /**
     * 紧邻S1的真实班次；复制到窗口副本时不能按被重编号的槽位重新定位。
     */
    LhShiftConfigVO secondShift;
    /**
     * 首班最早实际落地SKU的物料编码。
     */
    String materialCode;
    /**
     * 首班SKU的产品状态。
     */
    String productStatus;
    /**
     * 首班SKU的胎胚编码。
     */
    String embryoCode;
    /**
     * 首个成功组合冻结的续作标识及供胚班判定，回裁不通过实际正量改写此依据。
     */
    StructureSwitchFirstShiftDecision firstShiftDecision;
    /**
     * 当前剩余正量对应的实际首产班起点。
     */
    Date actualFirstShiftStart;

    /**
     * 兼容原首班状态构造，适用范围外保留实际首产班口径。
     *
     * @param sourceId 供胚来源主键
     * @param firstShiftStart 首班起点
     * @param secondShift 紧邻第二班
     * @param materialCode 参照物料
     * @param productStatus 产品状态
     * @param embryoCode 参照胎胚
     */
    public StructureSwitchRuntimeState(Long sourceId, Date firstShiftStart, LhShiftConfigVO secondShift,
            String materialCode, String productStatus, String embryoCode) {
        this(sourceId, firstShiftStart, secondShift, materialCode, productStatus, embryoCode, null, firstShiftStart);
    }
}
