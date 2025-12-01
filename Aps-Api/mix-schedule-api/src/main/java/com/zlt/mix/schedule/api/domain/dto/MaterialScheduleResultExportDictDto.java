package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author: Chen
 * @since: 2022/5/31 16:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialScheduleResultExportDictDto extends MaterialScheduleResult {

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

    /**
     * 用于导出转换是否常用字典项
     */
    private Map<String, String> isOrNotDictMap;

    /**
     * 用于导出转换班制字典项
     */
    private Map<String, String> classShiftDictMap;
}
