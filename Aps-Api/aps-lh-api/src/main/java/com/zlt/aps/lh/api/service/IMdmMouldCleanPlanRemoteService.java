package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "IMdmMouldCleanPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface IMdmMouldCleanPlanRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/mouldCleanPlan/list")
    TableDataInfo list(@RequestBody MdmMouldCleanPlan query);

    @ApiOperation("获取详细信息")
    @GetMapping("/mouldCleanPlan/{id}")
    MdmMouldCleanPlan getInfo(@PathVariable("id") Long id);

    @ApiOperation("保存")
    @PostMapping("/mouldCleanPlan/save")
    AjaxResult save(@RequestBody MdmMouldCleanPlan billVO);

    @ApiOperation("删除")
    @DeleteMapping("/mouldCleanPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("导入数据")
    @PostMapping("/mouldCleanPlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception;

    @ApiOperation("导出数据")
    @PostMapping("/mouldCleanPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMouldCleanPlan queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("从模具清洗预警同步生成计划")
    @PostMapping("/mouldCleanPlan/syncFromWarn")
    AjaxResult syncFromWarn();
}
