package com.zlt.aps.cxlh.cx.api.domain.vo;


import com.zlt.aps.cxlh.cx.api.domain.entity.CxShiftConfig;
import com.zlt.aps.cxlh.cx.api.domain.enums.ScheduleTaskTypeEnum;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 成型排产任务业务模型
 * 对应PDF：续作/新增/试制任务的核心属性
 * @author 金宇全钢成型排产系统
 * @date 2026-03-23
 */
@Data
public class CxScheduleTask {
    /**
     * 任务ID（业务主键）
     */
    private String taskId;
    /**
     * 胎胚代码
     */
    private String embryoCode;
    /**
     * 物料编码（SKU）
     */
    private String materialCode;
    /**
     * 结构名称
     */
    private String structureName;
    /**
     * 排产任务类型
     */
    private ScheduleTaskTypeEnum taskType;
    /**
     * 所属班次
     */
    private List<CxShiftConfig> shiftList;
    /**
     * 胎胚库存
     */
    private BigDecimal embryoStock;
    /**
     * 待排产量
     */
    private BigDecimal toScheduleQty;
    /**
     * 成型余量
     */
    private BigDecimal cxResidueQty;
    /**
     * 硫化余量
     */
    private BigDecimal lhResidueQty;
    /**
     * 是否收尾：0-否，1-是
     */
    private Integer isEnding;
    /**
     * 推荐机台列表
     */
    private List<String> recommendMachineList;
    /**
     * 日硫化量
     */
    private Integer dailyLhQty;
    /**
     * 损耗率
     */
    private BigDecimal lossRate;
    /**
     * 分配是否成功
     */
    private Boolean allocateSuccess;
    /**
     * 分配失败信息
     */
    private String allocateMsg;
    /**
     * 分配的机台编码
     */
    private String allocateMachineCode;
}