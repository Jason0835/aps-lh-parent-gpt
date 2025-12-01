package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyySpecifyMachineDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延定点机台对外暴露接口
 */
@FeignClient(contextId = "IGdyySpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyySpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/gdyy/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody GdyySpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/gdyy/specifyMachine/getSpecifyMachine/{id}")
    GdyySpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/gdyy/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody GdyySpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/gdyy/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/gdyy/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/gdyy/specifyMachine/exportData")
    List<GdyySpecifyMachineDto> exportData(@RequestBody GdyySpecifyMachineDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/gdyy/specifyMachine/importData")
    public AjaxResult importData(@RequestBody List<GdyySpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
