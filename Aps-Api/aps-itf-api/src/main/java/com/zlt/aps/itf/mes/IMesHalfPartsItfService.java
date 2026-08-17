package com.zlt.aps.itf.mes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.MesDjScheduleResult;
import com.zlt.aps.itf.vo.MesNcScheduleResult;

import io.swagger.annotations.ApiOperation;

/**
 * MES接口业务
 *
 * @author zlt
 * @since 2026/07/29
 */
@FeignClient(contextId = "IMesItfService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IMesHalfPartsItfService {

    /**
     * 同步垫胶库存
     *
     * @return 结果
     */
    @ApiOperation("同步垫胶库存")
    @PostMapping("/mesHalfPartsItf/syncDjStock")
    public AjaxResult syncDjStock();

    /**
     * 同步内衬库存
     *
     * @return 结果
     */
    @ApiOperation("同步内衬库存")
    @PostMapping("/mesHalfPartsItf/syncNcStock")
    public AjaxResult syncNcStock();
    
    


    @ApiOperation("下发内衬排程")
    @PostMapping("/mesHalfPartsItf/issueNcScheduleResult")
    public AjaxResult issueNcScheduleResult(@RequestBody MesNcScheduleResult mesNcScheduleResult);

    @ApiOperation("下发垫胶排程")
    @PostMapping("/mesHalfPartsItf/issueDjScheduleResult")
    public AjaxResult issueDjScheduleResult(@RequestBody MesDjScheduleResult mesDjScheduleResult);
}
