package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** CD15斜裁未排结果远程服务。 */
@FeignClient(contextId = "ICd15UnscheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15UnscheduleResultRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/cd15UnscheduleResult/list")
    TableDataInfo list(@RequestBody Cd15UnscheduleResult queryVO);

    @ApiOperation("获取详情")
    @GetMapping("/cd15UnscheduleResult/getInfo/{id}")
    Cd15UnscheduleResult getInfo(@PathVariable("id") Long id);

    @ApiOperation("导出")
    @PostMapping("/cd15UnscheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15UnscheduleResult queryVO, @PathVariable("fileName") String fileName);
}