package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


/**
 * 成型排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-12
 */
@FeignClient(contextId = "iCxScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxSchedulingAlgorithmService {

    @PutMapping("/cxSchedulingAlgorithm/calculateCarbonationPlan")
    AjaxResult calculateCarbonationPlan(@RequestBody CxScheduleResult cxScheduleResult);

}
