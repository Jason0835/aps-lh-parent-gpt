package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小料机台信息Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
@FeignClient(contextId = "ILhflMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ILhflMachineService {

    /**
     * 查询小料机台信息列表
     */
    @PostMapping("/lhflMachine/list")
    TableDataInfo listLhflMachine(@RequestBody LhflMachine lhflMachine);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/lhflMachine/{id}")
    LhflMachine getLhflMachineInfo(@PathVariable("id") Long id);

    /**
     * 保存小料机台信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/lhflMachine/save")
    AjaxResult saveLhflMachine(@RequestBody LhflMachine lhflMachine);

    /**
     * 批量删除小料机台信息
     */
    @PostMapping("/lhflMachine/delete/{ids}")
    AjaxResult deleteLhflMachine(@PathVariable("ids") Long[] ids);

    /**
     * 校验小料机台信息唯一性
     */
    @ApiOperation("校验小料机台信息唯一性")
    @PostMapping("/lhflMachine/checkLhflMachineUnique")
    String checkLhflMachineUnique(@RequestBody LhflMachine lhflMachine);

    /**
     * 导出小料机台信息列表
     */
    @PostMapping("/lhflMachine/exportData")
    List<LhflMachine> exportData(@RequestBody LhflMachine lhflMachine);

    /**
     * 导入小料机台信息数据
     */
    @ApiOperation("导入小料机台信息")
    @PostMapping("/lhflMachine/importData")
    public AjaxResult importData(@RequestBody List<LhflMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
