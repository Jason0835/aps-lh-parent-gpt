package com.zlt.aps.cd15.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 斜裁转机台请求。
 */
@Data
@ApiModel(value = "斜裁转机台请求")
public class Cd15TransferMachineRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工厂编码")
    private String factoryCode;
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;
    @ApiModelProperty("原机台编码")
    private String sourceMachineCode;
    @ApiModelProperty("目标机台编码")
    private String targetMachineCode;
    @ApiModelProperty("钢带代码")
    private String steelStripCode;
    @ApiModelProperty("起始班次字段")
    private String startClassField;
    @ApiModelProperty("一班目标机台插入顺序")
    private Integer class1ProduceOrder;
    @ApiModelProperty("二班目标机台插入顺序")
    private Integer class2ProduceOrder;
    @ApiModelProperty("三班目标机台插入顺序")
    private Integer class3ProduceOrder;
    @ApiModelProperty("四班目标机台插入顺序")
    private Integer class4ProduceOrder;
    @ApiModelProperty("五班目标机台插入顺序")
    private Integer class5ProduceOrder;
    @ApiModelProperty("六班目标机台插入顺序")
    private Integer class6ProduceOrder;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("是否已确认影响")
    private Boolean confirmed;

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
            throw new IllegalArgumentException("斜裁转机台字段不存在: " + fieldName, exception);
        }
    }
}
