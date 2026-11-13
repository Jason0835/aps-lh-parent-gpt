package com.zlt.aps.common.core.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * TM/TC自动滚动解释公共字段；具体模块实体负责表名映射。
 */
@Data
public abstract class AutoRollingExplainRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 本次执行标识。
     */
    @TableField("EXECUTION_ID")
    private String executionId;
    /**
     * SUMMARY或ITEM。
     */
    @TableField("RECORD_TYPE")
    private String recordType;
    /**
     * 执行内物料或固定摘要键。
     */
    @TableField("OBJECT_KEY")
    private String objectKey;
    /**
     * 稳定窗口MD5原始16字节，只有当前SUMMARY非空。
     */
    @TableField("REQUEST_KEY")
    @JsonIgnore
    private byte[] requestKey;
    /**
     * 本次执行在基础物理窗口内的修订号。
     */
    @TableField("REQUEST_REVISION")
    private Integer requestRevision;
    /**
     * 基础物理窗口MD5键，SUMMARY专用。
     */
    @TableField("BASE_REQUEST_KEY")
    @JsonIgnore
    private byte[] baseRequestKey;
    /**
     * 本次动作的窗口范围唯一MD5键，SUMMARY专用。
     */
    @TableField("ACTION_KEY")
    @JsonIgnore
    private byte[] actionKey;
    /**
     * 调用方生成的稳定动作标识。
     */
    @TableField("CLIENT_REQUEST_ID")
    private String clientRequestId;
    /**
     * 同一基础窗口上一条执行摘要标识。
     */
    @TableField("PREVIOUS_EXECUTION_ID")
    private String previousExecutionId;
    /**
     * 固定的目标物理班开始时刻。
     */
    @TableField("PHYSICAL_START_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date physicalStartTime;
    /**
     * 执行登记及恢复状态：RUNNING、COMPLETED或FAILED。
     */
    @TableField("EXECUTION_STATE")
    private String executionState;
    /**
     * 同窗口尝试序号。
     */
    @TableField("ATTEMPT_NO")
    private Integer attemptNo;
    /**
     * 工厂编码。
     */
    @TableField("FACTORY_CODE")
    private String factoryCode;
    /**
     * 主侧排程日期。
     */
    @TableField("SCHEDULE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;
    /**
     * 主侧班序。
     */
    @TableField("SHIFT_ORDER")
    private Integer shiftOrder;
    /**
     * 当前有效批次。
     */
    @TableField("BATCH_NO")
    private String batchNo;
    /**
     * 后台任务标识。
     */
    @TableField("TASK_ID")
    private String taskId;
    /**
     * 胎面或胎侧编码。
     */
    @TableField("MATERIAL_CODE")
    private String materialCode;
    /**
     * 业务终态。
     */
    @TableField("STATUS")
    private String status;
    /**
     * 阈值方向。
     */
    @TableField("DIRECTION")
    private String direction;
    /**
     * 实际变化类型。
     */
    @TableField("CHANGE_TYPE")
    private String changeType;
    /**
     * 业务原因编码。
     */
    @TableField("REASON_CODE")
    private String reasonCode;
    /**
     * 初始计划总米数。
     */
    @TableField("INITIAL_PLAN_QTY")
    private BigDecimal initialPlanQty;
    /**
     * 结算目标总米数。
     */
    @TableField("TARGET_PLAN_QTY")
    private BigDecimal targetPlanQty;
    /**
     * 最终计划总米数。
     */
    @TableField("ACTUAL_PLAN_QTY")
    private BigDecimal actualPlanQty;
    /**
     * 实际新增承接米数。
     */
    @TableField("ACCEPTED_UP_QTY")
    private BigDecimal acceptedUpQty;
    /**
     * 上修缺口米数。
     */
    @TableField("GAP_QTY")
    private BigDecimal gapQty;
    /**
     * 让位米数。
     */
    @TableField("YIELDED_QTY")
    private BigDecimal yieldedQty;
    /**
     * 初始供应班数。
     */
    @TableField("COVERAGE")
    private BigDecimal coverage;
    /**
     * 供应窗口是否封顶。
     */
    @TableField("IS_WINDOW_CAPPED")
    private Integer isWindowCapped;
    /**
     * 需求窗口消耗后余量。
     */
    @TableField("ENDING_AVAILABLE_QTY")
    private BigDecimal endingAvailableQty;
    /**
     * 最终或绑定机台。
     */
    @TableField("MACHINE_CODE")
    private String machineCode;
    /**
     * 可复核的公式摘要。
     */
    @TableField("FORMULA_SUMMARY")
    private String formulaSummary;
    /**
     * 按需装配的证据JSON。
     */
    @TableField("EVIDENCE_JSON")
    private String evidenceJson;
    /**
     * 事务提交标记。
     */
    @TableField("IS_COMMITTED")
    private Integer isCommitted;
    /**
     * 是否实际改变计划。
     */
    @TableField("PLAN_CHANGED")
    private Integer planChanged;
    /**
     * 算法版本。
     */
    @TableField("ALGORITHM_VERSION")
    private String algorithmVersion;
    /**
     * 输入摘要。
     */
    @TableField("INPUT_FINGERPRINT")
    private String inputFingerprint;
    /**
     * 证据结构版本。
     */
    @TableField("SCHEMA_VERSION")
    private Integer schemaVersion;
    /**
     * 执行结束时间。
     */
    @TableField("FINISHED_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedTime;
}
