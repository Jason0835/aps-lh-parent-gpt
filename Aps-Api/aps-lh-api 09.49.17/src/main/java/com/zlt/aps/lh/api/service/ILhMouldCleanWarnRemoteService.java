package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 模具清洗预警Feign接口
 *
 * @author APS Team
 * @since 2026/04/10
 */
@FeignClient(contextId = "ILhMouldCleanWarnRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMouldCleanWarnRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/mouldCleanWarn/list")
    TableDataInfo list(@RequestBody LhMouldCleanWarn query);

    @ApiOperation("获取详细信息")
    @GetMapping("/mouldCleanWarn/{id}")
    LhMouldCleanWarn getInfo(@PathVariable("id") Long id);

    @ApiOperation("导出数据")
    @PostMapping("/mouldCleanWarn/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMouldCleanWarn queryVO, @PathVariable("fileName") String fileName);
}
