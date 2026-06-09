package com.zlt.aps.dj.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.dj.api.domain.dto.DjExportLogManagementDto;
import com.zlt.aps.dj.api.domain.entity.DjExportLogManagement;
import com.zlt.aps.dj.mapper.DjExportLogManagementMapper;
import com.zlt.aps.dj.service.DjExportLogManagementService;

/**
 * 工序导出日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class DjExportLogManagementServiceImpl extends ServiceImpl<DjExportLogManagementMapper, DjExportLogManagement> implements DjExportLogManagementService {

    @Resource
    private DjExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<DjExportLogManagementDto> selectExportLogManagementList(DjExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
