package com.zlt.aps.cx.vo;

import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 贪心分配共享参数 holder（消除多方法间 7 参数列表重复）。
 *
 * @author APS Team
 */
@Data
public class GreedyParams {
    /** 可用机台配置列表 */
    private List<MpCxCapacityConfiguration> availableMachines;
    /** 机台 -> 历史在产胎胚集合 */
    private Map<String, Set<String>> machineHistoryMap;
    /** 机台 -> 最大硫化机台数 */
    private Map<String, Integer> machineMaxLhMap;
    /** 机台 -> 最大胎胚种类数 */
    private Map<String, Integer> machineMaxEmbryoTypesMap;
    /** 续作预扣负荷（null 表示无预扣） */
    private Map<String, Integer> continueLoadMap;
    /** 续作预扣种类（null 表示无预扣） */
    private Map<String, Set<String>> continueTypeMap;
    /** 续作预扣硫化机台号（null 表示无预扣） */
    private Map<String, Set<String>> continueLhMachineCodeMap;
}
