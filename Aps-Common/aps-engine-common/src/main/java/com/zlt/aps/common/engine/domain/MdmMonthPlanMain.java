package com.zlt.aps.common.engine.domain;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
  * 月度计划主表
  * @ClassName MdmMonthPlanMain
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/22 16:07
  * @Version 1.0
**/
@Data
@ApiModel(value = "planmain对象", description = "planmain对象 ")
public class MdmMonthPlanMain extends ApsBaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 主键ID */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 生产排程记录主计划版本号,年+月+日+01，02 */
    @Excel(name = "生产排程记录主计划版本号,年+月+日+01，02")
    @ApiModelProperty(value = "生产排程记录主计划版本号,年+月+日+01，02")
    private String monthPlanApsVersion;

    /** 主计划版本号 */
    @Excel(name = "主计划版本号")
    @ApiModelProperty(value = "主计划版本号")
    private String monthPlanVersion;

    /** 主计划所属年份 */
    @Excel(name = "主计划所属年份")
    @ApiModelProperty(value = "主计划所属年份")
    private String year;

    /** 主计划所属月份 */
    @Excel(name = "主计划所属月份")
    @ApiModelProperty(value = "主计划所属月份")
    private String month;

    /** 如果不是定稿就是初稿,0-定稿；1初稿 */
    @Excel(name = "如果不是定稿就是初稿,0-定稿；1初稿")
    @ApiModelProperty(value = "如果不是定稿就是初稿,0-定稿；1初稿")
    private String isFinalized;

}
