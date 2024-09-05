package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90°裁断机台信息对外暴露接口
 */
@FeignClient(contextId = "iCd90MachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90MachineInfoService {

    /**
     * 获取90°裁断机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list")
    TableDataInfo list(@RequestBody Cd90MachineInfo machineInfo);

    /**
     * 删除90°裁断机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增90°裁断机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine")
    AjaxResult add(@Validated @RequestBody Cd90MachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/machine/{id}")
    Cd90MachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改90°裁断机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/machine")
    AjaxResult edit(@Validated @RequestBody Cd90MachineInfo machineInfo);

    /**
     * 校验90°裁断机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody Cd90MachineInfo machineInfo);

    /**
     * 导出90°裁断机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/machine/exportList")
    List<Cd90MachineInfo> exportList(@RequestBody Cd90MachineInfo machineInfo);

    @PostMapping("/machine/list2")
    List<Cd90MachineInfo> list2(@RequestBody Cd90MachineInfo machineInfo);

    /**
     * 导入数据
     */
    @PostMapping("/machine/importData")
    public AjaxResult importData(@RequestBody List<Cd90MachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
