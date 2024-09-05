package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd15.api.domain.dto.Cd15ExportLogManagementDto;
import com.zlt.aps.cd15.entity.Cd15ExportLogManagement;
import com.zlt.aps.cd15.mapper.Cd15ExportLogManagementMapper;
import com.zlt.aps.cd15.service.Cd15ExportLogManagementService;
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
public class Cd15ExportLogManagementServiceImpl extends ServiceImpl<Cd15ExportLogManagementMapper, Cd15ExportLogManagement> implements Cd15ExportLogManagementService {

    @Resource
    private Cd15ExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<Cd15ExportLogManagementDto> selectExportLogManagementList(Cd15ExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
