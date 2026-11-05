package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * 独立备库新行的逐机未建行证据。
 *
 * <p>只记录试算过程中已经得到的门禁结论或数量上限，不重新查询资料、不重新计算数量；
 * 用于过程日志和解释记录追溯“为什么本班没有建备库行”。</p>
 */
@Data
public class ScheduleExtraCreateSkipReason {

    /**
     * 任务业务键，用于回连主上下文任务并写入原因码。
     */
    private String businessKey;

    /**
     * 候选目标班次。
     */
    private Integer shiftOrder;

    /**
     * 机台编码；无法定位具体机台时为空。
     */
    private String machineCode;

    /**
     * 稳定拒绝码，必须在多语言资源中存在中文含义。
     */
    private String reasonCode;

    /**
     * 已算出的数量或时间明细，可为空。
     */
    private String detail;

    /**
     * 构造逐机未建行证据。
     *
     * @param businessKey 任务业务键
     * @param shiftOrder  目标班次
     * @param machineCode 机台编码
     * @param reasonCode  稳定拒绝码
     * @param detail      已算出的明细
     * @return 未被任何资源占用的只读证据
     */
    public static ScheduleExtraCreateSkipReason of(String businessKey, Integer shiftOrder, String machineCode,
                                                   String reasonCode, String detail) {
        ScheduleExtraCreateSkipReason reason = new ScheduleExtraCreateSkipReason();
        reason.setBusinessKey(businessKey);
        reason.setShiftOrder(shiftOrder);
        reason.setMachineCode(machineCode);
        reason.setReasonCode(reasonCode);
        reason.setDetail(detail);
        return reason;
    }
}
