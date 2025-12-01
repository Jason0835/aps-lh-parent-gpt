package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanAdjustDetail;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanAdjustDetailVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 计划调整明细SQL接口定义类
 *
 * @author ZLT
 * @date 20250603
 */
@Mapper
public interface MonthPlanAdjustDetailMapper extends CommBaseMapper<MonthPlanAdjustDetail> {
    /**
     * 获取调整通知单的调整明细信息
     *
     * @param noticeNo
     * @return
     */
    List<MonthPlanAdjustDetailVo> getNoticeDetail(@Param("noticeNo") String noticeNo);
}
