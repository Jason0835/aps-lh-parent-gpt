package com.zlt.aps.cd90.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * 直裁转机台请求。
 */
@Data
@ApiModel(value = "直裁转机台请求")
public class Cd90TransferMachineRequest implements Serializable {

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
    @ApiModelProperty("帘布代号")
    private String clothCode;
    @ApiModelProperty("开始转走班次，取值CLASS1至CLASS6")
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
    @ApiModelProperty("是否已确认跨班顺延影响")
    private Boolean confirmed;

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
            throw new IllegalArgumentException("直裁转机台字段不存在: " + fieldName, exception);
        }
    }
}
