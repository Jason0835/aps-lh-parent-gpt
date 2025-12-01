package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.dto.ScheduleImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 */
public interface ScheduleImportErrorLogManagementService extends IService<ScheduleImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     */
    List<ScheduleImportErrorLogManagementDto> selectImportErrorLogManagementList(ScheduleImportErrorLogManagementDto dto);
}
