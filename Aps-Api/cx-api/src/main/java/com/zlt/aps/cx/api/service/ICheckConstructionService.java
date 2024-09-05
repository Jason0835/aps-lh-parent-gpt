package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxCheckConstruction;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;


/**
 * 施工信息检测Service接口
 */
@FeignClient(contextId = "ICheckConstructionService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICheckConstructionService {

    @ApiOperation("查询施工信息检测列表")
    @PostMapping("/checkConstruction/list")
    TableDataInfo list(@RequestBody CxCheckConstruction cxCheckConstruction);

    @ApiOperation("检测施工")
    @PostMapping("/checkConstruction/buildCheckConstructionExcel")
    public CxCheckConstruction buildCheckConstructionExcel(@RequestBody CxCheckConstruction cxCheckConstruction);

    @ApiOperation("检测施工")
    @PostMapping("/checkConstruction/saveCheckConstruction")
    public AjaxResult saveCheckConstruction(@RequestBody CxCheckConstruction cxCheckConstruction);
}
