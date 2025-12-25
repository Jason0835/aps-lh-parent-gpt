//package com.zlt.aps.cd15.common.handle;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import com.alibaba.fastjson.JSONObject;
//import com.ruoyi.common.core.web.domain.AjaxResult;
//import com.zlt.aps.common.engine.service.FactoryService;
//import com.zlt.aps.common.engine.utils.DateUtil;
//import com.zlt.sync.handle.SyncDataHandle;
//import com.zlt.sync.povo.SyncParamsVO;
//
//@Component
//public class Cd15SyncDataHandle extends SyncDataHandle {
//	@Autowired
//	private FactoryService factoryService;
//
//	/**
//	 * 到MES请求15°裁断线边库库存
//	 * 
//	 * @return
//	 */
//	public AjaxResult syncLineSideStock() {
//		String factoryCode = factoryService.getFactoryCode();
//		String companyCode = factoryService.getCompanyCode();
//		SyncParamsVO paramsVO = new SyncParamsVO();
//		paramsVO.setSyncKey("ADJUDI15_LINESIDE_STOCK");
//		JSONObject json = new JSONObject();
//		json.put("factoryCode", factoryCode);
//		json.put("companyCode", companyCode);
//		json.put("endDate", DateUtil.nowDate());
//		paramsVO.setParams(json);
//		paramsVO.setFactoryCode(factoryCode);
//		paramsVO.setCompanyCode(companyCode);
//		AjaxResult result = this.syncRequest(paramsVO); // 发送请求
//		result.put(AjaxResult.DATA_TAG, paramsVO.getDataVersion());
//		return result;
//	}
//
//	@Override
//	public void asyncResult(AjaxResult ajaxResult) {
//
//	}
//}
