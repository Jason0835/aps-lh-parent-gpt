package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90SpecifyMachineDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断定点机台对外暴露接口
 */
@FeignClient(contextId = "iCd90SpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90SpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @GetMapping("/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@SpringQueryMap Cd90SpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/specifyMachine/getSpecifyMachine/{id}")
    Cd90SpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody Cd90SpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/specifyMachine/exportData")
    List<Cd90SpecifyMachineDto> exportData(@SpringQueryMap Cd90SpecifyMachineDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/specifyMachine/importData")
    public AjaxResult importData(@RequestBody List<Cd90SpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
