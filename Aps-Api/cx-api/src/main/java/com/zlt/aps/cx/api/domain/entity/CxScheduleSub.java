package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 成型排程子表对象
 *
 * @author zlt
 * @date 2021-07-12
 */
@Data
@ApiModel(value = "成型排程子表对象", description = "成型排程子表对象")
public class CxScheduleSub extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    //半部件公共属性
    @ApiModelProperty(value = "半部件主键id")
    private Long id;
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;
    @ApiModelProperty(value = "机台ID")
    private String machineId;
    @ApiModelProperty(value = "机台名称")
    private String machineName;
    @ApiModelProperty(value = "原计划量提示标签")
    private String osLable;

    //15度裁断
    @ApiModelProperty(value = "1#钢带代码")
    private String cd15SteelStripCode1;
    @ApiModelProperty(value = "1#钢带中班(12点-24点)计划量")
    private Double cd15DayPlanQty1=0d;
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)计划量")
    private Double cd15NightPlanQty1=0d;

    //90度裁断
    @ApiModelProperty(value = "帘布代码1")
    private String cd90ClothCode;
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double cd90DayPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double cd90NightPlanQty=0d;

    //钢带压延
    @ApiModelProperty(value = "钢带大卷编号")
    private String gdyyBigRollCode;
    @ApiModelProperty(value = "中班")
    private Double gdyyClass1Plan=0d;
    @ApiModelProperty(value = "夜班")
    private Double gdyyClass2Plan=0d;
    @ApiModelProperty(value = "白班")
    private Double gdyyClass3Plan=0d;

    //钢丝圈
    @ApiModelProperty(value = "钢丝圈代码")
    private String gsqSteelRingCode;
    @ApiModelProperty(value = "中班(16点-24点)计划量(条)")
    private Double gsqMidPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-8点)计划量(条)")
    private Double gsqNightPlanQty=0d;
    @ApiModelProperty(value = "白班(8点-16点)计划量(条)")
    private Double gsqDayPlanQty=0d;

    //内衬
    @ApiModelProperty(value = "内衬代码")
    private String ncLiningCode;
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double ncDayPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double ncNightPlanQty=0d;

    //胎侧
    @ApiModelProperty(value = "胎侧代码")
    private String tcSidewallCode;
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double tcDayPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double tcNightPlanQty=0d;

    //胎面
    @ApiModelProperty(value = "胎面代码")
    private String tmTreadCode;
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double tmDayPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double tmNightPlanQty=0d;

    //胎圈
    @ApiModelProperty(value = "胎圈代码")
    private String tqBeadCode;
    @ApiModelProperty(value = "中班(16点-24点)计划量(条)")
    private Double tqMidPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-8点)计划量(条)")
    private Double tqNightPlanQty=0d;
    @ApiModelProperty(value = "白班(8点-16点)计划量(条)")
    private Double tqDayPlanQty=0d;
    @ApiModelProperty(value = "次日中班(16点-24点)计划量(条)")
    private Double tqNextMidPlanQty=0d;


    //纤维压延
    @ApiModelProperty(value = "帘布大卷编号")
    private String xwyyBigRollCode;
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double xwyyDayPlanQty=0d;
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double xwyyNightPlanQty=0d;

}
