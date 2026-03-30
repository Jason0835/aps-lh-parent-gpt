package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "IMdmOutbountOrdersNotScanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmOutbountOrdersNotScanRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/mdmOutbountOrdersNotScan/list")
    TableDataInfo list(@RequestBody MdmOutbountOrdersNotScan queryVO);
}
