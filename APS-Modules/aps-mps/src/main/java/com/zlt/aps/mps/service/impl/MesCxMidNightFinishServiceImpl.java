package com.zlt.aps.mps.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mps.mapper.MesCxMidNightFinishMapper;
import com.zlt.aps.mps.service.MesCxMidNightFinishService;

/**
 * 成型中夜班完成量接口服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 10:45:28
 */
@Service("mesCxMidNightFinishService")
public class MesCxMidNightFinishServiceImpl implements MesCxMidNightFinishService {

	@Resource
	private MesCxMidNightFinishMapper mesCxMidNightFinishMapper;

	/**
	 * 完成量同步
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	@Override
	public AjaxResult mergeFinishQty(String dataVersion) {
		// 统计数据版本的数据量
		int dataCount = mesCxMidNightFinishMapper.countSyncData(dataVersion);
		if (dataCount == 0) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		mesCxMidNightFinishMapper.mergeFinishQty(dataVersion);
		return AjaxResult.success();
	}

}
