package com.zlt.aps.tm.engine.domain;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面待排任务草稿。
 *
 * <p>用于在自动排程步骤之间传递运行态任务数据，不继承数据库实体。它承载胎面、胶料、
 * 口型板、需求量、计划量、库存覆盖和未排原因等信息，最终由落库服务转换成结果表实体。</p>
 */
@Data
public class TmTaskDraft {

    /** 工单号 */
    private String orderNo;

    /** 机台编码 */
    private String machineCode;

    /** 胎面编码 */
    private String treadCode;

    /** 主胶料编码 */
    private String glueCode;

    /** 基部胶编码 */
    private String baseGlueCode;

    /** 口型板编码 */
    private String mouthPlateCode;

    /** 需求量 */
    private BigDecimal demandQty;

    /** 计划量 */
    private BigDecimal planQty;

    /** 未排原因编码 */
    private String unplannedReasonCode;

    /** 未排原因描述 */
    private String unplannedReasonDesc;

    /**
     * 获取稳定业务键。
     *
     * @return 按工单、胎面、胶料和口型板拼接的业务键，用于排序兜底和日志
     */
    public String getBusinessKey() {
        return safe(orderNo) + "|" + safe(treadCode) + "|" + safe(glueCode) + "|" + safe(mouthPlateCode);
    }

    /**
     * 判断任务是否尚未分配机台。
     *
     * @return true 表示机台为空
     */
    public boolean isUnassigned() {
        return StrUtil.isBlank(machineCode);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
