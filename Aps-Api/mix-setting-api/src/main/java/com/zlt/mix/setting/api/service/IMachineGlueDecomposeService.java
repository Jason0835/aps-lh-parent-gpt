package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.dto.MachineGlueDecomposeDto;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密炼机指定胶料分解Service接口
 *
 * @author Liam
 * @date 2022-03-29
 */
@FeignClient(contextId = "IMachineGlueDecomposeService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMachineGlueDecomposeService {

    /**
     * 查询密炼机指定胶料分解列表
     */
    @PostMapping("/machineGlueDecompose/list")
    TableDataInfo listMachineGlueDecompose(@RequestBody MachineGlueDecompose machineGlueDecompose);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/machineGlueDecompose/{id}")
    MachineGlueDecomposeDto getMachineGlueDecomposeInfo(@PathVariable("id") Long id);

    /**
     * 保存密炼机指定胶料分解信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/machineGlueDecompose/save")
    AjaxResult saveMachineGlueDecompose(@RequestBody MachineGlueDecompose machineGlueDecompose);

    /**
     * 批量删除密炼机指定胶料分解
     */
    @PostMapping("/machineGlueDecompose/delete/{ids}")
    AjaxResult deleteMachineGlueDecompose(@PathVariable("ids") Long[] ids);

    /**
     * 校验密炼机指定胶料分解唯一性
     */
    @ApiOperation("校验密炼机指定胶料分解唯一性")
    @PostMapping("/machineGlueDecompose/checkMachineGlueDecomposeUnique")
    String checkMachineGlueDecomposeUnique(@RequestBody MachineGlueDecompose machineGlueDecompose);

    /**
     * 导出密炼机指定胶料分解列表
     */
    @PostMapping("/machineGlueDecompose/exportData")
    List<MachineGlueDecomposeDto> exportData(@RequestBody MachineGlueDecompose machineGlueDecompose);

    /**
     * 导入密炼机指定胶料分解数据
     */
    @ApiOperation("导入密炼机指定胶料分解")
    @PostMapping("/machineGlueDecompose/importData")
    public AjaxResult importData(@RequestBody List<MachineGlueDecomposeDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
