package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import lombok.Value;
import lombok.ToString;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 结构切换规则首班的只读判定，供胚所在班、规则首班与实际首产班分别维护。
 *
 * @author APS
 * @date 2026/10/14
 */
@Value
public class StructureSwitchFirstShiftDecision implements Serializable {
    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 同一次切换的供胚来源主键。
     */
    Long sourceId;
    /**
     * 原始最早可供硫化时间。
     */
    Date supplyTime;
    /**
     * 供胚时间所属真实班次，含窗口前的T日晚班。
     */
    LhShiftConfigVO supplyShift;
    /**
     * 达标的供胚班或紧邻下一班；无法判定或超出范围时为空。
     */
    LhShiftConfigVO firstShift;
    /**
     * 首班所用参照组合的运行态机台编码。
     */
    String machineCode;
    /**
     * 参照组合的运行态完整班产。
     */
    Integer runtimeCapacity;
    /**
     * 普通机台为2条，配置单控运行侧为1条。
     */
    Integer minimumQuantity;
    /**
     * 供胚后剩余时长能完整完成的数量，禁止向上补模。
     */
    Integer availableQuantity;
    /**
     * 达标、顺延或拒绝的具体依据。
     */
    String reason;
    /**
     * 原窗口连续真实班次坐标，隔离副本只保留两个槽位时仍可计算相对首班偏移。
     */
    @ToString.Exclude
    List<LhShiftConfigVO> referenceShifts;
}
