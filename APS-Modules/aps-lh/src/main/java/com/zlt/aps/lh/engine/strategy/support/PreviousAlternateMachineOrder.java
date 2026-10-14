package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本批前日交替机台顺序快照，排名与唯一历史关系在续作决策前冻结。
 * 排名只决定释放优先级，不授予后料承接资格，也不修改原历史计划。
 *
 * @author Codex
 * @date 2026/10/09
 */
public final class PreviousAlternateMachineOrder {
    /**
     * 未命中历史机台时沿用原顺序。
     */
    public static final int UNMATCHED = Integer.MAX_VALUE;
    /**
     * 未初始化或没有有效历史时的空快照。
     */
    public static final PreviousAlternateMachineOrder EMPTY =
            new PreviousAlternateMachineOrder(Collections.emptyList(), Collections.emptyMap());
    /**
     * 按历史日期、班次排序并按物理机台去重的列表。
     */
    private final List<String> machineCodes;
    /**
     * 物理机台对应的固定排名。
     */
    private final Map<String, Integer> ranks;
    /**
     * 运行态机台及前物料对应的唯一有效计划，仅供原历史等待角色校验。
     */
    private final Map<String, LhMouldChangePlan> uniquePlans;

    /**
     * 复制顺序和关系索引，后续预演或资格刷新不能改变本批排名。
     *
     * @param machineCodes 已按历史顺序去重的物理机台列表
     * @param uniquePlans 已确认唯一的运行态机台及前物料关系
     */
    public PreviousAlternateMachineOrder(List<String> machineCodes, Map<String, LhMouldChangePlan> uniquePlans) {
        this.machineCodes = Collections.unmodifiableList(new ArrayList<>(machineCodes));
        Map<String, Integer> positions = new LinkedHashMap<>(machineCodes.size());
        for (int position = 0; position < machineCodes.size(); position++) {
            positions.put(machineCodes.get(position), position);
        }
        this.ranks = Collections.unmodifiableMap(positions);
        this.uniquePlans = Collections.unmodifiableMap(new LinkedHashMap<>(uniquePlans));
    }

    /**
     * 读取固定物理机台列表。
     *
     * @return 不可修改的历史机台顺序
     */
    public List<String> getMachineCodes() {
        return machineCodes;
    }

    /**
     * 将运行态侧别机台归一为物理机台后读取排名。
     *
     * @param machineCode 运行态或物理机台编码
     * @return 历史排名，未命中时返回 UNMATCHED
     */
    public int rank(String machineCode) {
        return ranks.getOrDefault(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode), UNMATCHED);
    }

    /**
     * 读取历史等待组的唯一关系；多计划歧义不因机台有排名而自动消除。
     *
     * @param machineCode 运行态机台编码
     * @param materialCode 当前前物料编码
     * @return 唯一计划，没有可靠关系时返回空
     */
    public LhMouldChangePlan uniquePlan(String machineCode, String materialCode) {
        return uniquePlans.get(machineCode + "|" + materialCode);
    }
}
