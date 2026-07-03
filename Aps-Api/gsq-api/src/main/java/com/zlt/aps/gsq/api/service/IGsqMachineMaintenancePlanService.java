package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineMaintenancePlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈机台维修计划对外暴露接口
 *
 * @author zlt
 * @date 2026-07-01
 */
@FeignClient(contextId = "iGsqMachineMaintenancePlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqMachineMaintenancePlanService {

    /**
     * 查询钢丝圈机台维修计划列表
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/machineMaintenancePlan/list")
    @ApiOperation("查询钢丝圈机台维修计划列表")
    TableDataInfo list(@RequestBody GsqMachineMaintenancePlan entity);

    /**
     * 获取钢丝圈机台维修计划详细信息
     *
     * @param id 主键ID
     * @return 详细信息
     */
    @GetMapping(value = "/gsq/machineMaintenancePlan/{id}")
    @ApiOperation("获取钢丝圈机台维修计划详细信息")
    GsqMachineMaintenancePlan getInfo(@PathVariable("id") Long id);

    /**
     * 保存钢丝圈机台维修计划（id为空则新增，id不为空则修改）
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/machineMaintenancePlan/save")
    @ApiOperation("保存钢丝圈机台维修计划（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody GsqMachineMaintenancePlan entity);

    /**
     * 删除钢丝圈机台维修计划
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/machineMaintenancePlan/delete/{ids}")
    @ApiOperation("删除钢丝圈机台维修计划")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    /**
     * 导出钢丝圈机台维修计划
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/machineMaintenancePlan/exportData/{fileName}")
    @ApiOperation("导出钢丝圈机台维修计划")
    byte[] exportData(@RequestBody GsqMachineMaintenancePlan entity, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝圈机台维修计划
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在是否更新
     * @return 操作结果
     */
    @PostMapping("/gsq/machineMaintenancePlan/importData")
    @ApiOperation("导入钢丝圈机台维修计划")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 校验钢丝圈机台维修计划唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/machineMaintenancePlan/checkUnique")
    @ApiOperation("校验钢丝圈机台维修计划唯一性")
    String checkUnique(@RequestBody GsqMachineMaintenancePlan entity);
}
