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
     * 是否更新结构最早可供硫化时间 0-否、1-是，缺省 0。
     *
     * <p>前端编排开关：勾选时由前端依次执行「硫化排程 → 成型排程 → 再硫化排程
     * （若同步生成成型勾选则再成型）」两次成型刷新 T_CX_EMBRYO_LH_TIME 后由第二次硫化消费新表；
     * 后端不重算该表（结构可收尾时间重算服务已废弃）。</p>
     *
     * <p>后端用途：置位时 {@code loadStructureEarliestLhTime} 会额外加载窗口中间日批次
     * （= 排程目标日，即本次编排里今天刚跑完的成型批次），重叠结构用该批次覆盖窗口第一天批次
     * （老批独有结构保留），两批并集给出更远的班次预测覆盖，供硫化拿到更未来一段时间的胎胚可供时间。</p>
     */
    private String updateStructureLhTime;
}
