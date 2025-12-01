package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.schedule.api.domain.entity.ScheduleImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 */
public interface ScheduleImportLogManagementMapper extends BaseMapper<ScheduleImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<ScheduleImportLogManagement> listImportLogManagement(ScheduleImportLogManagement dto);

}
