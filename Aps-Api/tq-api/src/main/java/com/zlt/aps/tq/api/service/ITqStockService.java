package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈库存信息对外暴露接口
 */
@FeignClient(contextId = "iTqStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqStockService {

    /**
     * 获取胎圈库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody TqStock stock);

    /**
     * 删除胎圈库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增胎圈库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody TqStock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectStockById/{id}")
    TqStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎圈库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody TqStock stock);

    /**
     * 导出胎圈库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<TqStock> exportList(@RequestBody TqStock stock);

    @PostMapping("/stock/importData")
    @ApiOperation("导入胎圈定点机台信息")
    public AjaxResult importData(@RequestBody List<TqStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
