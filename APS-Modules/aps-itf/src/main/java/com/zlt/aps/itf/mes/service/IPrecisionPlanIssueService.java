package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue;

import java.util.List;

/**
 * 精度计划下发服务接口
 * 统一处理成型精度计划和硫化精度计划的下发到MES中间表
 * 成型精度和硫化精度统一写入MES_PRECISION_PLAN表，通过PRECISION_TYPE区分
 *
 * @author APS Team
 */
public interface IPrecisionPlanIssueService {

    /**
     * 精度计划下发到MES
     * 将计划排程精度日期有值且实际执行日期为空的精度计划下发到MES中间表MES_PRECISION_PLAN
     * precisionType值直接存"硫化精度"或"成型精度"
     *
     * @param lhPrecisionPlanIssueList 精度计划列表
     * @param factoryCode 厂别
     * @param companyCode 分公司编码
     * @return 下发结果
     */
    AjaxResult issueLhPrecisionPlan(List<LhPrecisionPlanIssue> lhPrecisionPlanIssueList, String factoryCode, String companyCode);
}
