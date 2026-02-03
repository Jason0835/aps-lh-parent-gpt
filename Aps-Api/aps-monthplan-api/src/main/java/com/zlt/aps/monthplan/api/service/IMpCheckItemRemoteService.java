package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpCheckItemRemoteService.java
 * 描    述：IMpCheckItemRemoteService 检测项前端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2026-01-30
 */
@FeignClient(contextId = "IMpCheckItemRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpCheckItemRemoteService {

    @ApiOperation("检测项检测接口")
    @PostMapping("/checkItem/check/{productType}")
    public AjaxResult check(@PathVariable("productType") String productType);
}
