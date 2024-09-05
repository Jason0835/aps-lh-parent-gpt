package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportErrorLogManagementDto;
import com.zlt.aps.cd15.mapper.Cd15ImportErrorLogManagementMapper;
import com.zlt.aps.cd15.service.Cd15ImportErrorLogManagementService;
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
public class Cd15ImportErrorLogManagementServiceImpl extends ServiceImpl<Cd15ImportErrorLogManagementMapper, Cd15ImportErrorLogManagementDto> implements Cd15ImportErrorLogManagementService {

    @Resource
    private Cd15ImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<Cd15ImportErrorLogManagementDto> selectImportErrorLogManagementList(Cd15ImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
