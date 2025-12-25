package com.zlt.aps.itf.scm.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
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

	/**
	 * 锁定订单池
	 * @param planedNotShipParamVo
	 * @return
	 */
	AjaxResult lockSalesOrderPool(SyncPlanedNotShipParamVo planedNotShipParamVo);

	/**
	 * 同步发货明细数据
	 * 
	 * @param syncOutShipDmdOrdVo
	 * @return
	 */
	AjaxResult syncOutShipDmdOrdList(SyncPlanedNotShipParamVo syncOutShipDmdOrdVo);

	/**
	 * 月计划排程结果推送
	 * 
	 * @param outFacScheduleVersionList
	 * @return
	 */
	AjaxResult publicFacScheduleVersion(List<SyncOutFacScheduleVersionVo> outFacScheduleVersionList);
}
