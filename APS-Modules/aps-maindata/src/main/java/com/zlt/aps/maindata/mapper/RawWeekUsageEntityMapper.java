package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;
import com.zlt.aps.monthplan.api.domain.entity.RawWeekUsage;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author nick
 */
@Mapper
public interface RawWeekUsageEntityMapper extends CommBaseMapper<RawWeekUsage> {
    /**
     * 批量插入预警记录
     * @param list 预警记录列表
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<RawWarningRecord> list);

    /**
     * 批量更新预警记录
     * @param list 预警记录列表
     * @return 更新条数
     */
    int batchUpdateRawWarningRecord(@Param("list") List<RawWarningRecord> list);

    /**
     * 批量更新周用量记录
     * @param list 周用量记录列表
     * @return 更新条数
     */
    int batchUpdate(@Param("list") List<RawWeekUsage> list);

    /**
     * 批量更新实际用量和偏差数据
     * @param list 周用量记录列表
     * @return 更新条数
     */
    int batchUpdateActualAndDeviation(@Param("list") List<RawWeekUsage> list);

}
