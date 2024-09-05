package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15°裁断库存信息对外暴露接口
 */
@FeignClient(contextId = "iCd15StockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15StockService {

    /**
     * 获取15°裁断库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody Cd15Stock stock);

    /**
     * 删除15°裁断库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增15°裁断库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody Cd15Stock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectStockById/{id}")
    Cd15Stock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改15°裁断库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody Cd15Stock stock);

    /**
     * 导出15°裁断库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<Cd15Stock> exportList(@RequestBody Cd15Stock stock);

    @PostMapping("/stock/importData")
    @ApiOperation("导入15度裁断库存信息")
    public AjaxResult importData(@RequestBody List<Cd15Stock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
