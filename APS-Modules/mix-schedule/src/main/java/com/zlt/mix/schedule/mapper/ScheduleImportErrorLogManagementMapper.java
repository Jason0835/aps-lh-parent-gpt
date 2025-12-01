package com.zlt.mix.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.schedule.api.domain.dto.ScheduleImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理错误信息Mapper接口
 */
public interface ScheduleImportErrorLogManagementMapper extends BaseMapper<ScheduleImportErrorLogManagementDto>
{
    /**
     * 根据条件工序导出日志管理
     * @return
     */
    List<ScheduleImportErrorLogManagementDto> listImportErrorLogManagement(ScheduleImportErrorLogManagementDto dto);

}
