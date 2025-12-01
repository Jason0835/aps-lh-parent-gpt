package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面机台信息对外暴露接口
 */
@FeignClient(contextId = "iTmMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmMachineInfoService {

    /**
     * 获取胎面机台信息列表
     * @param machineInfo
     * @return
     */
    @PostMapping("/tm/machine/list")
    TableDataInfo list(@RequestBody TmMachineInfo machineInfo);

    /**
     * 根据胎面和口型板获取对应机台信息
     * @param machineInfo
     * @return
     */
    @PostMapping("/tm/machine/list2")
    List<TmMachineInfo> list2(@RequestBody TmMachineInfo machineInfo);
	
	/**
     * 删除胎面机台信息
     * @param ids
     * @return
     */
    @DeleteMapping("/tm/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);
	
	/**
     * 新增胎面机台信息
     * @param machineInfo
     * @return
     */
    @PostMapping("/tm/machine")
    AjaxResult add(@Validated @RequestBody TmMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     * @param id
     * @return
     */
    @GetMapping(value = "/tm/machine/{id}")
    TmMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎面机台信息
     * @param machineInfo
     * @return
     */
    @PutMapping("/tm/machine")
    AjaxResult edit(@Validated @RequestBody TmMachineInfo machineInfo);

    /**
     * 校验胎面机台唯一性
     * @param machineInfo
     * @return
     */
    @PostMapping("/tm/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody TmMachineInfo machineInfo);

    /**
     * 导出胎面机台列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tm/machine/exportList")
    List<TmMachineInfo> exportList(@RequestBody TmMachineInfo machineInfo);

    @PostMapping("/tm/machine/importData")
    @ApiOperation("导入胎面机台信息")
    public AjaxResult importData(@RequestBody List<TmMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
