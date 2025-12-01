package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方与机台对应Service接口
 *
 * @author Gim
 * @date 2022-03-28
 */
@FeignClient(contextId = "IFormulaMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IFormulaMachineService {

    /**
     * 查询配方与机台对应列表
     */
    @PostMapping("/formulaMachine/list")
    TableDataInfo listFormulaMachine(@RequestBody FormulaMachine formulaMachine);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/formulaMachine/{id}")
    FormulaMachine getFormulaMachineInfo(@PathVariable("id") Long id);

    /**
     * 根据mixArea,glue获取详细信息
     */
    @GetMapping(value = "/formulaMachine/{mixArea}/{glue}")
    FormulaMachine getFormulaMachineInfo(@PathVariable("mixArea") String mixArea, @PathVariable("glue") String glue);

    /**
     * 保存配方与机台对应信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/formulaMachine/save")
    AjaxResult saveFormulaMachine(@RequestBody FormulaMachine formulaMachine);

    /**
     * 批量删除配方与机台对应
     */
    @PostMapping("/formulaMachine/delete/{ids}")
    AjaxResult deleteFormulaMachine(@PathVariable("ids") Long[] ids);

    /**
     * 校验配方与机台对应唯一性
     */
    @ApiOperation("校验配方与机台对应唯一性")
    @PostMapping("/formulaMachine/checkFormulaMachineUnique")
    String checkFormulaMachineUnique(@RequestBody FormulaMachine formulaMachine);

    /**
     * 导出配方与机台对应列表
     */
    @PostMapping("/formulaMachine/exportData")
    List<FormulaMachine> exportData(@RequestBody FormulaMachine formulaMachine);

    /**
     * 导入配方与机台对应数据
     */
    @ApiOperation("导入配方与机台对应")
    @PostMapping("/formulaMachine/importData")
    public AjaxResult importData(@RequestBody List<FormulaMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据密炼区和胶料名称进行精确查询
     */
    @ApiOperation("根据密炼区和胶料名称进行精确查询")
    @PostMapping("/formulaMachine/getFormulaMachineList")
    ArrayList<FormulaMachine> getFormulaMachineList(@RequestBody FormulaMachine formulaMachine);

    /**
     * 查询配方与机台对应列表
     */
    @PostMapping("/formulaMachine/getRecipeMachineList")
    ArrayList<FormulaMachine> listRecipeMachine(@RequestBody FormulaMachine formulaMachine);
}
