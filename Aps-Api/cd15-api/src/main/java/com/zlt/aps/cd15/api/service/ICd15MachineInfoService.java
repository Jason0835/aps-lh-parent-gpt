package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15°裁断机台信息对外暴露接口
 */
@FeignClient(contextId = "iCd15MachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15MachineInfoService {

    /**
     * 获取15°裁断机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list")
    TableDataInfo list(@RequestBody Cd15MachineInfo machineInfo);

    /**
     * 删除15°裁断机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增15°裁断机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine")
    AjaxResult add(@Validated @RequestBody Cd15MachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/machine/{id}")
    Cd15MachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改15°裁断机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/machine")
    AjaxResult edit(@Validated @RequestBody Cd15MachineInfo machineInfo);

    /**
     * 校验15°裁断机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody Cd15MachineInfo machineInfo);

    /**
     * 导出15°裁断机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/machine/exportList")
    List<Cd15MachineInfo> exportList(@RequestBody Cd15MachineInfo machineInfo);

    /**
     * 根据机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list2")
    List<Cd15MachineInfo> list2(@RequestBody Cd15MachineInfo machineInfo);

    @PostMapping("/machine/importData")
    @ApiOperation("导入15度裁断机台信息")
    public AjaxResult importData(@RequestBody List<Cd15MachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
