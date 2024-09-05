package com.zlt.aps.cd90.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;

/**
 * 90°裁断线边库存信息对外暴露接口
 */
@FeignClient(contextId = "iCd90LineSideStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90LineSideStockService {

	/**
	 * 获取90°裁断线边库存信息列表
	 *
	 * @param stock
	 * @return
	 */
	@PostMapping("/lineSideStock/list")
	TableDataInfo list(@RequestBody Cd90LineSideStock stock);

	/**
	 * 到MES同步90°裁断线边库存信息
	 *
	 * @param ids
	 * @return
	 */
	@PostMapping("/lineSideStock/syncStock")
	AjaxResult syncStock();

	/**
	 * 导出90°裁断库线边存信息
	 *
	 * @param stock
	 * @return
	 */
	@PostMapping("/lineSideStock/exportList")
	List<Cd90LineSideStock> exportList(@RequestBody Cd90LineSideStock stock);
}
