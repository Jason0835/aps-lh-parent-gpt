package com.zlt.aps.cd15.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 斜裁调量请求。
 */
@Data
@ApiModel(value = "斜裁调量请求")
public class Cd15ChangeQtyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工厂编码")
    private String factoryCode;
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;
    @ApiModelProperty("排程结果ID")
    private Long scheduleResultId;
    @ApiModelProperty("分裁组号")
    private String groupNo;
    @ApiModelProperty("机台编码")
    private String machineCode;
    @ApiModelProperty("钢带代码")
    private String steelStripCode;
    @ApiModelProperty("单班调量班次")
    private String startClassField;
    @ApiModelProperty("单班目标计划量")
    private Double targetPlanQty;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("是否已确认影响")
    private Boolean confirmed;

    private Double class1PlanQty;
    private Double class2PlanQty;
    private Double class3PlanQty;
    private Double class4PlanQty;
    private Double class5PlanQty;
    private Double class6PlanQty;
    private Double class7PlanQty;
    private Double class8PlanQty;

    /**
     * 按班次字段模板动态读取值。
     *
     * @param fieldName Java 字段名
     * @return 字段值
     */
    public Object getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("斜裁调量字段不存在: " + fieldName, exception);
        }
    }
}
