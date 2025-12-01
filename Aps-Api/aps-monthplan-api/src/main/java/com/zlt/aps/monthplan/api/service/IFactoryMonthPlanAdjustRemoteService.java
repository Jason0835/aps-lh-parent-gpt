package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanAdjustPlanVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 分厂月度计划调整服务接口
 *
 * @author ZLT
 * @date 20250213
 */
@FeignClient(contextId = "IFactoryMonthPlanAdjustRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryMonthPlanAdjustRemoteService {
    /**
     * 根据分厂、年份、月份获取调整控制信息对象
     *
     * @param param 查询条件
     * @return 控制信息
     */
    @ApiOperation("根据分厂、年份、月份获取调整控制信息对象")
    @PostMapping("/monthPlanAdjust/getAdjustControlInfo")
    AjaxResult getAdjustControlInfo(@RequestBody FactoryMonthPlanProdResultDto param);

    /**
     * 对分厂月计划执行调整
     *
     * @param adjustPlan 查询条件
     * @return
     */
    @ApiOperation("对分厂月计划执行调整")
    @PostMapping("/monthPlanAdjust/adjustFactoryMonthPlan")
    AjaxResult adjustMonthPlan(@RequestBody FactoryMonthPlanAdjustPlanVo adjustPlan);
}
