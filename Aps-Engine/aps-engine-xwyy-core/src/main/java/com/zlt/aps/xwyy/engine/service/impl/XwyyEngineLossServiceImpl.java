package com.zlt.aps.xwyy.engine.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineLossMapper;
import com.zlt.aps.xwyy.engine.service.XwyyEngineLossService;
import com.zlt.aps.xwyy.engine.vo.XwyyLossSettingVo;

/**
 * 损耗率服务实现类
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 17:32:01
 */
@Service("xwyyEngineLossService")
public class XwyyEngineLossServiceImpl implements XwyyEngineLossService {

	@Resource
	private XwyyEngineLossMapper xwyyEngineLossMapper;

	/**
	 * 
	 * 获取损耗率设定信息map
	 * 
	 * @return key:大卷编号#机台id，value：损耗率
	 */
	@Override
	public Map<String, Double> getLossRateMap() {
		Map<String, Double> lossMap = new HashMap<>();
		List<XwyyLossSettingVo> list = xwyyEngineLossMapper.listLossRate();
		for (XwyyLossSettingVo lossVo : list) {
			lossMap.put(lossVo.getLossKey(), lossVo.getLossRate());
		}
		return lossMap;
	}

	/**
	 * 获得损耗率 从损耗率表获取对应的损耗率，按以下优先级匹配：机台+物料编号 > 物料编号 > 机台 >默认值
	 *
	 * @param bigRollCode     大卷编号
	 * @param machineIds      机台id
	 * @param defaultLossRate 损耗率默认值
	 * @return
	 */
	@Override
	public double getLossRate(String bigRollCode, String machineIds, Map<String, Double> lossMap,
			double paramLossRate) {
		bigRollCode = (StringUtils.isBlank(bigRollCode) ? "" : bigRollCode);
		// 如果有没有机台或有多个机台，则耗损率为0
		if (StringUtils.isBlank(machineIds) || machineIds.indexOf(",") >= 0) {
			return BigDecimal.ZERO.doubleValue();
		}
		// 第一优先级：机台+大卷编号
		String key1 = machineIds + "#" + bigRollCode;
		// 第二优先级：大卷编号
		String key2 = "#" + bigRollCode;
		// 第三优先级：机台
		String key3 = machineIds + "#";
		// 按优先级取出损耗率
		Double lossRate = lossMap.getOrDefault(key1,
				lossMap.getOrDefault(key2, lossMap.getOrDefault(key3, paramLossRate)));
		// 把耗损率由百分比，转成对应小数
		return BigDecimalUtil.div(lossRate, 100D);
	}
}
