package com.zlt.aps.itf.mes.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.itf.vo.MesDjScheduleResult;

/**
 * MES垫胶排程结果中间表Mapper接口
 *
 * @author zlt
 */
public interface MesDjScheduleResultMapper extends BaseMapper<MesDjScheduleResult> {

    /**
     * 批量插入MES垫胶排程结果
     *
     * @param list 排程结果列表
     */
    void batchInsert(List<MesDjScheduleResult> list);
}
