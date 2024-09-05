package com.zlt.aps.cx.api.domain.dto;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 班次准确率报表 明细 dto
 */
@Data
@ApiModel(value = "班次准确率报表明细dto", description = "班次准确率报表明细dto")
public class ReportClassAccuracyDto extends ApsBaseDto {

    public ReportClassAccuracyDto() {
    }

    public ReportClassAccuracyDto(String procedureCode, String class1PlanMaterial, Integer planClass1, String class1ActualMaterial, Integer actualClass1, String class2PlanMaterial, Integer planClass2, String class2ActualMaterial, Integer actualClass2, String class3PlanMaterial, Integer planClass3, String class3ActualMaterial, Integer actualClass3, Integer isSummary) {
        this.procedureCode = procedureCode;
        this.class1PlanMaterial = class1PlanMaterial;
        this.planClass1 = planClass1;
        this.class1ActualMaterial = class1ActualMaterial;
        this.actualClass1 = actualClass1;
        this.class2PlanMaterial = class2PlanMaterial;
        this.planClass2 = planClass2;
        this.class2ActualMaterial = class2ActualMaterial;
        this.actualClass2 = actualClass2;
        this.class3PlanMaterial = class3PlanMaterial;
        this.planClass3 = planClass3;
        this.class3ActualMaterial = class3ActualMaterial;
        this.actualClass3 = actualClass3;
        this.isSummary = isSummary;
    }

    @ApiModelProperty(value = "工序数据，0、硫化，1、成型，2、胎面，3、胎侧，4、内衬，5、胎圈，6、钢丝圈，7、15度裁断，8、90度裁断，9、钢带压延，10、纤维压延", position = 10)
    private String procedureCode;

    @ApiModelProperty(value = "排程日期,yyy-MM-dd格式", position = 20)
    private String scheduleDate;



    @ApiModelProperty(value = "中班(16:00-24:00)计划完成规格", position = 30)
    private String class1PlanMaterial;

    @ApiModelProperty(value = "中班(16:00-24:00)计划产量", position = 40)
    private Integer planClass1;

    @ApiModelProperty(value = "中班(16:00-24:00)实际完成规格", position = 50)
    private String class1ActualMaterial;

    @ApiModelProperty(value = "中班(16:00-24:00)实际产量", position = 60)
    private Integer actualClass1;



    @ApiModelProperty(value = "夜班(00:00-08:00 / 00:00-12:00)计划完成规格", position = 70)
    private String class2PlanMaterial;

    @ApiModelProperty(value = "夜班(00:00-08:00 / 00:00-12:00)计划产量", position = 80)
    private Integer planClass2;

    @ApiModelProperty(value = "夜班(00:00-08:00 / 00:00-12:00)实际完成规格", position = 90)
    private String class2ActualMaterial;

    @ApiModelProperty(value = "夜班(00:00-08:00 / 00:00-12:00)实际产量", position = 100)
    private Integer actualClass2;



    @ApiModelProperty(value = "白班(08:00-16:00 / 12:00-24:00)计划完成规格", position = 110)
    private String class3PlanMaterial;

    @ApiModelProperty(value = "白班(08:00-16:00 / 12:00-24:00)计划产量", position = 120)
    private Integer planClass3;

    @ApiModelProperty(value = "白班(08:00-16:00 / 12:00-24:00)实际完成规格", position = 130)
    private String class3ActualMaterial;

    @ApiModelProperty(value = "白班(08:00-16:00 / 12:00-24:00)实际产量", position = 140)
    private Integer actualClass3;


    @ApiModelProperty(value = "汇总记录标识，1-汇总记录（汇总记录在前端需要整行变色提醒）", position = 150)
    private Integer isSummary;


    @ApiModelProperty(value = "计划总规格数", position = 160)
    private Integer planSpecNum;

    @ApiModelProperty(value = "计划总产量", position = 170)
    private Integer planTotalNum;

    @ApiModelProperty(value = "实际总规格数", position = 180)
    private Integer actualSpecNum;

    @ApiModelProperty(value = "实际总产量", position = 190)
    private Integer actualTotalNum;

    @ApiModelProperty("用于导出使用的工序字典转换")
    private Map<String, String> procedureCodeMap;
}
