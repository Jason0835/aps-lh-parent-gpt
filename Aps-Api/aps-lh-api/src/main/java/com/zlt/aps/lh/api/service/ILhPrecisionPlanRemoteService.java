package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 硫化精度计划远程服务接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ILhPrecisionPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhPrecisionPlanRemoteService {

    /**
     * 从MES同步数据生成硫化精度初版计划
     *
     * @return 生成数量
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划")
    @PostMapping("/lhPrecisionPlan/generateFromMes")
    AjaxResult generatePlansFromMes();

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("自动生成年度硫化精度计划")
    @PostMapping("/lhPrecisionPlan/autoGenerateYearly")
    AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year);

    /**
     * 执行30天预警检查
     *
     * @return 预警数量
     */
    @ApiOperation("执行30天预警检查")
    @PostMapping("/lhPrecisionPlan/checkWarning")
    AjaxResult checkWarning();

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    @ApiOperation("批量更新到期天数")
    @PostMapping("/lhPrecisionPlan/batchUpdateDaysToDue")
    AjaxResult batchUpdateDaysToDue();

    /**
     * MES回传实际完成时间
     *
     * @param mesSourceId MES来源ID
     * @param actualDate 实际日期
     * @return 是否成功
     */
    @ApiOperation("MES回传实际完成时间")
    @PostMapping("/lhPrecisionPlan/updateActualDate")
    AjaxResult updateActualDate(@RequestParam("mesSourceId") Long mesSourceId, 
                                @RequestParam("actualDate") String actualDate);
}
