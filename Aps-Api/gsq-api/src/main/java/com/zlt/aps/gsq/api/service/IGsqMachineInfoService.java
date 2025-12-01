package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈机台信息对外暴露接口
 */
@FeignClient(contextId = "iGsqMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqMachineInfoService {

    /**
     * 获取钢丝圈机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine/list")
    TableDataInfo list(@RequestBody GsqMachineInfo machineInfo);

    /**
     * 删除钢丝圈机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/gsq/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增钢丝圈机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine")
    AjaxResult add(@Validated @RequestBody GsqMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/gsq/machine/{id}")
    GsqMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/gsq/machine")
    AjaxResult edit(@Validated @RequestBody GsqMachineInfo machineInfo);

    /**
     * 校验钢丝圈机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody GsqMachineInfo machineInfo);

    /**
     * 导出钢丝圈机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/gsq/machine/exportList")
    List<GsqMachineInfo> exportList(@RequestBody GsqMachineInfo machineInfo);

    /**
     * 获取钢丝圈机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine/listMachineInfo")
    List<GsqMachineInfo> listMachineInfo(@RequestBody GsqMachineInfo machineInfo);

    @PostMapping("/gsq/machine/importData")
    @ApiOperation("导入钢丝圈机台信息")
    public AjaxResult importData(@RequestBody List<GsqMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
