package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延机台信息对外暴露接口
 */
@FeignClient(contextId = "IXwyyMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyMachineInfoService {

    /**
     * 获取纤维压延机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list")
    TableDataInfo list(@RequestBody XwyyMachineInfo machineInfo);

    /**
     * 删除纤维压延机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增纤维压延机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine")
    AjaxResult add(@Validated @RequestBody XwyyMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/machine/{id}")
    XwyyMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改纤维压延机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/machine")
    AjaxResult edit(@Validated @RequestBody XwyyMachineInfo machineInfo);

    /**
     * 校验纤维压延机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody XwyyMachineInfo machineInfo);

    /**
     * 导出纤维压延机台列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/exportList")
    List<XwyyMachineInfo> exportList(@RequestBody XwyyMachineInfo machineInfo);

    /**
     * 根据帘布大卷和机台映射获取对应机台信息
     *
     * @param machineInfo 帘布大卷信息
     * @return 查询到的机台信息
     */
    @PostMapping("/machine/listMachineInfo")
    List<XwyyMachineInfo> listMachineInfo(@RequestBody XwyyMachineInfo machineInfo);

    /**
     * 导入数据
     */
    @PostMapping("/machine/importData")
    public AjaxResult importData(@RequestBody List<XwyyMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
