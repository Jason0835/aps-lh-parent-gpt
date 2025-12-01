package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 排程操作日志导出用于转换字典对象
 * @author: Chen
 * @since: 2022/7/13 11:34
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value="排程操作日志导出用于转换字典对象", description="排程操作日志导出用于转换字典对象")
public class ScheduleOperLogDto extends ScheduleOperLog {

    /**
     * 用于导出转换排程类型字典项
     */
    private Map<String, String> scheduleTypeDictMap;

    /**
     * 用于导出转换操作类型字典项
     */
    private Map<String, String> operTypeDictMap;

    /**
     * 用于导出转换密炼区字典项
     */
    private Map<String, String> mixAreaDictMap;

    /**
     * 用于导出转换配方阶段字典项
     */
    private Map<String, String> recipeStageDictMap;
}
