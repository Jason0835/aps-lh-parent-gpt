package com.zlt.aps.dj.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.dj.api.domain.dto.DjImportErrorLogManagementDto;
import com.zlt.aps.dj.mapper.DjImportErrorLogManagementMapper;
import com.zlt.aps.dj.service.DjImportErrorLogManagementService;

/**
 * 工序导入日志管理错误日志Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class DjImportErrorLogManagementServiceImpl extends ServiceImpl<DjImportErrorLogManagementMapper, DjImportErrorLogManagementDto> implements DjImportErrorLogManagementService {

    @Resource
    private DjImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<DjImportErrorLogManagementDto> selectImportErrorLogManagementList(DjImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
