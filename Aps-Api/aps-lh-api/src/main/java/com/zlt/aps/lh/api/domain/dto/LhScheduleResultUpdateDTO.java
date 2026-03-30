package com.zlt.aps.lh.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description 排程结果更新DTO
 * @date 2025/3/24
 */
@Data
public class LhScheduleResultUpdateDTO implements Serializable {

    private static final long serialVersionUID = -1589207140582788030L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "调整类型", name = "operType")
    private String operType;

    @ApiModelProperty(value = "一班计划量", name = "class1PlanQty")
    private Integer class1PlanQty;

    @ApiModelProperty(value = "一班原因分析", name = "class1Analysis")
    @ImportValidated(maxLength = 66)
    private String class1Analysis;

    @ApiModelProperty(value = "二班计划量", name = "class2PlanQty")
    private Integer class2PlanQty;

    @ApiModelProperty(value = "二班原因分析", name = "class2Analysis")
    @ImportValidated(maxLength = 66)
    private String class2Analysis;

    @ApiModelProperty(value = "三班计划量", name = "class3PlanQty")
    private Integer class3PlanQty;

    @ApiModelProperty(value = "三班原因分析", name = "class3Analysis")
    private String class3Analysis;

    @ApiModelProperty(value = "次日一班计划量", name = "class4PlanQty")
    private Integer class4PlanQty;

    @ApiModelProperty(value = "次日一班原因分析", name = "class4Analysis")
    private String class4Analysis;

    @ApiModelProperty(value = "次日二班计划量", name = "class5PlanQty")
    private Integer class5PlanQty;

    @ApiModelProperty(value = "次日二班原因分析", name = "class5Analysis")
    private String class5Analysis;

    @ApiModelProperty(value = "次日三班计划量", name = "class6PlanQty")
    private Integer class6PlanQty;

    @ApiModelProperty(value = "次日三班原因分析", name = "class6Analysis")
    private String class6Analysis;
}
