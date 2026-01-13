package com.zlt.aps.monthplan.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustResult;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 周程滚动调整DTO
 * @author wengpc
 */
@Data
public class MpWeekRollAdjustDTO implements Serializable {

    @ApiModelProperty(value = "工厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "月度计划年份")
    private Integer mpYear;

    @ApiModelProperty(value = "月度计划月份")
    private Integer mpMonth;

    @ApiModelProperty(value = "月度计划年月")
    private Integer yearMonth;

    @ApiModelProperty(value = "产品结构")
    private String structureName;

    @ApiModelProperty(value = "版本号")
    private String version;

    /** 排产机台,多个机台用逗号分隔 */
    @ApiModelProperty(value = "排产机台")
    private String scheduledMachines;


    /**
     * 调整类型 01-结构内，02-结构外
     */
    @ApiModelProperty(value = "调整类型")
    private String adjustType;

    @ApiModelProperty(value = "调整明细列表")
    private List<MpAdjustDetailVo> mpAdjustStructureInList;

    @ApiModelProperty(value = "调整结果列表")
    private List<MpAdjustResult> adjustResultList;

}
