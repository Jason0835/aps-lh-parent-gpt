package com.zlt.mix.schedule.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;

/**
 * 参数设置相关mapper
 */
public interface ParamsEngineMapper {

    /**
     * 获取硫磺辅料参数设置列表
     * @param mixAreas
     * @return
     */
    List<LhflScheduleParams> listLhflParamsList(@Param("mixAreas") List<String> mixAreas);
    
	/**
	 * 加载胶料排程参数设置
	 * 
	 * @param mixArea 密炼区
	 * @return
	 */
	List<SettingScheduleParams> listGlueParams(@Param("mixArea") String mixArea);
}
