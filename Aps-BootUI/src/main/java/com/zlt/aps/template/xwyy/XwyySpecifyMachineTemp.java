package com.zlt.aps.template.xwyy;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 纤维压延定点机台表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Data
@ApiModel(value = "TmSpecifyMachine对象", description = "纤维压延定点机台表")
public class XwyySpecifyMachineTemp  {

    @ApiModelProperty(value = "帘布大卷代码")
    @Excel(name = "ui.xwyy.specifyMachine.column.bigRollCode")
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
