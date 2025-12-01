package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延机台信息对外暴露接口
 */
@FeignClient(contextId = "IGdyyMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyMachineInfoService {

    /**
     * 获取钢带压延机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gdyy/machine/list")
    TableDataInfo list(@RequestBody GdyyMachineInfo machineInfo);

    /**
     * 删除钢带压延机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/gdyy/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增钢带压延机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gdyy/machine")
    AjaxResult add(@Validated @RequestBody GdyyMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/gdyy/machine/{id}")
    GdyyMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢带压延机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/gdyy/machine")
    AjaxResult edit(@Validated @RequestBody GdyyMachineInfo machineInfo);

    /**
     * 校验钢带压延机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gdyy/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody GdyyMachineInfo machineInfo);

    /**
     * 导出钢带压延机台列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gdyy/machine/exportList")
    List<GdyyMachineInfo> exportList(@RequestBody GdyyMachineInfo machineInfo);

    /**
     * 根据帘布大卷和机台映射获取对应机台信息
     *
     * @param machineInfo 帘布大卷信息
     * @return 查询到的机台信息
     */
    @PostMapping("/gdyy/machine/listMachineInfo")
    List<GdyyMachineInfo> listMachineInfo(@RequestBody GdyyMachineInfo machineInfo);

    /**
     * 导入数据
     */
    @PostMapping("/gdyy/machine/importData")
    public AjaxResult importData(@RequestBody List<GdyyMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
