package com.zlt.aps.itf.scm.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.utils.AppUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;
import com.zlt.aps.itf.util.ItfSyncDataHandle;
import com.zlt.aps.itf.util.PostMethodUtils;
import com.zlt.sync.povo.SyncParamsVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ScmItfServiceImpl implements ScmItfService {
	@Autowired
	private ItfSyncDataHandle itfSyncDataHandle;

	@Value("itf:scm:syncPlanedNotShipList:url")
	private String SYNC_PLANED_NOTSHIP_URL;

	/**
	 * 同步已计划未发货数据
	 * 
	 * @param planedNotShipParamVo
	 * @return
	 */
	public AjaxResult syncPlanedNotShipList(SyncPlanedNotShipParamVo planedNotShipParamVo) {
		AjaxResult ajaxResult = null;
		try {
			if (planedNotShipParamVo == null) {
				ajaxResult = AjaxResult.error("传入参数为空");
				return ajaxResult;
			}
			// 调用供应链接口获取数据
			String result = PostMethodUtils.sendPost(SYNC_PLANED_NOTSHIP_URL,
					JSONObject.toJSONString(planedNotShipParamVo), null);
			// 校验数据格式是否合法
			if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
				String errorMsg = "syncPlanedNotShipList 返回数据格式校验失败：" + result;
				log.error(errorMsg);
				ajaxResult = AjaxResult.error(errorMsg);
				return ajaxResult;
			}
			ajaxResult = JSONObject.parseObject(result, AjaxResult.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Integer status = AppUtils.checkAjaxSuccess(ajaxResult) ? ApsConstant.SYNC_STATUS_6
					: ApsConstant.SYNC_STATUS_3;
			// 记录同步记录
			SyncParamsVO paramsVO = new SyncParamsVO();
			paramsVO.setSyncKey(ApsConstant.SYNC_PLANED_NOT_SHIP);
			paramsVO.setParams(new JSONObject());
			paramsVO.setFactoryCode(planedNotShipParamVo.getFactory());
			paramsVO.setCompanyCode(planedNotShipParamVo.getFactory());
			paramsVO.setStatus(status);
			paramsVO.setNoMq(ApsConstant.APS_YES_NO_1); // 不发送mq，仅记录日志
			itfSyncDataHandle.syncRequest(paramsVO); // 记录请求日志
		}
		return ajaxResult;
	}
}
