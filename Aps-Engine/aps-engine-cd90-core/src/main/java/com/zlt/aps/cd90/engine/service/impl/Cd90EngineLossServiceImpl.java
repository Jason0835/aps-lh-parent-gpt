package com.zlt.aps.cd90.engine.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineLossMapper;
import com.zlt.aps.cd90.engine.service.Cd90EngineLossService;
import com.zlt.aps.cd90.engine.vo.Cd90LossSettingVo;
import com.zlt.aps.common.core.utils.BigDecimalUtil;

/**
 * 损耗率服务实现类
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 16:02:01
 */
@Service("cd90EngineLossService")
public class Cd90EngineLossServiceImpl implements Cd90EngineLossService {

	@Resource
	private Cd90EngineLossMapper cd90EngineLossMapper;

	/**
	 * 
	 * 获取损耗率设定信息map
	 * 
	 * @return key:钢带编号#机台id，value：损耗率
	 */
	@Override
	public Map<String, Double> getLossRateMap() {
		Map<String, Double> lossMap = new HashMap<>();
		List<Cd90LossSettingVo> list = cd90EngineLossMapper.listLossRate();
		for (Cd90LossSettingVo lossVo : list) {
			lossMap.put(lossVo.getLossKey(), lossVo.getLossRate());
		}
		return lossMap;
	}

	/**
	 * 获得损耗率 从损耗率表获取对应的损耗率，按以下优先级匹配：机台+物料编号 > 物料编号 > 机台 >默认值
	 *
	 * @param clothCode       帘布编号
	 * @param machineIds      机台id
	 * @param defaultLossRate 工序参数设置的损耗率默认值
	 * @return
	 */
	@Override
	public double getLossRate(String clothCode, String machineIds, Map<String, Double> lossMap, double paramLossRate) {
		clothCode = (StringUtils.isBlank(clothCode) ? "" : clothCode);
		// 如果有没有机台或者存在多个机台，则耗损率为0
		if (StringUtils.isBlank(machineIds) || machineIds.indexOf(",") >= 0) {
			return BigDecimal.ZERO.doubleValue();
		}
		// 第一优先级：机台+帘布编号
		String key1 = machineIds + "#" + clothCode;
		// 第二优先级：帘布编号
		String key2 = "#" + clothCode;
		// 第三优先级：机台
		String key3 = machineIds + "#";
		// 按优先级取出损耗率
		Double lossRate = lossMap.getOrDefault(key1,
				lossMap.getOrDefault(key2, lossMap.getOrDefault(key3, paramLossRate)));
		// 把耗损率由百分比，转成对应小数
		return BigDecimalUtil.div(lossRate, 100D);
	}
}
