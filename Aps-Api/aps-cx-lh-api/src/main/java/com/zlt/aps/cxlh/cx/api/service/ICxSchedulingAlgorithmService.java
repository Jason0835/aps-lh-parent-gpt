package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResultOld;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * 成型排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-12
 */
@FeignClient(contextId = "iCxScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxSchedulingAlgorithmService {

    @PutMapping("/cxSchedulingAlgorithm/calculateCarbonationPlan")
    AjaxResult calculateCarbonationPlan(@RequestBody CxScheduleResultOld cxScheduleResult);

}
