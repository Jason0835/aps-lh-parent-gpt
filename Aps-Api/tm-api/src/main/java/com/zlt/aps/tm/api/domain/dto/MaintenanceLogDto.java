package com.zlt.aps.tm.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 运维操作日志DTO
 * </p>
 *
 * @author zhangbinglin
 * @since 2022-02-08
 */
@Data
public class MaintenanceLogDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_MAINTENANCE_LOG")
    private Long id;

    @ApiModelProperty(value = "操作类型：1--排程发布重置、2--排程删除。对应数据字典：MAINTENANCE_OPER_TYPE")
    private String operType;

    @ApiModelProperty(value = "操作状态（0正常 1异常）")
    private Integer operStatus;

    @ApiModelProperty(value = "备注")
    private String remark;

    /**下面开始是请求参数字段信息**/
    @ApiModelProperty(value = "工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE）")
    private String procedureCode;

    @ApiModelProperty(value = "排程日期")
    private String scheduleDate;

    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    @ApiModelProperty(value = "物料编号（成型工序表示胎胚代码、胎面工序表示胎面代码、胎侧工序表示胎侧代码，以此类推）")
    private String materialCode;

    @ApiModelProperty(value = "机台id")
    private Long machineId;

    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @ApiModelProperty(value = "操作原因")
    private String operReason;

    @ApiModelProperty(value = "操作时间（开始时间）")
    private String startTime;

    @ApiModelProperty(value = "操作时间（结束时间）")
    private String endTime;
}
