package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.schedule.engine.mapper.ParamsEngineMapper;
import com.zlt.mix.schedule.engine.mapper.StockEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.ParamsEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.StockEngineService;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 引擎部分硫磺辅料参数相关ServiceImpl
 */
@Service
public class ParamsEngineServiceImpl implements ParamsEngineService {

    @Resource
    private ParamsEngineMapper paramsEngineMapper;

    /**
     * 获取硫磺辅料参数设置map
     * @param mixArea 密炼区
     * @return map，key--参数key， value--参数值
     */
    public Map<String, String> mapLhflParams(String mixArea) {
        List<LhflScheduleParams> paramList = paramsEngineMapper.listLhflParamsList(Arrays.asList(new String[]{EngineConstants.MIX_AREA_DEFAULT, mixArea}));
        Map<String, String> map = paramList.stream().collect(Collectors.toMap(LhflScheduleParams::getParamCode, LhflScheduleParams::getParamValue));
        return map;
    }

	/**
	 * 获取胶料参数设置map
	 * 
	 * @param mixArea 密炼区
	 * @return map，key--参数key， value--参数值
	 */
    @Override
	public Map<String, String> mapGlueParams(String mixArea) {
		return paramsEngineMapper.listGlueParams(mixArea).stream()
				.collect(Collectors.toMap(SettingScheduleParams::getParamCode, SettingScheduleParams::getParamValue));
	}
}
