package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬库存信息对外暴露接口
 */
@FeignClient(contextId = "iNcStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcStockService {

    /**
     * 获取内衬库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock/list")
    TableDataInfo list(@RequestBody NcStock stock);

    /**
     * 删除内衬库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/nc/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增内衬库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock")
    AjaxResult add(@Validated @RequestBody NcStock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/stock/selectStockById/{id}")
    NcStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改内衬库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/nc/stock")
    AjaxResult edit(@Validated @RequestBody NcStock stock);

    /**
     * 导出内衬库存信息
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock/exportList")
    List<NcStock> exportList(@RequestBody NcStock stock);

    @PostMapping("/nc/stock/importData")
    @ApiOperation("导入内衬定点机台信息")
    public AjaxResult importData(@RequestBody List<NcStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
