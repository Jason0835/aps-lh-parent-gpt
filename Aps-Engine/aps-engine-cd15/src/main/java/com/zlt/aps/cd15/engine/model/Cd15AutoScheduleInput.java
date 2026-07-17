package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 斜裁自动排程统一输入模型。
 */
@Data
@Builder
public class Cd15AutoScheduleInput {

    /** 排程日期。 */
    private Date scheduleDate;
    /** 当前工厂启用的班次配置解析结果。 */
    private List<Cd15ShiftDescriptor> shifts;
    /** 成型排程来源数据。 */
    private List<CxScheduleResult> formingSchedules;
    /** 施工拆解后的钢带和加强层材料。 */
    private List<Cd15ConstructionMaterial> constructionMaterials;
    /** 胎胚月计划剩余量。 */
    private List<Cd15EmbryoPlanSurplus> embryoPlanSurpluses;
    /** 按钢带汇总的成型来源追溯信息。 */
    private Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip;
    /** 6点库存来源数据。 */
    private List<Cd15Stock> stocksAtSix;
    /** 启用的斜裁机台档案。 */
    private List<Cd15MachineInfo> machines;
    /** 标准卷曲长度配置。 */
    private List<Cd15CurlLength> curlLengths;
    /** 角度宽度配置。 */
    private List<Cd15AngleWidthMapping> angleWidthMappings;
    /** 备库深度配置。 */
    private List<Cd15DepthConfig> depthConfigs;
    /** 按钢带解析后的备库班数。 */
    private Map<String, BigDecimal> depthClassQtyBySteelStrip;
    /** 钢带/机台损耗率配置。 */
    private List<Cd15LossSetting> lossSettings;
    /** 按裁断角度归集的最大可分裁宽度。 */
    private Map<String, BigDecimal> angleWidthMaxByAngle;
    /** 机台大卷映射配置。 */
    private List<Cd15MachineRollMapping> machineRollMappings;
    /** 指定/不可作业机台配置。 */
    private List<Cd15SpecifyMachine> specifyMachines;
    /** 机台检修计划。 */
    private List<Cd15MachineMaintenancePlan> maintenancePlans;
    /** 6点库排资源快照。 */
    private List<Cd15StorageLaneLimit> storageLanesAtSix;
    /** GDYY 实际库存。 */
    private List<GdyyStock> gdyyStocks;
    /** GDYY 排程结果，用于后续成熟库存推算。 */
    private List<GdyyScheduleResult> gdyyPlans;
    /** 定时滚动目标班次之前已保留排程占用的资源。 */
    private List<Cd15RollingPrefixResourceUsage> prefixResourceUsages;
    /** 大卷成熟期小时数。 */
    private int agingPeriodHours;
}
