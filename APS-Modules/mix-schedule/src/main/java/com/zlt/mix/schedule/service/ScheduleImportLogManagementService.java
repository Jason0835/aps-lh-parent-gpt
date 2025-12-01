package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.entity.ScheduleImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 */
public interface ScheduleImportLogManagementService extends IService<ScheduleImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<ScheduleImportLogManagement> selectImportLogManagementList(ScheduleImportLogManagement dto);
}
