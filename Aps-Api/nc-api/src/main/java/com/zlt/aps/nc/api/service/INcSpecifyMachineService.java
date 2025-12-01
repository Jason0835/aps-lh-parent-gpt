package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcSpecifyMachineDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬定点机台对外暴露接口
 */
@FeignClient(contextId = "iNcSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.nc:nc}")
public interface INcSpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/nc/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody NcSpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/nc/specifyMachine/getSpecifyMachine/{id}")
    NcSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/nc/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody NcSpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/nc/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/nc/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/nc/specifyMachine/exportData")
    List<NcSpecifyMachineDto> exportData(@RequestBody NcSpecifyMachineDto dto);

    @PostMapping("/nc/specifyMachine/importData")
    @ApiOperation("导入内衬定点机台信息")
    public AjaxResult importData(@RequestBody List<NcSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
