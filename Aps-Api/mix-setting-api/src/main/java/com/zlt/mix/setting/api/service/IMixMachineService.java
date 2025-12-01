package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 密炼机台信息Service接口
 * @author Gim
 * @date 2022-03-22
 */
@FeignClient(contextId = "IMixMachineService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMixMachineService {

    /**
     * 查询密炼机台信息列表
     */
    @PostMapping("/machine/list")
    TableDataInfo listMixMachine(@RequestBody MixMachine mixMachine);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/machine/{id}")
    MixMachine getMixMachineInfo(@PathVariable("id") Long id);

    /**
    * 保存密炼机台信息信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/machine/save")
    AjaxResult saveMixMachine(@RequestBody MixMachine mixMachine);

    /**
     * 批量删除密炼机台信息
     */
    @PostMapping("/machine/delete/{ids}")
    AjaxResult deleteMixMachine(@PathVariable("ids") Long[] ids);

    /**
     * 校验密炼机台信息唯一性
     */
    @ApiOperation("校验密炼机台信息唯一性")
    @PostMapping("/machine/checkMixMachineUnique")
    String checkMixMachineUnique(@RequestBody MixMachine mixMachine);
    
    /**
     * 取出机台信息列表
     */
    @PostMapping("/machine/getMachines")
    List<MixMachine> getMachines(@RequestBody MixMachine mixMachine);

    /**
     * 导出密炼机台信息列表
     */
    @PostMapping("/machine/exportData")
    List<MixMachine> exportData(@RequestBody MixMachine mixMachine);

    /**
     * 导入密炼机台信息数据
     */
    @ApiOperation("导入密炼机台信息")
    @PostMapping("/machine/importData")
    public AjaxResult importData(@RequestBody List<MixMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * @return 查询到的机台信息
     */
    @ApiOperation("查询所有机台信息(包含硫磺辅料机台信息)")
    @PostMapping("/machine/getAllMachineInfo")
    public ArrayList<MixMachine> getAllMachineInfo();
}
