package com.zlt.aps.cd90.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 直裁插单请求。
 */
@Data
@ApiModel(value = "直裁插单请求")
public class Cd90InsertOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工厂编码")
    private String factoryCode;
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;
    @ApiModelProperty("机台编码")
    private String machineCode;
    @ApiModelProperty("帘布代号")
    private String clothCode;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("是否已确认跨班顺延影响")
    private Boolean confirmed;

    private Double class1PlanQty;
    private Integer class1ProduceOrder;
    private String class1AnalysisInput;
    private Double class2PlanQty;
    private Integer class2ProduceOrder;
    private String class2AnalysisInput;
    private Double class3PlanQty;
    private Integer class3ProduceOrder;
    private String class3AnalysisInput;
    private Double class4PlanQty;
    private Integer class4ProduceOrder;
    private String class4AnalysisInput;
    private Double class5PlanQty;
    private Integer class5ProduceOrder;
    private String class5AnalysisInput;
    private Double class6PlanQty;
    private Integer class6ProduceOrder;
    private String class6AnalysisInput;

    /**
     * 按班次字段模板动态读取值，避免为CLASS1至CLASS6编写switch。
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
            throw new IllegalArgumentException("直裁插单字段不存在: " + fieldName, exception);
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
            throw new IllegalArgumentException("直裁插单字段不存在: " + fieldName, exception);
        }
    }
}
