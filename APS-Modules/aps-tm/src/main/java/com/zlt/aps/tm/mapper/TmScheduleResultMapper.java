package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面排程结果表 Mapper接口
 */
@Mapper
public interface TmScheduleResultMapper extends CommBaseMapper<TmScheduleResult> {

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录数
     * @param ids id数组
     * @return 符合条件的记录数
     */
    int isReleasingOrTimeoutByIds(Long[] ids);
}
