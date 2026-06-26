package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 纤维压延预生产库存倍数配置Service接口
 *
 * @author chen
 * @date 2025-02-11
 */
@FeignClient(contextId = "IXwyyReserveStockSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyReserveStockService {

    /**
     * 查询纤维压延预生产库存倍数配置列表
     *
     * @param dto 纤维压延预生产库存倍数配置
     * @return 纤维压延预生产库存倍数配置集合
     */
    @PostMapping("/xwyy/reserveStock/list")
    @ApiOperation("查询纤维压延预生产库存倍数配置信息维护列表")
    public TableDataInfo list(@RequestBody XwyyReserveStockDto dto);

    /**
     * 查询纤维压延预生产库存倍数配置
     *
     * @param id 纤维压延预生产库存倍数配置ID
     * @return 纤维压延预生产库存倍数配置
     */
    @GetMapping("/xwyy/reserveStock/{id}")
    @ApiOperation("查询纤维压延预生产库存倍数配置信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyReserveStockDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改纤维压延预生产库存倍数配置
     *
     * @param dto 纤维压延预生产库存倍数配置
     * @return 结果
     */
    @PostMapping("/xwyy/reserveStock/edit")
    @ApiOperation("修改纤维压延预生产库存倍数配置（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody XwyyReserveStockDto dto);

    /**
     * 删除纤维压延预生产库存倍数配置
     *
     * @param ids 需要删除的纤维压延预生产库存倍数配置ID
     * @return 结果
     */
    @PostMapping("/xwyy/reserveStock/{ids}")
    @ApiOperation("删除纤维压延预生产库存倍数配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出纤维压延预生产库存倍数配置信息
     */
    @PostMapping("/xwyy/reserveStock/export")
    @ApiOperation("导出纤维压延预生产库存倍数配置信息")
    public List<XwyyReserveStockDto> exportData(@RequestBody XwyyReserveStockDto dto);


    @PostMapping("/xwyy/reserveStock/importData")
    @ApiOperation("导入纤维压延预生产库存倍数配置信息")
    public AjaxResult importData(@RequestBody List<XwyyReserveStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
