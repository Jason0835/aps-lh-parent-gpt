package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.monthplan.api.domain.vo.LhMonthPlanSurplusDetailVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 月度计划外胎汇总明细Mapper接口
 *
 * @author Liam
 * @since 2025/4/2
 */
@Mapper
public interface LhMonthPlanSurplusDetailMapper extends CommBaseMapper<LhMonthPlanSurplusDetail> {
    /**
     * 查询对应外胎汇总明细
     */
    List<LhMonthPlanSurplusDetail> selectParamDetailList(List<LhMonthPlanSurplusDetail> list);

    /**
     * 根据外胎汇总记录查询对应外胎汇总明细
     */
    List<LhMonthPlanSurplusDetail> selectParamList(List<LhMonthPlanSurplus> list);

    /**
     * 查询外胎汇总明细
     */
    List<LhMonthPlanSurplusDetailVo> selectDetailList(LhMonthPlanSurplusDetail queryVO);
}
