package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90°裁断库存信息对外暴露接口
 */
@FeignClient(contextId = "iCd90StockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90StockService {

    /**
     * 获取90°裁断库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/cd90/stock/list")
    TableDataInfo list(@RequestBody Cd90Stock stock);

    /**
     * 删除90°裁断库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/cd90/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增90°裁断库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/cd90/stock")
    AjaxResult add(@Validated @RequestBody Cd90Stock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/cd90/stock/selectStockById/{id}")
    Cd90Stock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/cd90/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改90°裁断库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/cd90/stock")
    AjaxResult edit(@Validated @RequestBody Cd90Stock stock);

    /**
     * 导出90°裁断库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/cd90/stock/exportList")
    List<Cd90Stock> exportList(@RequestBody Cd90Stock stock);

    /**
     * 导入数据
     */
    @PostMapping("/cd90/stock/importData")
    public AjaxResult importData(@RequestBody List<Cd90Stock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
