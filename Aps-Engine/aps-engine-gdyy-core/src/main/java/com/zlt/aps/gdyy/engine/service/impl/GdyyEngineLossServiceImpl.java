package com.zlt.aps.gdyy.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineLossMapper;
import com.zlt.aps.gdyy.engine.service.GdyyEngineLossService;
import com.zlt.aps.gdyy.engine.vo.GdyyLossSettingVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 损耗率服务实现类
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 17:02:01
 */
@Service("gdyyEngineLossService")
public class GdyyEngineLossServiceImpl implements GdyyEngineLossService {

	@Resource
	private GdyyEngineLossMapper gdyyEngineLossMapper;

	/**
	 * 
	 * 获取损耗率设定信息map
	 * 
	 * @return key:大卷编号，value：损耗率
	 */
	@Override
	public Map<String, Double> getLossRateMap() {
		Map<String, Double> lossMap = new HashMap<>();
		List<GdyyLossSettingVo> list = gdyyEngineLossMapper.listLossRate();
		for (GdyyLossSettingVo lossVo : list) {
			lossMap.put(lossVo.getLossKey(), lossVo.getLossRate());
		}
		return lossMap;
	}

	/**
	 * 获得损耗率 从损耗率表获取对应的损耗率，按以下优先级匹配：物料编号 >默认值
	 *
	 * @param bigRollCode     大卷编号
	 * @param defaultLossRate 工序参数设置的损耗率默认值
	 * @return
	 */
	@Override
	public double getLossRate(String bigRollCode, String machineIds, Map<String, Double> lossMap, double paramLossRate) {
		/*bigRollCode = (StringUtils.isBlank(bigRollCode) ? "" : bigRollCode);
		// 获大卷的损耗率设定值
		double totalLoss = lossMap.getOrDefault(bigRollCode, paramLossRate);
		// 把耗损率由百分比，转成对应小数
		return BigDecimalUtil.div(totalLoss, 100D);*/
		bigRollCode = (StringUtils.isBlank(bigRollCode) ? "" : bigRollCode);
		// 如果有没有机台或有多个机台，则耗损率为0
		if (StringUtils.isBlank(machineIds) || machineIds.contains(",")) {
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
