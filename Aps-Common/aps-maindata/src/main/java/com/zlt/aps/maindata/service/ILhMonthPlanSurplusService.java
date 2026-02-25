package com.zlt.aps.maindata.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.mp.api.domain.vo.LhMonthPlanSurplusDetailVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMonthPlanSurplusService.java
 * 描    述：ILhMonthPlanSurplusService月度计划外胎汇总后端接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ILhMonthPlanSurplusService  extends IDocService<LhMonthPlanSurplus>{


    /**
     * 根据分厂编码和规格代码查询
     * @param factoryCode
     * @param specCodes
     * @return
     */
    List<LhMonthPlanSurplus> queryByFactoryAndSpecCodes(String factoryCode,Set<String> specCodes,Integer year,Integer month);

    /**
     * 更新指定年、月的月度计划外胎汇总明细的完成量
     *
     * @param year        年
     * @param month       月
     * @return 结果
     */
    AjaxResult updateMonthPlanSurplus(int year, int month);

    /**
     * 重算分配明细完成量
     * 
     * @param lhMonthPlanSurpluses 月度计划外胎汇总对象列表
     */
    void reAssignmentFinishQty(List<LhMonthPlanSurplus> lhMonthPlanSurpluses);

    /**
     * 查询月度外胎汇总
     */
    List<LhMonthPlanSurplusDetailVo> selectDetailList(LhMonthPlanSurplusDetail queryVO);
}
