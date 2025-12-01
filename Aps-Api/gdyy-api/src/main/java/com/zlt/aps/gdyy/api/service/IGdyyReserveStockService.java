package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyReserveStockDto;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢带压延预生产库存倍数配置Service接口
 *
 * @author chen
 * @date 2025-02-11
 */
@FeignClient(contextId = "IGdyyReserveStockSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyReserveStockService {

    /**
     * 查询钢带压延预生产库存倍数配置列表
     *
     * @param dto 钢带压延预生产库存倍数配置
     * @return 钢带压延预生产库存倍数配置集合
     */
    @PostMapping("/gdyy/reserveStock/list")
    @ApiOperation("查询钢带压延预生产库存倍数配置信息维护列表")
    public TableDataInfo list(@RequestBody GdyyReserveStockDto dto);

    /**
     * 查询钢带压延预生产库存倍数配置
     *
     * @param id 钢带压延预生产库存倍数配置ID
     * @return 钢带压延预生产库存倍数配置
     */
    @GetMapping("/gdyy/reserveStock/{id}")
    @ApiOperation("查询钢带压延预生产库存倍数配置信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyyReserveStockDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢带压延预生产库存倍数配置
     *
     * @param dto 钢带压延预生产库存倍数配置
     * @return 结果
     */
    @PostMapping("/gdyy/reserveStock/edit")
    @ApiOperation("修改钢带压延预生产库存倍数配置（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody GdyyReserveStockDto dto);

    /**
     * 删除钢带压延预生产库存倍数配置
     *
     * @param ids 需要删除的钢带压延预生产库存倍数配置ID
     * @return 结果
     */
    @PostMapping("/gdyy/reserveStock/{ids}")
    @ApiOperation("删除钢带压延预生产库存倍数配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出钢带压延预生产库存倍数配置信息
     */
    @PostMapping("/gdyy/reserveStock/export")
    @ApiOperation("导出钢带压延预生产库存倍数配置信息")
    public List<GdyyReserveStockDto> exportData(@RequestBody GdyyReserveStockDto dto);


    @PostMapping("/gdyy/reserveStock/importData")
    @ApiOperation("导入钢带压延预生产库存倍数配置信息")
    public AjaxResult importData(@RequestBody List<GdyyReserveStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
