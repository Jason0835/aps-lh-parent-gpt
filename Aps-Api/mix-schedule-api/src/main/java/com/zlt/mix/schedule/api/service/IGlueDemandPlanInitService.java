package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 分厂胶料需求计划（初始表）Service接口
 *
 * @author Gim
 * @date 2022-04-05
 */
@FeignClient(contextId = "IGlueDemandPlanInitService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueDemandPlanInitService {

    /**
     * 查询分厂胶料需求计划（初始表）列表
     */
    @PostMapping("/factoryGluePlanStatistics/list")
    TableDataInfo listGlueDemandPlanInit(@RequestBody GlueDemandPlanInit glueDemandPlanInit);

    /**
     * 导出分厂胶料需求计划（初始表）列表
     */
    @PostMapping("/factoryGluePlanStatistics/exportData")
    byte[] exportData(@RequestBody GlueDemandPlanInit glueDemandPlanInit);

}
