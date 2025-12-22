package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.sync.domain.AuxReqSyncDataLogs;

/**
 * MES接口-Bom相关接口
 * 
 * @author zlt
 * @since 2025/12/19
 */
public interface MesBomItfService {
	/**
	 * 同步产月度计划及硫化施工信息同步接口（SKU与施工关系表）
	 *
	 * @param syncDataLogs 同步参数
	 * @return 结果
	 */
	AjaxResult syncLhConstructionInfo(AuxReqSyncDataLogs syncDataLogs);

	/**
	 * 半部件BOM接口
	 *
	 * @param syncDataLogs 同步参数
	 * @return 结果
	 */
	AjaxResult syncConstructionInfo(AuxReqSyncDataLogs syncDataLogs);

	/**
	 * 成型及半部件BOM施工信息同步
	 *
	 * @param syncDataLogs 同步参数
	 * @return 结果
	 */
	AjaxResult syncBomInfo(AuxReqSyncDataLogs syncDataLogs);

}
