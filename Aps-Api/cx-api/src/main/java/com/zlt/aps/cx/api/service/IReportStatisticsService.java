package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.ReportStatisticsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 报表统计对外暴露接口
 * @author: Chen
 * @since: 2022/4/25 10:22
 */
@FeignClient(contextId = "IPlmParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IReportStatisticsService {

    /**
     * 根据查询条件查询报表统计列表
     */
    @PostMapping("/cx/reportStatistics/list")
    public TableDataInfo list(@RequestBody ReportStatisticsDto dto);

    /**
     * 导出报表统计数据
     */
    @PostMapping("/cx/reportStatistics/export")
    public byte[] export(@RequestBody ReportStatisticsDto dto);
}
