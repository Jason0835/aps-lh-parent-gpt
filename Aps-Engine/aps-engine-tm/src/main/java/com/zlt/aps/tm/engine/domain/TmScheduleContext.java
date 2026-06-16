package com.zlt.aps.tm.engine.domain;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import lombok.Data;

import java.util.*;

/**
 * 胎面排程上下文。
 *
 * <p>贯穿一次胎面自动排程的运行态数据总线，承载批次、追踪号、排程日期、操作人、
 * 参数快照、待排任务和机台班次任务链。该对象会被步骤服务按流程原地补充数据。</p>
 */
@Data
public class TmScheduleContext {

    /** 工厂编号 */
    private String factoryCode;

    /** 批次号 */
    private String batchNo;

    /** 追踪标识 */
    private String traceId;

    /** 排程日期 */
    private Date scheduleDate;

    /** 操作人 */
    private String operator;

    /** 参数快照，key=参数编码 */
    private Map<String, TmParamValue> paramMap = new HashMap<>();

    /** 待排任务草稿列表 */
    private List<TmTaskDraft> taskDraftList = new ArrayList<>();

    /** 机台班次任务链集合 */
    private MachineShiftTaskChain<TmTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();

    /** 解释快照，key=任务业务键 */
    private Map<String, TmSnapshotBuildResult> snapshotMap = new HashMap<>();

    /** 本次落库转换汇总 */
    private TmPersistResult persistResult;

    /** 库存预测结果，key=胎面编码 */
    private Map<String, TmStockForecast> stockForecastMap = new HashMap<>();

    /**
     * 按参数编码读取本次排程参数快照。
     *
     * @param paramCode 参数编码
     * @return 参数快照值
     * @throws IllegalArgumentException 参数不存在或没有有效值时抛出
     */
    public TmParamValue getParam(String paramCode) {
        TmParamValue paramValue = paramMap.get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            throw new IllegalArgumentException("缺少胎面排程参数:" + paramCode);
        }
        return paramValue;
    }

    /**
     * 获取指定机台班次任务链。
     *
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     * @return 已存在任务链；不存在时返回空
     */
    public ScheduleTaskLinkedList<TmTaskDraft> getTaskChain(String machineCode, Integer shiftOrder) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("获取胎面任务链时排程日期不能为空");
        }
        return taskChainGroup.get(machineCode, DateUtil.toLocalDateTime(scheduleDate).toLocalDate(), shiftOrder);
    }

    public void setParamMap(Map<String, TmParamValue> paramMap) {
        this.paramMap = paramMap == null ? new HashMap<>() : paramMap;
    }

    public void setTaskDraftList(List<TmTaskDraft> taskDraftList) {
        this.taskDraftList = taskDraftList == null ? new ArrayList<>() : taskDraftList;
    }

    public void setTaskChainGroup(MachineShiftTaskChain<TmTaskDraft> taskChainGroup) {
        this.taskChainGroup = taskChainGroup == null ? new MachineShiftTaskChain<>() : taskChainGroup;
    }

    public void setSnapshotMap(Map<String, TmSnapshotBuildResult> snapshotMap) {
        this.snapshotMap = snapshotMap == null ? new HashMap<>() : snapshotMap;
    }

    public void setStockForecastMap(Map<String, TmStockForecast> stockForecastMap) {
        this.stockForecastMap = stockForecastMap == null ? new HashMap<>() : stockForecastMap;
    }
}
