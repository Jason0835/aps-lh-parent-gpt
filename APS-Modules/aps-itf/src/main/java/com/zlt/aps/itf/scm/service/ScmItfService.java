package com.zlt.aps.itf.scm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;

/**
 * SCM接口
 *
 * @author zlt
 * @since 2025/12/10
 */
public interface ScmItfService {
	/**
	 * 同步已计划未发货数据
	 * 
	 * @param planedNotShipParamVo
	 * @return
	 */
	AjaxResult syncPlanedNotShipList(SyncPlanedNotShipParamVo planedNotShipParamVo);
}
