package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author: Chen
 * @since: 2022/5/31 11:17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueScheduleResultExportDictDto extends GlueScheduleResult {

    /**
     * 用于导出转换配方阶段字典项
     */
    private Map<String, String> recipeStageDictMap;

    /**
     * 用于导出转换密炼区字典项
     */
    private Map<String, String> mixAreaDictMap;

    /**
     * 用于导出转换发布状态字典项
     */
    private Map<String, String> releaseStatusDictMap;
}
