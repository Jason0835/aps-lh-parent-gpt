package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportErrorLogManagementDto;
import com.zlt.aps.gdyy.mapper.GdyyImportErrorLogManagementMapper;
import com.zlt.aps.gdyy.service.GdyyImportErrorLogManagementService;
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
public class GdyyImportErrorLogManagementServiceImpl extends ServiceImpl<GdyyImportErrorLogManagementMapper, GdyyImportErrorLogManagementDto> implements GdyyImportErrorLogManagementService {

    @Resource
    private GdyyImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<GdyyImportErrorLogManagementDto> selectImportErrorLogManagementList(GdyyImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
