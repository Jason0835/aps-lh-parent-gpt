package com.zlt.aps.nc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.nc.api.domain.dto.NcImportLogManagementDto;
import com.zlt.aps.nc.entity.NcImportLogManagement;
import com.zlt.aps.nc.mapper.NcImportLogManagementMapper;
import com.zlt.aps.nc.service.NcImportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导入日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class NcImportLogManagementServiceImpl extends ServiceImpl<NcImportLogManagementMapper, NcImportLogManagement> implements NcImportLogManagementService {

    @Resource
    private NcImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<NcImportLogManagementDto> selectImportLogManagementList(NcImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
