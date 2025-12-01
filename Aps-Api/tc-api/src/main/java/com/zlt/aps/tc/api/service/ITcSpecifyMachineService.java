package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcSpecifyMachineDto;
import com.zlt.aps.tc.api.domain.entity.TcQuotaSetting;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧定点机台对外暴露接口
 */
@FeignClient(contextId = "iTcSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcSpecifyMachineService {

    /**
     * 根据条件查询定点机台列表
     */
    @PostMapping("/tc/specifyMachine/listSpecifyMachine")
    TableDataInfo listSpecifyMachine(@RequestBody TcSpecifyMachineDto dto);

    /**
     * 根据id查询定点机台信息
     */
    @GetMapping("/tc/specifyMachine/getSpecifyMachine/{id}")
    TcSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id);

    /**
     * 保存定点机台信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tc/specifyMachine/saveSpecifyMachine")
    AjaxResult saveSpecifyMachine(@RequestBody TcSpecifyMachineDto dto);

    /**
     * 批量删除定点机台信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tc/specifyMachine/deleteSpecifyMachine/{ids}")
    AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids);

    /**
     * 删除全部定点机台信息(逻辑删)
     */
    @PostMapping("/tc/specifyMachine/deleteAllSpecifyMachine")
    AjaxResult deleteAllSpecifyMachine();

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/tc/specifyMachine/exportData")
    List<TcSpecifyMachineDto> exportData(@RequestBody TcSpecifyMachineDto dto);

    /**
     * 数据导入
     */
    @PostMapping("/tc/specifyMachine/importData")
    AjaxResult importData(@RequestBody List<TcSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
