package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "直裁大卷与机台映射", description = "直裁大卷与机台映射")
@TableName("t_cd90_machine_roll_mapping")
public class Cd90MachineRollMapping extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */

    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.machineRollMapping.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 钢压大卷代码 */

    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.machineRollMapping.bigRollCode")
    private String bigRollCode;

    /** 帘布代码 */

    @ImportExcelValidated(maxLength = 30)
    @TableField("CORD_FABRIC_CODE")
    @Excel(name = "ui.data.column.machineRollMapping.cordFabricCode")
    private String cordFabricCode;

    /** 机台编码 */

    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.machineRollMapping.machineCode")
    private String machineCode;
}