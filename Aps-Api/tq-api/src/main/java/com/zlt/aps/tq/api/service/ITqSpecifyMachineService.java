package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqSpecifyMachineDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈定点机台对外暴露接口
 */
@FeignClient(contextId = "iTqSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqSpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/tq/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody TqSpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/tq/specifyMachine/getSpecifyMachine/{id}")
    TqSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tq/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody TqSpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tq/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/tq/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/tq/specifyMachine/exportData")
    List<TqSpecifyMachineDto> exportData(@RequestBody TqSpecifyMachineDto dto);

    @PostMapping("/tq/specifyMachine/importData")
    @ApiOperation("导入胎圈定点机台信息")
    public AjaxResult importData(@RequestBody List<TqSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
