package com.zlt.aps.cd15.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;

/**
 * 15°裁断线边库存信息对外暴露接口
 */
@FeignClient(contextId = "iCd15LineSideStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15LineSideStockService {

	/**
	 * 获取15°裁断线边库存信息列表
	 *
	 * @param stock
	 * @return
	 */
	@PostMapping("/lineSideStock/list")
	TableDataInfo list(@RequestBody Cd15LineSideStock stock);

	/**
	 * 到MES同步15°裁断线边库存信息
	 *
	 * @param ids
	 * @return
	 */
	@PostMapping("/lineSideStock/syncStock")
	AjaxResult syncStock();

	/**
	 * 导出15°裁断库线边存信息
	 *
	 * @param stock
	 * @return
	 */
	@PostMapping("/lineSideStock/exportList")
	List<Cd15LineSideStock> exportList(@RequestBody Cd15LineSideStock stock);
}
