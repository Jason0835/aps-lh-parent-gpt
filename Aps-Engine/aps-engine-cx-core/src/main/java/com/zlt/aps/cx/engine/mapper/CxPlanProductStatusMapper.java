package com.zlt.aps.cx.engine.mapper;



import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型工序月计划投产状态信息mapper接口
 */
public interface CxPlanProductStatusMapper {

    /**
     * 获取月度计划投产状态信息
     * @param cxPlanProductStatus
     * @return
     */
    public List<CxPlanProductStatus> selectCxPlanProductStatusList(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 更新投产信息表
     * @param cxPlanProductStatus
     * @return
     */
    public int updateCxPlanProductStatus(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 更新投产状态
     * @param cxPlanProductStatus
     * @return
     */
    public int updatePlanProductStatus(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 提供导入进行投产表状态更新为投产待发布状态
     * @param updateList
     * @return
     */
    int updateProductStatusToWaitPublish(@Param("list") List<CxPlanProductStatus> updateList);

    /**
     * 批量将投产表更新为已投产状态
     * @param updateList
     * @return
     */
    int updateProductStatusToProduct(@Param("list") List<CxPlanProductStatus> updateList);

    /**
     * 投产表根据排程进行处理
     * @param monthPlanApsVersion
     * @param scheduleMonth
     * @return
     */
    int batchUpdateProductStatusToProduct(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("scheduleMonth") String scheduleMonth);

    /**
     * 投产表根据胎胚维度汇总有成型完成量进行变更为已投产，后续需要再进行投产手工进行投产
     * @param monthPlanApsVersion
     * @return
     */
    int batchUpdateProductStatusToProductBySurplus(@Param("monthPlanApsVersion") String monthPlanApsVersion);
}
