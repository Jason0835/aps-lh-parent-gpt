package com.zlt.aps.gdyy.engine.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineLossMapper;
import com.zlt.aps.gdyy.engine.service.GdyyEngineLossService;
import com.zlt.aps.gdyy.engine.vo.GdyyLossSettingVo;

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
			lossMap.put(lossVo.getBigRollCode(), lossVo.getLossRate());
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
	public double getLossRate(String bigRollCode, Map<String, Double> lossMap, double paramLossRate) {
		bigRollCode = (StringUtils.isBlank(bigRollCode) ? "" : bigRollCode);
		// 获大卷的损耗率设定值
		double totalLoss = lossMap.getOrDefault(bigRollCode, paramLossRate);
		// 把耗损率由百分比，转成对应小数
		return BigDecimalUtil.div(totalLoss, 100D);
	}
}
