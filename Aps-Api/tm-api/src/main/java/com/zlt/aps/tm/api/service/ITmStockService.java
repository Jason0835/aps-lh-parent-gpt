package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面库存信息对外暴露接口
 */
@FeignClient(contextId = "iTmStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmStockService {

    /**
     * 获取胎面库存信息列表
     * @param stock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody TmStock stock);
	
	/**
     * 删除胎面库存信息
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);
	
	/**
     * 新增胎面库存信息
     * @param tTmStock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody TmStock tTmStock);


    /**
     * 根据ID获取详细信息
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectTmStockById/{id}")
    TmStock selectTmStockById(@PathVariable("id") Long id);
	
	/**
     * 导出胎面库存信息
     */
  /*  @PostMapping("/stock/export")
    void export(HttpServletResponse response, TTmStock tTmStock) throws IOException;*/

    /**
     * 根据ID获取详细信息
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎面库存信息
     * @param tTmStock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody TmStock tTmStock);

    /**
     * 导出胎面库存信息列表
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<TmStock> exportList(@RequestBody TmStock stock);

    @PostMapping("/stock/importData")
    @ApiOperation("导入胎面定点机台信息")
    public AjaxResult importData(@RequestBody List<TmStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
