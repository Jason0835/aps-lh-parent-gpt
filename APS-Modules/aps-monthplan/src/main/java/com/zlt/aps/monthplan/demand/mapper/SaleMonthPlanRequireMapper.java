package com.zlt.aps.monthplan.demand.mapper;

import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.SaleMonthPlanRequireReportVo;
import com.zlt.aps.monthplan.demand.controller.TempProductionDto;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 分厂与计划SQL接口定义Mapper
 *
 * @author ZLT
 * @date 20250211
 */
@Mapper
public interface SaleMonthPlanRequireMapper extends CommBaseMapper<SaleMonthPlanRequire> {

    /**
     * 判断销售生产需求计划是否存在，大于1标识存在，否则不存在
     *
     * @param monthPlanVersion 销售生产需求版本
     * @return
     */
    int exist(String monthPlanVersion);

    /**
     * 根据查询条件，获取分厂对应的生产计划版本记录列表
     *
     * @param queryCondition
     * @return
     */
    List<FactoryProductionPlanVersionDto> getProductionVersionList(FactoryProductionPlanVo queryCondition);

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    List<String> versionList(SaleMonthPlanRequire query);

    /**
     * 根据版本号，删除数据
     *
     * @param monthPlanVersion
     * @return
     */
    int deleteByVersion(String monthPlanVersion);

    /**
     * 获取已产数量
     *
     * @return
     */
    List<TempProductionDto> getProductionQty();

    /**
     * 根据条件查询统计数据
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    SaleMonthPlanRequireReportVo getSummaryVo(SaleMonthPlanRequire queryVO);
}
