package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延库存信息对外暴露接口
 */
@FeignClient(contextId = "iGdyyStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyStockService {

    /**
     * 获取钢带压延库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody GdyyStock stock);

    /**
     * 删除钢带压延库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增钢带压延库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody GdyyStock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectStockById/{id}")
    GdyyStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢带压延库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody GdyyStock stock);

    /**
     * 导出钢带压延库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<GdyyStock> exportList(@RequestBody GdyyStock stock);

    @PostMapping("/stock/importData")
    @ApiOperation("导入钢带压延库存信息")
    public AjaxResult importData(@RequestBody List<GdyyStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


    @GetMapping("/stock/isRollStock")
    @ApiOperation("判断是否按大卷计算库存")
    public Boolean isRollStock();
}
