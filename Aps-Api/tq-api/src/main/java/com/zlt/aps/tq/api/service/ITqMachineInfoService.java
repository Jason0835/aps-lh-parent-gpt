package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈机台信息对外暴露接口
 */
@FeignClient(contextId = "iTqMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqMachineInfoService {

    /**
     * 获取胎圈机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tq/machine/list")
    TableDataInfo list(@RequestBody TqMachineInfo machineInfo);

    /**
     * 删除胎圈机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/tq/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增胎圈机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tq/machine")
    AjaxResult add(@Validated @RequestBody TqMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/tq/machine/{id}")
    TqMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎圈机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/tq/machine")
    AjaxResult edit(@Validated @RequestBody TqMachineInfo machineInfo);

    /**
     * 校验胎圈机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tq/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody TqMachineInfo machineInfo);

    /**
     * 导出胎圈机台列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/tq/machine/exportList")
    List<TqMachineInfo> exportList(@RequestBody TqMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     * @param machineInfo 查询条件
     * @return 结果
     */
    @PostMapping("/tq/machine/listMachineInfo")
    List<TqMachineInfo> listMachineInfo(@RequestBody TqMachineInfo machineInfo);

    @PostMapping("/tq/machine/importData")
    @ApiOperation("导入胎面机台信息")
    public AjaxResult importData(@RequestBody List<TqMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
