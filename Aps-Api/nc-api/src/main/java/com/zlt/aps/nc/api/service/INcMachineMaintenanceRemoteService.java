package com.zlt.aps.nc.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcMachineMaintenance;

/**
 * 内衬机台维修计划对外暴露接口
 */
@FeignClient(contextId = "INcMachineMaintenanceRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcMachineMaintenanceRemoteService {

    /**
     * 获取机台维修计划列表
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    @PostMapping("/nc/machineMaintenance/list")
    TableDataInfo list(@RequestBody NcMachineMaintenance queryVO);

    /**
     * 保存机台维修计划
     *
     * @param billVO 机台维修计划
     * @return 保存结果
     */
    @PostMapping("/nc/machineMaintenance/save")
    AjaxResult save(@Validated @RequestBody NcMachineMaintenance billVO);

    /**
     * 删除机台维修计划
     *
     * @param ids 主键id集合
     * @return 删除结果
     */
    @PostMapping("/nc/machineMaintenance/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取机台维修计划详细信息
     *
     * @param id 主键id
     * @return 详细信息
     */
    @GetMapping(value = "/nc/machineMaintenance/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     *
     * @param billVO 机台维修计划
     * @return 唯一性标识
     */
    @PostMapping("/nc/machineMaintenance/checkUnique")
    String checkUnique(@RequestBody NcMachineMaintenance billVO);

    /**
     * 导出机台维修计划数据
     *
     * @param queryVO 查询条件
     * @param fileName 导出文件名
     * @return 导出文件字节
     */
    @PostMapping("/nc/machineMaintenance/exportData/{fileName}")
    byte[] exportData(@RequestBody NcMachineMaintenance queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入机台维修计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果
     */
    @PostMapping("/nc/machineMaintenance/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
