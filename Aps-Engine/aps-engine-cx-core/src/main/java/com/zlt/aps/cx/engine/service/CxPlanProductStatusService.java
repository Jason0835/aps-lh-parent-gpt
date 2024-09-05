package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;

import java.util.List;

/**
  * 月度计划成型工序投产状态信息表
  * @ClassName CxPlanProductStatusService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/7/9 14:23
  * @Version 1.0
**/
public interface CxPlanProductStatusService {

    /**
     * 加载未投产计划信息
     * @return
     */
    public List<CxPlanProductStatus> loadAviableProductList(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 根据ID进行投产状态信息更新
     * @param cxPlanProductStatus
     * @return
     */
    public int updateCxPlanProductStatusById(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 更新投产状态
     * @param cxPlanProductStatus
     * @return
     */
    public int updatePlanProductStatus(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 更新为已投产状态
     * @param cxPlanProductStatus
     * @return
     */
    public int updatePlanProductToProduction(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 提供导入进行投产表状态更新为投产待发布状态
     * @param updateList
     * @return
     */
    int updateProductStatusToWaitPublish(List<CxPlanProductStatus> updateList);

    /**
     * 提供自动排程校验存在排程结果表中则进行移除投产状态变更为已投产
     * @param updateList
     * @return
     */
    int updateProductStatusToProduct(List<CxPlanProductStatus> updateList);

    /**
     * 根据生产版本号和排程月份进行投产状态批量更新
     * @param monthPlanApsVersion
     * @param scheduleMonth
     * @return
     */
    int batchUpdateProductStatusToProduct(String monthPlanApsVersion, String scheduleMonth);

}
