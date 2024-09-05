package com.zlt.aps.nc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.nc.api.domain.dto.NcImportErrorLogManagementDto;
import com.zlt.aps.nc.mapper.NcImportErrorLogManagementMapper;
import com.zlt.aps.nc.service.NcImportErrorLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工序导入日志管理错误日志Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class NcImportErrorLogManagementServiceImpl extends ServiceImpl<NcImportErrorLogManagementMapper, NcImportErrorLogManagementDto> implements NcImportErrorLogManagementService {

    @Resource
    private NcImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<NcImportErrorLogManagementDto> selectImportErrorLogManagementList(NcImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
