package com.zlt.aps.cx.controller;

import cn.hutool.extra.tokenizer.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型机台Controller
 *
 * @author APS Team
 */
@Tag(name = "成型机台管理", description = "成型机台相关接口")
@RestController
@RequestMapping("/machine")
public class MachineController {

    @Autowired
    private MdmMoldingMachineMapper mdmMoldingMachineMapper;

    @Operation(summary = "获取所有可用机台", description = "获取所有状态为运行中的机台列表")
    @GetMapping("/available")
    public AjaxResult listAvailableMachines() {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(MdmMoldingMachine::getIsActive, 1)
//                .and(w -> w.eq(MdmMoldingMachine::getMaintainStatus, "RUNNING")
//                        .or().isNull(MdmMoldingMachine::getMaintainStatus));
        return AjaxResult.success(mdmMoldingMachineMapper.selectList(wrapper));
    }

    @Operation(summary = "根据产线获取机台", description = "根据产线编号获取机台列表")
    @GetMapping("/line/{lineNumber}")
    public AjaxResult listByLineNumber(
            @Parameter(description = "产线编号") @PathVariable Integer lineNumber) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(MdmMoldingMachine::getLineNumber, lineNumber)
//                .eq(MdmMoldingMachine::getIsActive, 1);
        return AjaxResult.success(mdmMoldingMachineMapper.selectList(wrapper));
    }

    @Operation(summary = "根据机台编码获取机台", description = "根据机台编码获取机台详情")
    @GetMapping("/code/{machineCode}")
    public AjaxResult getByMachineCode(
            @Parameter(description = "机台编码") @PathVariable String machineCode) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getCxMachineCode, machineCode);
        return AjaxResult.success(mdmMoldingMachineMapper.selectOne(wrapper));
    }

    @Operation(summary = "分页查询机台", description = "分页查询所有机台")
    @GetMapping("/page")
    public AjaxResult pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "机台状态") @RequestParam(required = false) String status) {
        Page<MdmMoldingMachine> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        

        
        wrapper.orderByAsc(MdmMoldingMachine::getCxMachineCode);
        return AjaxResult.success(mdmMoldingMachineMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "根据 ID 获取机台", description = "根据机台 ID 获取机台详情")
    @GetMapping("/{id}")
    public AjaxResult getById(
            @Parameter(description = "机台 ID") @PathVariable Long id) {
        return AjaxResult.success(mdmMoldingMachineMapper.selectById(id));
    }

    @Operation(summary = "新增机台", description = "新增成型机台")
    @PostMapping
    public AjaxResult save(@RequestBody MdmMoldingMachine machine) {
        return AjaxResult.success(mdmMoldingMachineMapper.insert(machine) > 0);
    }

    @Operation(summary = "更新机台", description = "更新成型机台信息")
    @PutMapping
    public AjaxResult update(@RequestBody MdmMoldingMachine machine) {
        return AjaxResult.success(mdmMoldingMachineMapper.updateById(machine) > 0);
    }

    @Operation(summary = "删除机台", description = "删除指定 ID 的机台")
    @DeleteMapping("/{id}")
    public AjaxResult delete(
            @Parameter(description = "机台 ID") @PathVariable Long id) {
        return AjaxResult.success(mdmMoldingMachineMapper.deleteById(id) > 0);
    }

    @Operation(summary = "更新机台状态", description = "更新机台的维护状态")
    @PutMapping("/status/{id}")
    public AjaxResult updateStatus(
            @Parameter(description = "机台 ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        MdmMoldingMachine machine = new MdmMoldingMachine();
        machine.setId(id);
//        machine.setMaintainStatus(status);
        return AjaxResult.success(mdmMoldingMachineMapper.updateById(machine) > 0);
    }
}
