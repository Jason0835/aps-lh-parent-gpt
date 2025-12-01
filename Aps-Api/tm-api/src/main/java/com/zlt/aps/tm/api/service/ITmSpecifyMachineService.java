package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmSpecifyMachineDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面定点机台对外暴露接口
 */
@FeignClient(contextId = "iTmSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmSpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/tm/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody TmSpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/tm/specifyMachine/getSpecifyMachine/{id}")
    TmSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tm/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody TmSpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tm/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/tm/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/tm/specifyMachine/exportData")
    List<TmSpecifyMachineDto> exportData(@RequestBody TmSpecifyMachineDto dto);

    @PostMapping("/tm/specifyMachine/importData")
    @ApiOperation("导入胎面定点机台信息")
    public AjaxResult importData(@RequestBody List<TmSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
