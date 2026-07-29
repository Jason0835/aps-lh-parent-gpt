package com.zlt.aps.nc.engine.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcMachineMaintenance;
import com.zlt.aps.nc.api.domain.entity.NcParams;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;

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
    private Map<String, Integer> liningCxMachineCount;

    /** 各内衬规格关联的成型机台号集合 Map<liningCode, Set<machineCode>> */
    private Map<String, Set<String>> liningCxMachineSet;

    /** 各内衬规格的供应窗口班次数（排产深度） Map<liningCode, supplyDepth> */
    private Map<String, Integer> liningSupplyDepth;

    /** 交班库存 Map<liningCode, inventory> */
    private Map<String, BigDecimal> handoverInventory;

    /** 有效库存 Map<liningCode, stock> */
    private Map<String, BigDecimal> effectiveStockMap;

    /** 月度剩余量 Map<liningCode, liningRemaining>（月计划硫化余量 × 单耗） */
    private Map<String, BigDecimal> liningRemainingMap;

    /** 机台 Map<machineCode, NcMachineInfo> */
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

    /** 排产参数 Map<paramCode, NcParams> */
    private Map<String, NcParams> paramsMap;

    /** 机台维修计划 Map<machineCode, List<NcMachineMaintenance>> */
    private Map<String, List<NcMachineMaintenance>> maintenanceMap;

    /** 工作日历（内衬 + 成型）Map<procCode, List<MdmWorkCalendar>> */
    private Map<String, List<MdmWorkCalendar>> workCalendarMap;

    /** 上一个班最后生产的规格 Map<machineCode, lastLiningCode> */
    private Map<String, String> lastSpecOfPrevShift;

    /** 当前批次号 */
    private String currentBatchNo;

    /** 各机台各班次的生产顺序计数器 Map<machineCode, Map<shiftIndex, sequence>> */
    private Map<String, Map<Integer, Integer>> shiftSequenceMap;

    /** 班次索引→classIndex 映射数组（长度6），根据排程首班班次参数动态构建 */
    private String[] shiftClassMap;

    /** 成型班次偏移量 = Integer.parseInt(shiftClassMap[0]) - 1，用于将内衬班次索引映射到成型班次索引 */
    private Integer formingShiftOffset;

    /** 成型班次配置映射：(scheduleDay, shiftName) → classField序号（CLASS1→1, CLASS8→8）
     * 例如：(1, "03") → 3 表示 t-1日中班对应CLASS3 */
    private Map<String, Integer> cxShiftClassMap;

    /** 成型计划列表 */
    private List<CxScheduleResult> cxScheduleList;

    /** 施工数据 Map<constructionCode, List<MdmConstructionInfo>>，同一施工号可能存在多个BOM版本 */
    private Map<String, List<MdmConstructionInfo>> constructionMap;

    /** 内衬编码→物料名映射 Map<liningCode, liningName> */
    private Map<String, String> liningCodeToNameMap;

    /** 施工信息缓存 Map<embryoCode, Map<shiftIndex, MdmConstructionInfo>>，避免重复解析施工版本 */
    private Map<String, Map<Integer, MdmConstructionInfo>> constructionCache = new HashMap<>();

    /** 各班各规格内衬消耗量缓存 Map<liningCode, Map<formingClassIndex, consumeQty>> */
    private Map<String, Map<Integer, BigDecimal>> shiftConsumeCache = new HashMap<>();

    /** 各班各规格内衬消耗量缓存（仅量试/试制）Map<liningCode, Map<formingClassIndex, consumeQty>> */
    private Map<String, Map<Integer, BigDecimal>> shiftConsumeTrialCache = new HashMap<>();

    /** 前一日排产结果列表缓存（避免重复查询） */
    private List<NcScheduleResult> prevDayScheduleResults;

    /** 各班次索引对应的排产日数组（长度6），根据 NcShiftConfig 班次顺序动态构建
     *  scheduleDay 从1开始，当班次从last shift绕回first shift时递增 */
    private int[] scheduleDays;

    /** 每日班次数（从 NcShiftConfig 启用的班次记录数），用于计算提前备料天数对应的成型班次前移量 */
    private int shiftCountPerDay;

    /** 排程过程日志收集器 */
    private StringBuilder processLog;

    /**
     * 构建日志显示的规格名称：物料名(编码)
     *
     * @param name 物料名，可为 null
     * @param code 编码
     * @return 物料名(编码)，name 为 null 时回退显示编码本身
     */
    public static String buildDisplayName(String name, String code) {
        return name != null ? name + "(" + code + ")" : code;
    }

    /**
     * 根据内衬编码获取物料名(编码)（日志输出用，编码不存在时回退显示编码本身）
     */
    public String getLiningNameByCode(String liningCode) {
        if (liningCodeToNameMap != null && liningCode != null) {
            String name = liningCodeToNameMap.get(liningCode);
            return buildDisplayName(name, liningCode);
        }
        return liningCode;
    }

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
        // BigDecimal 转为纯数字字符串（避免 MessageFormat 自动加千分符）
        Object[] plainArgs = args;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof BigDecimal) {
                if (plainArgs == args) {
                    plainArgs = args.clone();
                }
                plainArgs[i] = ((BigDecimal) args[i]).toPlainString();
            }
        }
        processLog.append(java.text.MessageFormat.format(format, plainArgs)).append("\n");
    }

    /**
     * 获取已收集的排程日志文本
     */
    public String getProcessLogText() {
        return processLog != null ? processLog.toString() : "";
    }
}
