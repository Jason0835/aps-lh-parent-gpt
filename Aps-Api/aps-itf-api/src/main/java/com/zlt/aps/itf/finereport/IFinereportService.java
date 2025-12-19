package com.zlt.aps.itf.finereport;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;

import io.swagger.annotations.ApiOperation;

/**
 * 分厂月度计划控制台业务
 *
 * @author ZLT
 * @date 20250213
 */
@FeignClient(contextId = "IFinereportService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IFinereportService {
    /**
     * 同步已计划未发货数据
     *
     * @param planedNotShipParamVo 查询条件
     * @return 结果集合
     */
    @ApiOperation("同步已计划未发货数据")
    @GetMapping("/finereport/inventoryAgeAnalysis")
    AjaxResult inventoryAgeAnalysis();
    
}
