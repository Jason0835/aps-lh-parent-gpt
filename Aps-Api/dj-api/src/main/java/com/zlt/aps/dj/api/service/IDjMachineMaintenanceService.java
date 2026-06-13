package com.zlt.aps.dj.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;

/**
 * 垫胶机台维修计划对外暴露接口
 */
@FeignClient(contextId = "iDjMachineMaintenanceService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjMachineMaintenanceService {

    /**
     * 获取信息列表
     *
     * @param djMachineMaintenance
     * @return
     */
    @PostMapping("/dj/machineMaintenance/list")
    TableDataInfo list(@RequestBody DjMachineMaintenance djMachineMaintenance);

    /**
     * 保存信息
     *
     * @param djMachineMaintenance
     * @return
     */
    @PostMapping("/dj/machineMaintenance/save")
    AjaxResult save(@Validated @RequestBody DjMachineMaintenance djMachineMaintenance);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/machineMaintenance/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/machineMaintenance/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/machineMaintenance/checkUnique")
    String checkUnique(@RequestBody DjMachineMaintenance djMachineMaintenance);

    /**
     * 导出信息
     * 
     * @param djMachineMaintenance
     * @return
     */
    @PostMapping("/dj/machineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody DjMachineMaintenance queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param importContext
     * @return
     */
    @PostMapping("/dj/machineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}