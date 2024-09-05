package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧库存信息对外暴露接口
 */
@FeignClient(contextId = "iTcStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcStockService {

    /**
     * 获取胎侧库存信息列表
     *
     * @param tcStock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody TcStock tcStock);

    /**
     * 删除胎侧库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增胎侧库存信息
     *
     * @param tcStock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody TcStock tcStock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectTcStockById/{id}")
    TcStock selectTcStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎侧库存信息
     *
     * @param tcStock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody TcStock tcStock);

    /**
     * 导出胎侧库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<TcStock> exportList(@RequestBody TcStock stock);

    /**
     * 数据导入
     */
    @PostMapping("/stock/importData")
    AjaxResult importData(@RequestBody List<TcStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


}
