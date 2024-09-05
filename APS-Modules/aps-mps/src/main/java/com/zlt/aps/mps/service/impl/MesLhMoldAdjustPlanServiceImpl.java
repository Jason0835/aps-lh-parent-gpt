package com.zlt.aps.mps.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mps.mapper.MesLhMoldAdjustPlanMapper;
import com.zlt.aps.mps.service.MesLhMoldAdjustPlanService;

/**
 * 硫化工序模具调整计划接口服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-3-22 14:04:07
 */
@Service("mesLhMoldAdjustPlanService")
public class MesLhMoldAdjustPlanServiceImpl implements MesLhMoldAdjustPlanService {
	@Autowired
	private MesLhMoldAdjustPlanMapper mesLhMoldAdjustPlanMapper;

	/**
	 * 合并数据
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	@Override
	@Transactional
	public AjaxResult mergeData(String dataVersion) {
		int rowCount = mesLhMoldAdjustPlanMapper.checkHasData(dataVersion);
		if (rowCount == 0) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		mesLhMoldAdjustPlanMapper.deleteData(dataVersion);
		mesLhMoldAdjustPlanMapper.mergeData(dataVersion);
		return AjaxResult.success();
	}

}
