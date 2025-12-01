package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分厂生产计划控制台查询列表-分厂排产版本信息对象
 *
 * @author ZLT
 * @date 20250314
 */
@Data
@ApiModel(value = "分厂生产计划控制台查询列表-分厂排产版本信息对象", description = "分厂生产计划控制台查询列表-分厂排产版本信息对象")
public class FactoryProductionVersionVo implements Serializable {
    /**
     * 初始化版本号
     */
    @ApiModelProperty(value = "分厂排产-初始化版本号-用以判断是否初始化", name = "initVersion")
    private String initVersion;
    /**
     * 模具排产版本号
     */
    @ApiModelProperty(value = "分厂排产-模具排产版本号-用以判断是否进行排产", name = "productionVersion")
    private String productionVersion;
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", name = "createTime")
    private Date createTime;
    /**
     * 是否定稿
     */
    @ApiModelProperty(value = "是否定稿", name = "isFinal")
    private Integer isFinal;
    /**
     * 月份排产起始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产起始日", name = "productionStartDate")
    private Date productionStartDate;
    /**
     * 0 不是自然月 1 是自然月
     */
    @ApiModelProperty(value = "0 不是自然月 1 是自然月", name = "isNaturalMonth")
    private Integer isNaturalMonth;
}
