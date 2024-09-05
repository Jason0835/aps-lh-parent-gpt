package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.SapImportBadNumber;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SAP导入不良数Service接口
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
@FeignClient(contextId = "ISapImportBadNumberService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ISapImportBadNumberService {

    /**
     * 查询SAP导入不良数列表
     */
    @ApiOperation("查询SAP导入不良数列表")
    @PostMapping("/badNumber/list")
    TableDataInfo list(@RequestBody SapImportBadNumber sapImportBadNumber);

    /**
     * 导入SAP导入不良数数据
     */
    @ApiOperation("导入SAP导入不良数")
    @PostMapping("/badNumber/importData")
    public AjaxResult importData(@RequestBody List<SapImportBadNumber> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
