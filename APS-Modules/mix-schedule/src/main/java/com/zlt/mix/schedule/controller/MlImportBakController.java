package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.schedule.api.domain.vo.MlImportBak;
import com.zlt.mix.schedule.service.IMlImportBakService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MlImportBakController.java
 * 描    述：密炼线下计划操作功能 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-05
 */
@Slf4j
@Api(tags = "密炼线下计划操作功能")
@RestController
@RequestMapping("/mlImportBak")
public class MlImportBakController extends BaseController<MlImportBak> {

    @Autowired
    private IMlImportBakService mlImportBakService;

    @Log(title = "schedule.glueScheduleResult.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入线下排程数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importOfflineData")
    public AjaxResult importOfflineData(@RequestBody List<MlImportBak> list, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("mixArea") String mixArea, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return mlImportBakService.importOfflineData(list, DateUtils.parseDate(scheduleDate), mixArea, importLogId);
    }
}
