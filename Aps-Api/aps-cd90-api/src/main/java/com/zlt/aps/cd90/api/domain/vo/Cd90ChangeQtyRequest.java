package com.zlt.aps.cd90.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 直裁调量请求。
 */
@Data
@ApiModel(value = "直裁调量请求")
public class Cd90ChangeQtyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工厂编码")
    private String factoryCode;
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;
    @ApiModelProperty("排程结果ID，优先用于定位调量记录")
    private Long scheduleResultId;
    @ApiModelProperty("机台编码")
    private String machineCode;
    @ApiModelProperty("帘布代号")
    private String clothCode;
    @ApiModelProperty("单班调量班次，取值CLASS1至CLASS6")
    private String startClassField;
    @ApiModelProperty("单班目标计划量")
    private Double targetPlanQty;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("是否已确认跨班顺延影响")
    private Boolean confirmed;

    private Double class1PlanQty;
    private Double class2PlanQty;
    private Double class3PlanQty;
    private Double class4PlanQty;
    private Double class5PlanQty;
    private Double class6PlanQty;

    /**
     * 按班次字段模板动态读取值。
     *
     * @param fieldName Java字段名
     * @return 字段值
     */
    public Object getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("直裁调量字段不存在: " + fieldName, exception);
        }
    }

    /**
     * 按班次字段模板动态写入值。
     *
     * @param fieldName Java字段名
     * @param value 字段值
     */
    public void setFieldValueByFieldName(String fieldName, Object value) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("直裁调量字段不存在: " + fieldName, exception);
        }
    }
}
