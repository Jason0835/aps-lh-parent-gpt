package com.zlt.aps.cd15.engine.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineLossMapper;
import com.zlt.aps.cd15.engine.service.Cd15EngineLossService;
import com.zlt.aps.cd15.engine.vo.Cd15LossSettingVo;
import com.zlt.aps.common.core.utils.BigDecimalUtil;

/**
 * 损耗率服务实现类
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 14:02:01
 */
@Service("cd15EngineLossService")
public class Cd15EngineLossServiceImpl implements Cd15EngineLossService {

	@Resource
	private Cd15EngineLossMapper cd15EngineLossMapper;

	/**
	 * 
	 * 获取损耗率设定信息map
	 * 
	 * @return key:钢带编号#机台id，value：损耗率
	 */
	@Override
	public Map<String, Double> getLossRateMap() {
		Map<String, Double> lossMap = new HashMap<>();
		List<Cd15LossSettingVo> list = cd15EngineLossMapper.listLossRate();
		for (Cd15LossSettingVo lossVo : list) {
			lossMap.put(lossVo.getLossKey(), lossVo.getLossRate());
		}
		return lossMap;
	}

	/**
	 * 获得损耗率 从损耗率表获取对应的损耗率，按以下优先级匹配：机台+物料编号 > 物料编号 > 机台 >默认值
	 *
	 * @param steelStripCode  钢带编号
	 * @param machineIds      机台id
	 * @param defaultLossRate 工序参数设置的损耗率默认值
	 * @return
	 */
	@Override
	public double getLossRate(String steelStripCode, String machineIds, Map<String, Double> lossMap,
			double paramLossRate) {
		steelStripCode = (StringUtils.isBlank(steelStripCode) ? "" : steelStripCode);
		// 如果有没有机台或有多个机台，则耗损率为0
		String machineId = "";
		if (StringUtils.isNotBlank(machineIds) && machineIds.indexOf(",") < 0) {
		    machineId = machineIds;
		}
		// 第一优先级：机台+钢带编号
		String key1 = machineId + "#" + steelStripCode;
		// 第二优先级：钢带编号
		String key2 = "#" + steelStripCode;
		// 第三优先级：机台
		String key3 = machineId + "#";
		// 按优先级取出损耗率
		Double lossRate = lossMap.getOrDefault(key1,
				lossMap.getOrDefault(key2, lossMap.getOrDefault(key3, paramLossRate)));
		// 把耗损率由百分比，转成对应小数
		return BigDecimalUtil.div(lossRate, 100D);
	}
}
