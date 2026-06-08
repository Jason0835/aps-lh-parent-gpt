package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjSpecifyMachineDto;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶定点机台对外暴露接口
 */
@FeignClient(contextId = "iNcSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjSpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/dj/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody DjSpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/dj/specifyMachine/getSpecifyMachine/{id}")
    DjSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody DjSpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/dj/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/dj/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/dj/specifyMachine/exportData")
    List<DjSpecifyMachineDto> exportData(@RequestBody DjSpecifyMachineDto dto);

    @PostMapping("/dj/specifyMachine/importData")
    @ApiOperation("导入垫胶定点机台信息")
    public AjaxResult importData(@RequestBody List<DjSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
