package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化机台信息对外暴露接口
 */
@FeignClient(contextId = "iLhMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhMachineInfoService {

    /**
     * 获取硫化机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/list")
    TableDataInfo list(@RequestBody LhMachineInfo machineInfo);

    /**
     * 删除硫化机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增硫化机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine")
    AjaxResult add(@Validated @RequestBody LhMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/machine/{id}")
    LhMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改硫化机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/machine")
    AjaxResult edit(@Validated @RequestBody LhMachineInfo machineInfo);

    /**
     * 校验硫化机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody LhMachineInfo machineInfo);

    /**
     * 导出硫化机台列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/machine/exportList")
    List<LhMachineInfo> exportList(@RequestBody LhMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    @PostMapping("/machine/listMachineInfo")
    List<LhMachineInfo> listMachineInfo(@RequestBody LhMachineInfo machineInfo);

    /**
     * 导入数据
     */
    @PostMapping("/machine/importData")
    public AjaxResult importData(@RequestBody List<LhMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
