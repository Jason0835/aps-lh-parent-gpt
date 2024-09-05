package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型库存对外暴露接口
 *
 * @author Joran.Zhang
 */
@FeignClient(contextId = "cxStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxStockService {

    /**
     * 获取成型库存信息列表
     *
     * @param cxStock
     * @return
     */
    @PostMapping("/cx/stock/list")
    TableDataInfo list(@RequestBody CxStock cxStock);

    /**
     * 删除成型库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/cx/stock/remove/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增成型库存信息
     *
     * @param cxStock
     * @return
     */
    @PostMapping("/cx/stock/add")
    AjaxResult add(@Validated @RequestBody CxStock cxStock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/cx/stock/selectCxStockById/{id}")
    CxStock selectCxStockById(@PathVariable("id") Long id);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/cx/stock/getInfo/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型库存信息
     *
     * @param cxStock
     * @return
     */
    @PutMapping("/cx/stock/edit")
    AjaxResult edit(@Validated @RequestBody CxStock cxStock);

    /**
     * 导出库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/cx/stock/exportList")
    List<CxStock> exportList(@RequestBody CxStock stock);

    /**
     * 导入数据
     */
    @PostMapping("/cx/stock/importData")
    public AjaxResult importData(@RequestBody List<CxStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 释放不可用库存
     * @param ids 待释放库存ID列表
     * @return
     */
    @PostMapping("/cx/stock/releaseStock")
    AjaxResult releaseStock(@RequestBody Long[] ids);
}
