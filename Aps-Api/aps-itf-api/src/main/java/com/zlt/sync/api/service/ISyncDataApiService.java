package com.zlt.sync.api.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;

import io.swagger.annotations.ApiOperation;

/**
 * 排程 远程同步接口
 */
@FeignClient(contextId = "iSyncDataApiService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface ISyncDataApiService {
    /**
     * 根据 sync-data-xxx-dev.yml中 schedules 下配置执行
     * @param scheduleKey
     */
    @ApiOperation("同步数据方法")
    @PostMapping(value = "/schedule/runSyncData")
    AjaxResult runSyncData(@RequestParam("scheduleKey") String scheduleKey);

    /**
     * 自定义请求方法
     * {"methodFrom":"syncKeys", syncKeys:"xxx,xxxx"}
     * - methodFrom: syncKeys 通过 syncKeys 获取需要请求或通知的接口
     * - methodFrom: custom, method: "methodName"
     * @param params
     * @return
     */
    @ApiOperation("自定义请求方法")
    @PostMapping(value = "/schedule/runSyncHandle")
    AjaxResult runSyncHandle(@RequestParam("params") String params);
}
