package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧机台信息对外暴露接口
 */
@FeignClient(contextId = "iTcMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcMachineInfoService {

    /**
     * 获取胎侧机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tc/machine/list")
    TableDataInfo list(@RequestBody TcMachineInfo machineInfo);

    /**
     * 删除胎侧机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/tc/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增胎侧机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tc/machine")
    AjaxResult add(@Validated @RequestBody TcMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/tc/machine/{id}")
    TcMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎侧机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/tc/machine")
    AjaxResult edit(@Validated @RequestBody TcMachineInfo machineInfo);

    /**
     * 校验胎侧机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tc/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody TcMachineInfo machineInfo);

    /**
     * 导出胎侧机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/tc/machine/exportList")
    List<TcMachineInfo> exportList(@RequestBody TcMachineInfo machineInfo);

    /**
     * 根据胎侧和口型板获取对应机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tc/machine/list2")
    List<TcMachineInfo> list2(@RequestBody TcMachineInfo machineInfo);

    /**
     * 数据导入
     */
    @PostMapping("/tc/machine/importData")
    AjaxResult importData(@RequestBody List<TcMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


}
