package com.zlt.aps.nc.engine.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcMachineMaintenance;
import com.zlt.aps.nc.api.domain.entity.NcParams;

import lombok.Data;

/**
 * 排产上下文，承载步骤间共享数据
 */
@Data
public class NcScheduleContext {

    /** 工厂编码 */
    private String factoryCode;

    /** 排产日期 */
    private Date scheduleDate;

    /** 各内衬规格的成型机台数量 Map<liningCode, machineCount> */
    private Map<String, Integer> paddingCxMachineCount;

    /** 各内衬规格的供应窗口班次数（排产深度） Map<liningCode, supplyDepth> */
    private Map<String, Integer> paddingSupplyDepth;

    /** 交班库存 Map<liningCode, inventory> */
    private Map<String, BigDecimal> handoverInventory;

    /** 有效库存 Map<liningCode, stock> */
    private Map<String, BigDecimal> effectiveStockMap;

    /** 月度剩余量 Map<liningCode, paddingRemaining>（月计划硫化余量 × 单耗） */
    private Map<String, BigDecimal> paddingRemainingMap;

    /** 机台 Map<machineCode, DjMachineInfo> */
    private Map<String, NcMachineInfo> machineMap;

    /** 损耗率 Map<liningCode+machineCode, lossRate> */
    private Map<String, BigDecimal> lossRateMap;

    /** 卷曲长度 Map<liningCode, curlLength> */
    private Map<String, BigDecimal> curlLengthMap;

    /** 胶料顺序 Map<glueCode, orderNum> */
    private Map<String, Integer> glueOrderMap;

    /** 胶料组 Map<glueCode, glueGroupCode> */
    private Map<String, String> glueGroupMap;

    /** 胶料组顺序 Map<glueGroupCode, orderNum> */
    private Map<String, Integer> glueGroupOrderMap;

    /** 排产参数 Map<paramCode, DjParams> */
    private Map<String, NcParams> paramsMap;

    /** 机台维修计划 Map<machineCode, List<DjMachineMaintenance>> */
    private Map<String, List<NcMachineMaintenance>> maintenanceMap;

    /** 工作日历（内衬 + 成型）Map<procCode, List<MdmWorkCalendar>> */
    private Map<String, List<MdmWorkCalendar>> workCalendarMap;

    /** 上一个班最后生产的规格 Map<machineCode, lastliningCode> */
    private Map<String, String> lastSpecOfPrevShift;

    /** 当前批次号 */
    private String currentBatchNo;

    /** 各机台各班次的生产顺序计数器 Map<machineCode, Map<shiftIndex, sequence>> */
    private Map<String, Map<Integer, Integer>> shiftSequenceMap;

    /** 班次索引→classIndex 映射数组（长度6），根据排程首班班次参数动态构建 */
    private String[] shiftClassMap;

    /** 成型班次偏移量 = Integer.parseInt(shiftClassMap[0]) - 1，用于将内衬班次索引映射到成型班次索引 */
    private Integer formingShiftOffset;

    /** 成型计划列表 */
    private List<CxScheduleResult> cxScheduleList;

    /** 施工数据 Map<constructionCode, List<MdmConstructionInfo>>，同一施工号可能存在多个BOM版本 */
    private Map<String, List<MdmConstructionInfo>> constructionMap;

    /** 排程过程日志收集器 */
    private StringBuilder processLog;

    /**
     * 追加排程日志
     *
     * @param format 日志格式（MessageFormat 风格）
     * @param args   参数
     */
    public void appendLog(String format, Object... args) {
        if (processLog == null) {
            processLog = new StringBuilder(4096);
        }
        processLog.append(java.text.MessageFormat.format(format, args)).append("\n");
    }

    /**
     * 获取已收集的排程日志文本
     */
    public String getProcessLogText() {
        return processLog != null ? processLog.toString() : "";
    }
}
