package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 斜裁公用下拉数据 Feign 接口。
 */
@FeignClient(contextId = "ICd15CommonRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15CommonRemoteService {

    /**
     * 钢带代码下拉。
     *
     * @return 去重后的钢带代码列表
     */
    @ApiOperation("钢带代码下拉")
    @PostMapping("/common/steelStripCodes")
    AjaxResult steelStripCodes();
}