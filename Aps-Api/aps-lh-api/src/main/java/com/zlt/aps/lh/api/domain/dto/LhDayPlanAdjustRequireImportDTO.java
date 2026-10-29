package com.zlt.aps.lh.api.domain.dto;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 硫化日计划调整需求导入请求。
 *
 * <p>工厂和年月取页面当前搜索条件，由 BootUI 随文件一起透传，
 * 用于校验 Excel 年月与页面年月一致，并把工厂作为调整记录的归属工厂。</p>
 */
@Data
@ApiModel(value = "硫化日计划调整需求导入请求", description = "硫化日计划调整需求导入请求")
public class LhDayPlanAdjustRequireImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 导入文件上下文 */
    @ApiModelProperty("导入文件上下文")
    private ImportContext importContext;

    /** 页面当前搜索工厂，作为导入数据的归属工厂 */
    @ApiModelProperty("页面当前搜索工厂")
    private String factoryCode;

    /** 页面当前搜索年月，格式 YYYYMM，用于与 Excel 年月整批一致性校验 */
    @ApiModelProperty("页面当前搜索年月")
    private Integer yearMonth;
}
