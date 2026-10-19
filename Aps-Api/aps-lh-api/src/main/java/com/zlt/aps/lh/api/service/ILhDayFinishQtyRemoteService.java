package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化排程日完成量远程服务接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ILhDayFinishQtyRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhDayFinishQtyRemoteService {

    /**
     * 查询硫化排程日完成量列表
     */
    @ApiOperation("查询硫化排程日完成量列表")
    @PostMapping("/lhDayFinishQty/list")
    TableDataInfo list(@RequestBody LhDayFinishQty lhDayFinishQty);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhDayFinishQty/{id}")
    LhDayFinishQty getInfo(@PathVariable("id") Long id);

    /**
     * 保存硫化排程日完成量信息（id为空则新增，id不为空则修改）
     */
    @ApiOperation("保存硫化排程日完成量信息")
    @PostMapping("/lhDayFinishQty/save")
    AjaxResult save(@RequestBody LhDayFinishQty lhDayFinishQty);

    /**
     * 批量删除硫化排程日完成量
     */
    @ApiOperation("批量删除硫化排程日完成量")
    @DeleteMapping("/lhDayFinishQty/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出硫化排程日完成量列表
     */
    @ApiOperation("导出硫化排程日完成量列表")
    @PostMapping("/lhDayFinishQty/exportData/{fileName}")
    byte[] exportData(@RequestBody LhDayFinishQty lhDayFinishQty, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化排程日完成量数据
     */
    @ApiOperation("导入硫化排程日完成量数据")
    @PostMapping("/lhDayFinishQty/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入硫化排程日完成量数据（Feign接口）
     */
    @ApiOperation("导入硫化排程日完成量数据")
    @PostMapping("/lhDayFinishQty/importDataFeign")
    AjaxResult importDataFeign(@RequestBody List<LhDayFinishQty> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhDayFinishQty/checkUnique")
    String checkUnique(@RequestBody LhDayFinishQty lhDayFinishQty);
}
