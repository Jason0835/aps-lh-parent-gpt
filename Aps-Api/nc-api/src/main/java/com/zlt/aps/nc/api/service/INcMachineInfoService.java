package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬机台信息对外暴露接口
 */
@FeignClient(contextId = "iNcMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcMachineInfoService {

    /**
     * 获取内衬机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list")
    TableDataInfo list(@RequestBody NcMachineInfo machineInfo);

    /**
     * 删除内衬机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增内衬机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine")
    AjaxResult add(@Validated @RequestBody NcMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/machine/{id}")
    NcMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改内衬机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/machine")
    AjaxResult edit(@Validated @RequestBody NcMachineInfo machineInfo);

    /**
     * 校验内衬机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody NcMachineInfo machineInfo);

    /**
     * 导出内衬机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/machine/exportList")
    List<NcMachineInfo> exportList(@RequestBody NcMachineInfo machineInfo);

    /**
     * 根据内衬和口型板获取对应机台信息
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list2")
    List<NcMachineInfo> list2(@RequestBody NcMachineInfo machineInfo);

    @PostMapping("/machine/importData")
    @ApiOperation("导入内衬机台信息")
    public AjaxResult importData(@RequestBody List<NcMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
