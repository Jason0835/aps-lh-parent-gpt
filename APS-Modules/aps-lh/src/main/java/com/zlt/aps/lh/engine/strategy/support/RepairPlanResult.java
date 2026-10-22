package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次维修解析的独立结果，来源、维修和故障分别表达，不携带精度身份或额度。
 * 结果及容量投影均属于调用方；候选失败直接丢弃，不能撤销批次维修事实。
 *
 * @author APS
 * @date 2026/10/21
 */
@Data
public class RepairPlanResult {

    /**
     * 当前候选身份及时间输入快照，不作为跨候选缓存。
     */
    private RepairPlanRequest request;

    /**
     * 当前运行侧机台。
     */
    private String machineCode;
    /**
     * 约束所属物理机台。
     */
    private String physicalMachineCode;
    /**
     * 来源与窗口约束版本；候选组合不缓存，始终使用本次完整输入计算。
     */
    private long inputVersion;
    /**
     * 原始维修来源独立快照，主键可追溯到合并窗口覆盖的所有来源。
     */
    private List<MdmDevicePlanShut> sourcePlans = new ArrayList<>();
    /**
     * 合并后的各段维修及预热、来源日期早班边界。
     */
    private List<MachineMaintenanceWindowDTO> plannedRepairItems = new ArrayList<>();
    /**
     * 每个合并区间对应的全部来源主键，顺序与维修条目一致。
     */
    private List<List<Long>> plannedRepairSourceIds = new ArrayList<>();
    /**
     * 当前配置的维修早班固定量上限，实际排量仍由容量及账本限制。
     */
    private int fixedMorningQtyLimit;
    /**
     * 原始故障独立副本，不进入维修组合编排。
     */
    private List<MachineFaultWindowDTO> faultWindows = new ArrayList<>();
    /**
     * 仅供通用容量计算使用的维修和故障投影，不挂入精度窗口。
     */
    private List<MachineMaintenanceWindowDTO> capacityWindows = new ArrayList<>();
    /**
     * 当前切换是否命中维修；零预热或时间不后移时仍可为真。
     */
    private boolean plannedRepairAffectingSwitch;
    /**
     * 维修恢复与切换完成取最大后的生产下限，首检等约束继续由原链处理。
     */
    private Date productionNotBefore;
    /**
     * 组合处置状态，不支持只限定当前候选组合，不撤销维修事实。
     */
    private OverlapStatus overlapStatus = OverlapStatus.NONE;
    /**
     * 本次组合的处理原因或保留原路径的扩展说明。
     */
    private String reason;
    /**
     * 已在原续作阶段识别的故障迁移事件独立副本。
     */
    private List<ContinuationTemporaryFaultTransferEvent> faultTransferEvents = new ArrayList<>();
    /**
     * 仅最终收口阶段填充的来源执行日期，不以正量结果为前提。
     */
    private Map<Long, Date> scheduleDateFillMap = new LinkedHashMap<>();

    /**
     * 已有组合规则的处理状态。
     */
    public enum OverlapStatus {
        /**
         * 当前区间无相关组合。
         */
        NONE,
        /**
         * 已按原组合规则处理。
         */
        SUPPORTED,
        /**
         * 保留原占用并集及执行顺序，尚未增加协同规则。
         */
        EXISTING_PATH,
        /**
         * 正式链路原本拒绝的当前组合。
         */
        UNSUPPORTED
    }
}
