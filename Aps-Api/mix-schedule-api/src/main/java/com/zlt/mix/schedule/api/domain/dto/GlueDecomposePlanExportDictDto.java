package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author: Chen
 * @since: 2022/5/31 10:27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDecomposePlanExportDictDto extends GlueDecomposePlan {

    /**
     * 用于导出转换密炼区字典项
     */
    private Map<String,String> mixAreaDictMap;

    /**
     * 用于导出转换收尾计划字典项
     */
    private Map<String, String> isFinishingDictMap;
}
