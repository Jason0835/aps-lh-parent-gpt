package com.zlt.aps.itf.mes.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.itf.vo.MesNcScheduleResult;

/**
 * MES内衬排程结果中间表Mapper接口
 *
 * @author zlt
 */
public interface MesNcScheduleResultMapper extends BaseMapper<MesNcScheduleResult> {

    /**
     * 批量插入MES内衬排程结果
     *
     * @param list 排程结果列表
     */
    void batchInsert(List<MesNcScheduleResult> list);
}
