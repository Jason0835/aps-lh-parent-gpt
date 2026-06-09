package com.zlt.aps.dj.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.dj.api.domain.dto.DjImportLogManagementDto;
import com.zlt.aps.dj.api.domain.entity.DjImportLogManagement;
import com.zlt.aps.dj.mapper.DjImportLogManagementMapper;
import com.zlt.aps.dj.service.DjImportLogManagementService;

/**
 * 工序导入日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class DjImportLogManagementServiceImpl extends ServiceImpl<DjImportLogManagementMapper, DjImportLogManagement> implements DjImportLogManagementService {

    @Resource
    private DjImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<DjImportLogManagementDto> selectImportLogManagementList(DjImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
