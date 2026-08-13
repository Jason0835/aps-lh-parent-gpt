package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计划调整需求信息 Feign 接口。
 */
@FeignClient(contextId = "IMpAdjustPlanRequireInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpAdjustPlanRequireInfoRemoteService {

    /** 查询列表 */
    @ApiOperation("查询计划调整需求信息列表")
    @PostMapping("/adjustPlanRequireInfo/list")
    TableDataInfo list(@RequestBody MpAdjustPlanRequireInfo queryVO);

    /** 获取详情 */
    @ApiOperation("获取计划调整需求信息详情")
    @GetMapping("/adjustPlanRequireInfo/getInfo/{id}")
    MpAdjustPlanRequireInfo getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增计划调整需求信息")
    @PostMapping("/adjustPlanRequireInfo/add")
    AjaxResult add(@RequestBody MpAdjustPlanRequireInfo entity);

    /** 编辑 */
    @ApiOperation("编辑计划调整需求信息")
    @PostMapping("/adjustPlanRequireInfo/edit")
    AjaxResult edit(@RequestBody MpAdjustPlanRequireInfo entity);

    /** 删除 */
    @ApiOperation("删除计划调整需求信息")
    @PostMapping("/adjustPlanRequireInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 导出数据 */
    @ApiOperation("导出计划调整需求信息")
    @PostMapping("/adjustPlanRequireInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MpAdjustPlanRequireInfo queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入计划调整需求信息")
    @PostMapping("/adjustPlanRequireInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /** 产品结构下拉数据（来源 mdmSkuStructureRef，去重） */
    @ApiOperation("产品结构下拉数据")
    @GetMapping("/adjustPlanRequireInfo/structureOptions")
    AjaxResult structureOptions(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                @RequestParam(value = "structureName", required = false) String structureName);

    /** 物料编码下拉数据（来源 mdmSkuStructureRef，含物料描述反显） */
    @ApiOperation("物料编码下拉数据")
    @GetMapping("/adjustPlanRequireInfo/materialOptions")
    AjaxResult materialOptions(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                               @RequestParam(value = "structureName", required = false) String structureName,
                               @RequestParam(value = "materialCode", required = false) String materialCode);
}
