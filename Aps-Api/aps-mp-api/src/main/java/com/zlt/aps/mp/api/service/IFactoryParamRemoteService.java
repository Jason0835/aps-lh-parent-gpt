package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.vo.FactoryParamVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryParamRemoteService.java
 * 描    述：IFactoryParamRemoteService系统参数（排产设定）前端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@FeignClient(contextId = "IFactoryParamRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryParamRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/factoryParam/list")
    TableDataInfo list(@RequestBody FactoryParam QueryVO);

    /**
     * 修改分厂排产设定
     */
    @ApiOperation("修改分厂排产设定")
    @PostMapping("/factoryParam/edit")
    AjaxResult edit(@RequestBody FactoryParam entity);

    /**
     * 复制分厂排产设定
     */
    @ApiOperation("复制分厂排产设定")
    @PostMapping("/factoryParam/copy")
    AjaxResult copy(@RequestBody FactoryParamVo factoryParamVo);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/factoryParam/{id}")
    FactoryParam getInfo(@PathVariable("id") Long id);

}
