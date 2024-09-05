package com.zlt.aps.mps.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mps.mapper.MesLhInProductionSpecMapper;
import com.zlt.aps.mps.service.MesLhInProductionSpecService;

/**
 * 硫化机台当前生产规格服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-3-22 14:04:02
 */
@Service("mesLhInProductionSpecService")
public class MesLhInProductionSpecServiceImpl implements MesLhInProductionSpecService {
	@Autowired
	private MesLhInProductionSpecMapper mesLhInProductionSpecMapper;

	/**
	 * 合并数据
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	@Override
	@Transactional
	public AjaxResult mergeData(String dataVersion) {
		int rowCount = mesLhInProductionSpecMapper.checkHasData(dataVersion);
		if (rowCount == 0) {
			return AjaxResult.error(I18nUtil.getMessage("mes.error.message.data.empty"));
		}
		mesLhInProductionSpecMapper.deleteData(dataVersion);
		mesLhInProductionSpecMapper.mergeData(dataVersion);
		return AjaxResult.success();
	}

}
