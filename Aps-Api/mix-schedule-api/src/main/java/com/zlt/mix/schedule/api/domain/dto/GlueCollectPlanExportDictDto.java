package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author: Chen
 * @since: 2022/5/31 9:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GlueCollectPlanExportDictDto extends GlueCollectPlan {

    /**
     * 用于导出转换分厂字典项
     */
    private Map<String,String> factoryDictMap;

    /**
     * 用于导出转换密炼区字典项
     */
    private Map<String,String> mixAreaDictMap;
}
