package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqSpecifyMachineDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈定点机台对外暴露接口
 */
@FeignClient(contextId = "iGsqSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqSpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/gsq/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody GsqSpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/gsq/specifyMachine/getSpecifyMachine/{id}")
    GsqSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gsq/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody GsqSpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/gsq/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/gsq/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/gsq/specifyMachine/exportData")
    List<GsqSpecifyMachineDto> exportData(@RequestBody GsqSpecifyMachineDto dto);

    @PostMapping("/gsq/specifyMachine/importData")
    @ApiOperation("导入钢丝圈定点机台信息")
    public AjaxResult importData(@RequestBody List<GsqSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
