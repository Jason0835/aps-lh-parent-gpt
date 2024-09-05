package com.zlt.aps.mps.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.zlt.aps.mps.api.domain.MdmMonthPlanAnalysis;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 月计划汇总对外暴露接口
 * @author Gim
 */
@FeignClient(contextId = "IMonthPlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.mps:mps}")
public interface IMonthPlanService {
    String prefix = "/mps/monthPlan";

    @GetMapping(value = prefix + "/unit/{embryoCode}/{num}")
    MdmMonthPlanAnalysis getEmbryoConsumption(@PathVariable("embryoCode") String embryoCode, @PathVariable("num") Integer num);

    /**
     * 月计划汇总
     * @param planMainVersion 主计划版本
     * @param year 年份
     * @param month 月份
     * @param isFinal 是否定稿 0是1否
     */
    @PostMapping(value = prefix + "/sum/{planMainVersion}/{year}/{month}/{isFinal}")
    void monthPlanAmountSum(@PathVariable("planMainVersion") String planMainVersion,
                          @PathVariable("year") String year,
                          @PathVariable("month") String month,
                          @PathVariable("isFinal") Integer isFinal);

    /**
     * 外胎月结库存抓取通知接口
     * @param sapCode sap品号
     * @param date 月份 格式：yyyy-MM
     */
    @PutMapping(value = prefix + "/storNum/{sapCode}/{date}")
    void updateCxMonthStorNum(@PathVariable("sapCode") String sapCode,
                              @PathVariable("date") String date);

    /**
     * 不良抓取通知接口
     * @param sapCode       sap品号
     * @param embryoCode    胎胚代码
     * @param date      月份 格式：yyyy-MM-dd
     */
    @PutMapping(value = prefix + "/badNum/{sapCode}/{embryoCode}/{date}")
    void updateMonthBadNum(@PathVariable("sapCode") String sapCode,
                           @PathVariable("embryoCode") String embryoCode,
                           @PathVariable("date") String date);
}
