package com.zlt.aps.tm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 运维操作日志表
 * </p>
 *
 * @author zhangbinglin
 * @since 2022-02-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_MAINTENANCE_LOG")
//@KeySequence(value = "SEQ_MAINTENANCE_LOG",dbType = DbType.ORACLE)
public class MaintenanceLog extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_MAINTENANCE_LOG
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型：1--排程发布重置、2--排程删除。对应数据字典：MAINTENANCE_OPER_TYPE
     */
    @TableField("OPER_TYPE")
    private String operType;

    /**
     * 工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE）
     */
    @TableField("PROCEDURE_CODE")
    private String procedureCode;

    /**
     * 请求参数（JSON字符串）
     */
    @TableField("REQUEST_PARAMS")
    private String requestParams;

    /**
     * 操作状态（0正常 1异常）
     */
    @TableField("OPER_STATUS")
    private Integer operStatus;

    /**
     * 操作原因
     */
    @TableField("OPER_REASON")
    private String operReason;

    /**
     * 备注
     */
    @TableField("REMARK")
    private String remark;
}
