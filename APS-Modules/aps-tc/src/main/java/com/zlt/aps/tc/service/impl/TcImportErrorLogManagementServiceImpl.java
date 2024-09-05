package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tc.api.domain.dto.TcImportErrorLogManagementDto;
import com.zlt.aps.tc.mapper.TcImportErrorLogManagementMapper;
import com.zlt.aps.tc.service.TcImportErrorLogManagementService;
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
public class TcImportErrorLogManagementServiceImpl extends ServiceImpl<TcImportErrorLogManagementMapper, TcImportErrorLogManagementDto> implements TcImportErrorLogManagementService {

    @Resource
    private TcImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<TcImportErrorLogManagementDto> selectImportErrorLogManagementList(TcImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
