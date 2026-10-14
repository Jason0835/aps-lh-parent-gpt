package com.zlt.aps.lh.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 硫化排程请求参数
 *
 * @author APS
 */
@Data
public class LhScheduleRequestDTO {

    /** 分厂编号 */
    private String factoryCode;
    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 排程业务日期(默认T+1)，仅用于业务归属；排程窗口仍为T～T+2三天8班次 */
    private Date scheduleDate;
    /** 月计划需求版本 */
    private String monthPlanVersion;
    /** 月计划排产版本 */
    private String productionVersion;
    /** 操作人 */
    private String operator;

    /** 是否自动生成成型 0-否、1-是 */
    private String isAutoExecCx;

    /**
     * 是否更新结构切换时间（T_CX_EMBRYO_LH_TIME 按最新成型余量重算）0-否、1-是，缺省 0。
     *
     * <p>前端「硫化自动排程」弹窗默认不勾选：勾选时在基础数据加载阶段按最新成型余量与机台班产
     * 重算各结构可收尾时间并回写；未勾选（或未传）时跳过该步骤，保留成型排程写入的原始快照。
     */
    private String updateStructureLhTime;
}
