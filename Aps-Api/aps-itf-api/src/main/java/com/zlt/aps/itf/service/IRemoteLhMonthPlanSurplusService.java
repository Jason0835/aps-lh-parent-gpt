package com.zlt.aps.itf.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplusDetail;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 月度计划外胎汇总调用
 */
@FeignClient(contextId = "IRemoteLhMonthPlanSurplusService", name = "${remoteApi.value.cxlh:aps-cxlh}")
public interface IRemoteLhMonthPlanSurplusService {

    /**
     * 查询月度计划外胎汇总列表明细
     */
    @ApiOperation("查询月度计划外胎汇总列表明细")
    @PostMapping("/lhMonthPlanSurplus/detailList")
    TableDataInfo detailList(@RequestBody LhMonthPlanSurplusDetail QueryVO, @RequestParam("pageNum") Integer pageNum, @RequestParam("pageSize") Integer pageSize);
    
}
