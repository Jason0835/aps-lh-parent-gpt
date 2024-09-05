package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportErrorLogManagementDto;
import com.zlt.aps.cd90.mapper.Cd90ImportErrorLogManagementMapper;
import com.zlt.aps.cd90.service.Cd90ImportErrorLogManagementService;
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
public class Cd90ImportErrorLogManagementServiceImpl extends ServiceImpl<Cd90ImportErrorLogManagementMapper, Cd90ImportErrorLogManagementDto> implements Cd90ImportErrorLogManagementService {

    @Resource
    private Cd90ImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<Cd90ImportErrorLogManagementDto> selectImportErrorLogManagementList(Cd90ImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
