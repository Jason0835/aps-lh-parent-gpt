package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型排程中夜班完成量Service接口
 * @author chen
 * @date 2022-02-25
 */
@FeignClient(contextId = "IMidNightShiftFinishService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IMidNightShiftFinishService {

    /**
     * 查询成型排程中夜班完成量列表
     */
    @ApiOperation("查询成型排程中夜班完成量列表")
    @PostMapping("/midNightFinish/list")
    TableDataInfo list(@RequestBody MidNightShiftFinish midNightShiftFinish);

    /**
    * 新增成型排程中夜班完成量
    */
    @ApiOperation("新增成型排程中夜班完成量")
    @PostMapping("/midNightFinish/add")
    AjaxResult add(@RequestBody MidNightShiftFinish midNightShiftFinish);

    /**
     * 修改成型排程中夜班完成量
     */
    @ApiOperation("修改成型排程中夜班完成量")
    @PostMapping("/midNightFinish/edit")
    AjaxResult edit(@RequestBody MidNightShiftFinish midNightShiftFinish);

    /**
     * 删除成型排程中夜班完成量
     */
    @ApiOperation("删除成型排程中夜班完成量")
    @DeleteMapping("/midNightFinish/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/midNightFinish/{id}")
    MidNightShiftFinish getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型排程中夜班完成量唯一性
     */
    @ApiOperation("校验成型排程中夜班完成量唯一性")
    @PostMapping("/midNightFinish/checkMidNightShiftFinishUnique")
    String checkMidNightShiftFinishUnique(@RequestBody MidNightShiftFinish midNightShiftFinish);

    /**
     * 导出成型排程中夜班完成量列表
     */
    @ApiOperation("导出成型排程中夜班完成量列表")
    @PostMapping("/midNightFinish/getList")
    List<MidNightShiftFinish> getList(@RequestBody MidNightShiftFinish midNightShiftFinish);

    /**
     * 导入成型排程中夜班完成量数据
     */
    @ApiOperation("导入成型排程中夜班完成量")
    @PostMapping("/midNightFinish/importData")
    public AjaxResult importData(@RequestBody List<MidNightShiftFinish> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
