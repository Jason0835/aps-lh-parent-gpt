package com.zlt.aps.maindata.api;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawWarningRecordRemoteService.java
 * 描    述：IRawWarningRecordRemoteService原材料预警记录前端接口
 *@author zlt
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawWarningRecordRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawWarningRecordRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawWarningRecord/list")
    TableDataInfo list(@RequestBody RawWarningRecord QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawWarningRecord/save")
    AjaxResult save(@RequestBody RawWarningRecord rawWarningRecord);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawWarningRecord/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawWarningRecord/{id}")
    RawWarningRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawWarningRecord/checkUnique")
    String checkUnique(@RequestBody RawWarningRecord rawWarningRecordVO);

    /**
     * 导出原材料预警记录列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawWarningRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody RawWarningRecord queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入原材料预警记录数据
     */
    @ApiOperation("导入原材料预警记录")
    @PostMapping("/rawWarningRecord/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);


    @GetMapping("/rawWarningRecord/statistics")
    @ApiOperation("获取预警统计")
    public AjaxResult getStatistics(@RequestParam("factoryCode") String factoryCode,
                                    @RequestParam(value = "warningType", required = false) String warningType,
                                    @RequestParam(value = "days", required = false) Integer days) ;


    @PostMapping("/rawWarningRecord/handle-warning")
    @ApiOperation("处理预警记录")
    public AjaxResult handleWarning(@RequestParam("id") Long id,
                                    @RequestParam("handler") String handler,
                                    @RequestParam("opinion") String opinion);

    @PostMapping("/rawWarningRecord/sync-actual-usage")
    @ApiOperation("同步实际用量数据")
    public AjaxResult syncActualUsage(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("year") Integer year,
                                      @RequestParam("week") Integer week,
                                      @RequestParam("month") Integer month);

    @PostMapping("/rawWarningRecord/execute-new-material-warning")
    @ApiOperation("执行新材料预警")
    public AjaxResult executeNewMaterialWarning(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("year") Integer year,
                                                @RequestParam("month") Integer month);

    @PostMapping("/rawWarningRecord/execute-usage-warning")
    @ApiOperation("执行用量偏差预警")
    public AjaxResult executeUsageWarning(@RequestParam("factoryCode") String factoryCode,
                                          @RequestParam("year") Integer year,
                                          @RequestParam("week") Integer week);
}
