package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 续作临时故障模具置换运行态事件。
 *
 * <p>事件只保存一次故障迁移的可核对事实，不参与普通续作、换活字块或新增排序。</p>
 */
@Data
public class ContinuationTemporaryFaultTransferEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源故障计划主键。 */
    private List<Long> sourcePlanIdList = new ArrayList<Long>(2);
    /** 原续作运行态机台。 */
    private String originalMachineCode;
    /** 原续作物理机台。 */
    private String originalPhysicalMachineCode;
    /** 原续作物料。 */
    private String materialCode;
    /** 原续作物料描述。 */
    private String materialDesc;
    /** 原续作产品状态。 */
    private String productStatus;
    /** 故障开始暨原物料下机时间。 */
    private Date faultStartTime;
    /** 故障结束时间。 */
    private Date faultEndTime;
    /** 连续无法生产的班次序号。 */
    private List<Integer> affectedShiftIndexList = new ArrayList<Integer>(4);
    /** 原在机模具。 */
    private String originalMouldCode;
    /** 是否命中前日交替计划。 */
    private boolean previousAlternateMatched;
    /** 前日交替计划目标机台。 */
    private String previousAlternateMachineCode;
    /** 最终接收机台。 */
    private String targetMachineCode;
    /** 最终上机方式：换模或换活字块。 */
    private String transferMode;
    /** 未成功转移原因。 */
    private String failureReason;

    /**
     * 生成迁移备注。
     *
     * @return 原机台到新机台的业务备注；尚未迁移时返回旧机下机备注
     */
    public String buildRemark() {
        if (targetMachineCode == null || targetMachineCode.isEmpty()) {
            return "临时性故障模具下机：" + originalPhysicalMachineCode;
        }
        return "临时性故障模具置换：" + originalPhysicalMachineCode + " -> " + targetMachineCode;
    }
}
