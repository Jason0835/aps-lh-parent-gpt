package com.zlt.mix.schedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.schedule.api.domain.entity.ScheduleImportLogManagement;
import com.zlt.mix.schedule.mapper.ScheduleImportLogManagementMapper;
import com.zlt.mix.schedule.service.ScheduleImportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导入日志管理Service业务层处理
 */
@Service
public class ScheduleImportLogManagementServiceImpl extends ServiceImpl<ScheduleImportLogManagementMapper, ScheduleImportLogManagement> implements ScheduleImportLogManagementService {

    @Resource
    private ScheduleImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<ScheduleImportLogManagement> selectImportLogManagementList(ScheduleImportLogManagement dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
