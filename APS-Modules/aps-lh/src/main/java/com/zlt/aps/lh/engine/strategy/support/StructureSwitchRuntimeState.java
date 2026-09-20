package com.zlt.aps.lh.engine.strategy.support;

import lombok.Value;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import java.io.Serializable;
import java.util.Date;

/** 同一次结构切换的已提交首班，跨SKU、物理机台和业务日共享。 */
@Value
public class StructureSwitchRuntimeState implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 配置快照中的来源记录主键。 */
    Long sourceId;
    /** 首个正量班次起点，避免跨日时按班别重新计数。 */
    Date firstShiftStart;
    /** 紧邻S1的真实班次；复制到窗口副本时不能按被重编号的槽位重新定位。 */
    LhShiftConfigVO secondShift;
    /** 首班最早实际落地SKU的物料编码。 */
    String materialCode;
    /** 首班SKU的产品状态。 */
    String productStatus;
    /** 首班SKU的胎胚编码。 */
    String embryoCode;
}
