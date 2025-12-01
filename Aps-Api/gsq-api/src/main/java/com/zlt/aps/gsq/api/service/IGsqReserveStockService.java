package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢丝圈预生产库存倍数配置Service接口
 *
 * @author chen
 * @date 2025-02-11
 */
@FeignClient(contextId = "IGsqReserveStockSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqReserveStockService {

    /**
     * 查询钢丝圈预生产库存倍数配置列表
     *
     * @param dto 钢丝圈预生产库存倍数配置
     * @return 钢丝圈预生产库存倍数配置集合
     */
    @PostMapping("/gsq/reserveStock/list")
    @ApiOperation("查询钢丝圈预生产库存倍数配置信息维护列表")
    public TableDataInfo list(@RequestBody GsqReserveStockDto dto);

    /**
     * 查询钢丝圈预生产库存倍数配置
     *
     * @param id 钢丝圈预生产库存倍数配置ID
     * @return 钢丝圈预生产库存倍数配置
     */
    @GetMapping("/gsq/reserveStock/{id}")
    @ApiOperation("查询钢丝圈预生产库存倍数配置信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqReserveStockDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈预生产库存倍数配置
     *
     * @param dto 钢丝圈预生产库存倍数配置
     * @return 结果
     */
    @PostMapping("/gsq/reserveStock/edit")
    @ApiOperation("修改钢丝圈预生产库存倍数配置（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody GsqReserveStockDto dto);

    /**
     * 删除钢丝圈预生产库存倍数配置
     *
     * @param ids 需要删除的钢丝圈预生产库存倍数配置ID
     * @return 结果
     */
    @PostMapping("/gsq/reserveStock/{ids}")
    @ApiOperation("删除钢丝圈预生产库存倍数配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出钢丝圈预生产库存倍数配置信息
     */
    @PostMapping("/gsq/reserveStock/export")
    @ApiOperation("导出钢丝圈预生产库存倍数配置信息")
    public List<GsqReserveStockDto> exportData(@RequestBody GsqReserveStockDto dto);


    @PostMapping("/gsq/reserveStock/importData")
    @ApiOperation("导入钢丝圈预生产库存倍数配置信息")
    public AjaxResult importData(@RequestBody List<GsqReserveStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
