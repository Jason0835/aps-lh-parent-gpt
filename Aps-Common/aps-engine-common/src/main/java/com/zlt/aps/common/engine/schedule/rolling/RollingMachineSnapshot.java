package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时自动滚动专用轻量快照，不映射数据库实体。
 */
@Data
public class RollingMachineSnapshot {
    /**
     * 机台编码。
     */
    private String machineCode = null;
    /**
     * 扣除默认速度检修损失后的有效米数上限。
     */
    private BigDecimal capacityQty = BigDecimal.ZERO;
    /**
     * 目标班已合并检修小时数。
     */
    private BigDecimal maintenanceHours = BigDecimal.ZERO;
    /**
     * 默认速度换算的检修扣量。
     */
    private BigDecimal maintenanceDeduct = BigDecimal.ZERO;
    /**
     * 容量配置阻断原因。
     */
    private String reason = null;
    /**
     * 上一物理班链尾主胶。
     */
    private String previousGlue = null;
    /**
     * 上一物理班链尾基部胶。
     */
    private String previousBaseGlue = null;
    /**
     * 上一物理班链尾口型。
     */
    private String previousMouth = null;
    /**
     * 上一物理班链尾物料。
     */
    private String previousMaterial = null;
    /**
     * 负余额后本轮停止自动分配。
     */
    private boolean allocationFrozen = false;
    /**
     * 机台目标班任务链。
     */
    private List<RollingTaskSnapshot> tasks = new ArrayList<>();
}
