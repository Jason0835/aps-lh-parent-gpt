package com.zlt.aps.mps.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mps.mapper.MesCxInProductionSpecMapper;
import com.zlt.aps.mps.service.MesCxInProductionSpecService;

/**
 * 成型机台当前生产规格接口服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 13:05:41
 */
@Service("mesCxInProductionSpecService")
public class MesCxInProductionSpecServiceImpl implements MesCxInProductionSpecService {

	@Resource
	private MesCxInProductionSpecMapper mesCxInProductionSpecMapper;

	/**
	 * 完成量同步
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	@Override
	public AjaxResult mergeSpes(String dataVersion) {
		// 统计数据版本的数据量
		int dataCount = mesCxInProductionSpecMapper.countSyncData(dataVersion);
		if (dataCount == 0) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		mesCxInProductionSpecMapper.mergeSpec(dataVersion);
		return AjaxResult.success();
	}

}
