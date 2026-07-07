package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 斜裁大卷与机台映射实体。
 */
@Data
@ApiModel(value = "斜裁大卷与机台映射", description = "斜裁大卷与机台映射")
@TableName("t_cd15_machine_roll_mapping")
public class Cd15MachineRollMapping extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 钢压大卷编号 */
    @ApiModelProperty(value = "钢压大卷编号", name = "bigRollCode")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.bigRollCode")
    private String bigRollCode;

    /** 机台编码 */
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.machineCode")
    private String machineCode;

    /** 班次，多选时使用逗号分隔 */
    @ApiModelProperty(value = "班次", name = "shiftCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("SHIFT_CODE")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.shiftCode", dictType = "class_num")
    private String shiftCode;
}
