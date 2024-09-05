package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈库存信息对外暴露接口
 */
@FeignClient(contextId = "iGsqStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqStockService {

    /**
     * 获取钢丝圈库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody GsqStock stock);

    /**
     * 删除钢丝圈库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增钢丝圈库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody GsqStock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectStockById/{id}")
    GsqStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody GsqStock stock);

    /**
     * 导出钢丝圈库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<GsqStock> exportList(@RequestBody GsqStock stock);

    @PostMapping("/stock/importData")
    @ApiOperation("导入钢丝圈库存信息")
    public AjaxResult importData(@RequestBody List<GsqStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
