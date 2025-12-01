package com.zlt.mix.schedule.engine.service.basicdata;

import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 引擎部分硫磺辅料参数相关Service
 */
public interface ParamsEngineService {

    /**
     * 获取硫磺辅料参数设置map
     * @param mixArea 密炼区
     * @return map，key--参数key， value--参数值
     */
    Map<String, String> mapLhflParams(String mixArea);


	/**
	 * 获取胶料参数设置map
	 * 
	 * @param mixArea 密炼区
	 * @return map，key--参数key， value--参数值
	 */
	Map<String, String> mapGlueParams(String mixArea);
}
