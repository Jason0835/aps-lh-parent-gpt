package com.zlt.aps.dj.engine.model;

import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 排产上下文，承载步骤间共享数据
 */
@Data
public class DjScheduleContext {

    /** 工厂编码 */
    private String factoryCode;

    /** 排产日期 */
    private Date scheduleDate;

    /** 各垫胶规格的成型机台数量 Map<paddingCode, machineCount> */
    private Map<String, Integer> paddingCxMachineCount;

    /** 各垫胶规格的供应窗口班次数（排产深度） Map<paddingCode, supplyDepth> */
    private Map<String, Integer> paddingSupplyDepth;

    /** 交班库存 Map<paddingCode, inventory> */
    private Map<String, BigDecimal> handoverInventory;

    /** 消耗量 Map<paddingCode, Map<shiftIndex, consumeQty>> */
    private Map<String, Map<Integer, BigDecimal>> consumeQty;

    /** 有效库存 Map<paddingCode, stock> */
    private Map<String, BigDecimal> effectiveStockMap;

    /** 机台 Map<machineCode, DjMachineInfo> */
    private Map<String, DjMachineInfo> machineMap;

    /** 损耗率 Map<paddingCode+machineCode, lossRate> */
    private Map<String, BigDecimal> lossRateMap;

    /** 卷曲长度 Map<paddingCode, curlLength> */
    private Map<String, BigDecimal> curlLengthMap;

    /** 胶料顺序 Map<glueCode, orderNum> */
    private Map<String, Integer> glueOrderMap;

    /** 胶料组 Map<glueCode, glueGroupCode> */
    private Map<String, String> glueGroupMap;

    /** 胶料组顺序 Map<glueGroupCode, orderNum> */
    private Map<String, Integer> glueGroupOrderMap;

    /** 排产参数 Map<paramCode, paramValue> */
    private Map<String, String> paramsMap;

    /** 机台维修计划 Map<machineCode, List<DjMachineMaintenance>> */
    private Map<String, List<DjMachineMaintenance>> maintenanceMap;

    /** 工作日历（垫胶 + 成型）Map<procCode, List<MdmWorkCalendar>> */
    private Map<String, List<MdmWorkCalendar>> workCalendarMap;

    /** 上一个班最后生产的规格 Map<machineCode, lastPaddingCode> */
    private Map<String, String> lastSpecOfPrevShift;

    /** 当前批次号 */
    private String currentBatchNo;

    /** 当前批次内的订单序号计数器 */
    private int currentOrderSeq;
}
