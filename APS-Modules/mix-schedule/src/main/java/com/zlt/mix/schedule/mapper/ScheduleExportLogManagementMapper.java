package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.schedule.api.domain.entity.ScheduleExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 */
public interface ScheduleExportLogManagementMapper extends BaseMapper<ScheduleExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<ScheduleExportLogManagement> listExportLogManagement(ScheduleExportLogManagement dto);

}
