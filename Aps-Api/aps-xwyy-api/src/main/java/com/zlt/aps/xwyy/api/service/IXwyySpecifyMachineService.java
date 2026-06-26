package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyySpecifyMachineDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延定点机台对外暴露接口
 */
@FeignClient(contextId = "iXwyySpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyySpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/xwyy/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody XwyySpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/xwyy/specifyMachine/getSpecifyMachine/{id}")
    XwyySpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/xwyy/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody XwyySpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/xwyy/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/xwyy/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/xwyy/specifyMachine/exportData")
    List<XwyySpecifyMachineDto> exportData(@RequestBody XwyySpecifyMachineDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/xwyy/specifyMachine/importData")
    public AjaxResult importData(@RequestBody List<XwyySpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
