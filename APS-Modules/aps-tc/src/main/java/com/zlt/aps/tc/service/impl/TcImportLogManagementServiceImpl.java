package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tc.api.domain.dto.TcImportLogManagementDto;
import com.zlt.aps.tc.entity.TcImportLogManagement;
import com.zlt.aps.tc.mapper.TcImportLogManagementMapper;
import com.zlt.aps.tc.service.TcImportLogManagementService;
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
public class TcImportLogManagementServiceImpl extends ServiceImpl<TcImportLogManagementMapper, TcImportLogManagement> implements TcImportLogManagementService {

    @Resource
    private TcImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<TcImportLogManagementDto> selectImportLogManagementList(TcImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
