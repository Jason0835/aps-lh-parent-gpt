package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.mapper.CxPlanProductStatusMapper;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
  *  月度计划成型工序投产状态信息逻辑层实现
  * @ClassName CxPlanProductStatusServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/7/9 14:25
  * @Version 1.0
**/
@Service("cxPlanProductStatusService")
public class CxPlanProductStatusServiceImpl implements CxPlanProductStatusService {

    @Autowired
    private CxPlanProductStatusMapper cxPlanProductStatusMapper;

    @Override
    public List<CxPlanProductStatus> loadAviableProductList(CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusMapper.selectCxPlanProductStatusList(cxPlanProductStatus);
    }

    @Override
    public int updateCxPlanProductStatusById(CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusMapper.updateCxPlanProductStatus(cxPlanProductStatus);
    }

    @Override
    public int updatePlanProductStatus(CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusMapper.updatePlanProductStatus(cxPlanProductStatus);
    }

    /**
     * 更新投产表中投产状态
     * @param cxPlanProductStatus
     * @return
     */
    @Override
    public int updatePlanProductToProduction(CxPlanProductStatus cxPlanProductStatus) {
        cxPlanProductStatus.setProductStatus(CxEngineConstants.MDM_PLAN_PRODUCT_STATUS_WAIT);//Joran 2021-08-04 调整为待发布状态，发布成功后变更为已投产状态
        cxPlanProductStatus.setUpdateTime(DateUtils.getNowDate());
        return updatePlanProductStatus(cxPlanProductStatus);
    }

    /**
     * 提供导入进行投产表状态更新为投产待发布状态
     * @param updateList
     * @return
     */
    @Override
    public int updateProductStatusToWaitPublish(List<CxPlanProductStatus> updateList) {
        return cxPlanProductStatusMapper.updateProductStatusToWaitPublish(updateList);
    }

    /**
     * 提供自动排程校验存在排程结果表中则进行移除投产状态变更为已投产
     * @param updateList
     * @return
     */
    @Override
    public int updateProductStatusToProduct(List<CxPlanProductStatus> updateList) {
        return cxPlanProductStatusMapper.updateProductStatusToProduct(updateList);
    }

    /**
     * 根据版本和排程月份进行投产表批量置为投产
     * @param monthPlanApsVersion
     * @param scheduleMonth
     * @return
     */
    @Override
    public int batchUpdateProductStatusToProduct(String monthPlanApsVersion, String scheduleMonth) {
        //1、根据排程计划进行更新投产表
        int updateByResult=cxPlanProductStatusMapper.batchUpdateProductStatusToProduct(monthPlanApsVersion,scheduleMonth);
        //2、根据胎胚维度汇总表存在成型完成量的都标记为已投产
         updateByResult+=cxPlanProductStatusMapper.batchUpdateProductStatusToProductBySurplus(monthPlanApsVersion);
        return updateByResult;
    }
}
