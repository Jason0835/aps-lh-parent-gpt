package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶机台信息对外暴露接口
 */
@FeignClient(contextId = "iNcMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:nc}")
public interface IDjMachineInfoService {

    /**
     * 获取垫胶机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/dj/machine/list")
    TableDataInfo list(@RequestBody DjMachineInfo machineInfo);

    /**
     * 删除垫胶机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增垫胶机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/dj/machine")
    AjaxResult add(@Validated @RequestBody DjMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/machine/{id}")
    DjMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改垫胶机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/dj/machine")
    AjaxResult edit(@Validated @RequestBody DjMachineInfo machineInfo);

    /**
     * 校验垫胶机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/dj/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody DjMachineInfo machineInfo);

    /**
     * 导出垫胶机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/machine/exportList")
    List<DjMachineInfo> exportList(@RequestBody DjMachineInfo machineInfo);

    /**
     * 根据垫胶和口型板获取对应机台信息
     * @param machineInfo
     * @return
     */
    @PostMapping("/dj/machine/list2")
    List<DjMachineInfo> list2(@RequestBody DjMachineInfo machineInfo);

    @PostMapping("/dj/machine/importData")
    @ApiOperation("导入垫胶机台信息")
    public AjaxResult importData(@RequestBody List<DjMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
