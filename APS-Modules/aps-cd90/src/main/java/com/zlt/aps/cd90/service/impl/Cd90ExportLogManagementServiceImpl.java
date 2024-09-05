package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd90.api.domain.dto.Cd90ExportLogManagementDto;
import com.zlt.aps.cd90.entity.Cd90ExportLogManagement;
import com.zlt.aps.cd90.mapper.Cd90ExportLogManagementMapper;
import com.zlt.aps.cd90.service.Cd90ExportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导出日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class Cd90ExportLogManagementServiceImpl extends ServiceImpl<Cd90ExportLogManagementMapper, Cd90ExportLogManagement> implements Cd90ExportLogManagementService {

    @Resource
    private Cd90ExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<Cd90ExportLogManagementDto> selectExportLogManagementList(Cd90ExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
