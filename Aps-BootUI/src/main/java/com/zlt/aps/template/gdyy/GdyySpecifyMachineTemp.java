package com.zlt.aps.template.gdyy;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 钢带压延定点机台表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Data
@ApiModel(value = "GdyySpecifyMachine对象", description = "钢带压延定点机台表")
public class GdyySpecifyMachineTemp extends ApsBaseEntity {

    @ApiModelProperty(value = "钢带大卷代码")
    @Excel(name = "ui.gdyy.specifyMachine.column.bigRollCode")
    private String bigRollCode;

    @Excel(name="ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "生产线")
    private String machineName;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    private String jobType;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
