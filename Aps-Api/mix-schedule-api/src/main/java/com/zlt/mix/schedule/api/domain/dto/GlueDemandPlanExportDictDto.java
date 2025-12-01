package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 分厂胶料需求计划导出转换字典dto
 * @author: Chen
 * @since: 2022/5/27 11:43
 */
@Data
public class GlueDemandPlanExportDictDto extends GlueDemandPlan {
    /**
     * 用于导出转换分厂字典项
     */
    private Map<String,String> factoryDictMap;

    /**
     * 用于导出转换密炼区字典项
     */
    private Map<String,String> mixAreaDictMap;
}
