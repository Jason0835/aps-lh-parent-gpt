package com.zlt.aps.monthplan.api.domain.dto;

import com.zlt.aps.monthplan.api.domain.vo.MpAdjustStructureInVo;
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

    /**
     * 调整类型 01-结构内，02-结构延长，03-结构缩短，04-新增结构
     */
    @ApiModelProperty(value = "调整类型")
    private String adjustType;

    @ApiModelProperty(value = "结构内调整记录")
    private List<MpAdjustStructureInVo> mpAdjustStructureInList;

}
