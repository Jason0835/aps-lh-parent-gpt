package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 直裁共用下拉数据 Feign 接口。
 */
@FeignClient(contextId = "ICd90CommonRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90CommonRemoteService {

    /**
     * 胎体布代号下拉。
     *
     * @return 去重后的胎体布代号列表
     */
    @ApiOperation("胎体布代号下拉")
    @PostMapping("/common/tireFabricCodes")
    AjaxResult tireFabricCodes();

    /**
     * 大卷代码（CORD_SPEC）下拉。
     *
     * @return 去重后的大卷代码列表
     */
    @ApiOperation("大卷代码下拉")
    @PostMapping("/common/cordSpecs")
    AjaxResult cordSpecs();
}
