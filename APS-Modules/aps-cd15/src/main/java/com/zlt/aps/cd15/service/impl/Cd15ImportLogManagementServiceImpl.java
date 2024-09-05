package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportLogManagementDto;
import com.zlt.aps.cd15.entity.Cd15ImportLogManagement;
import com.zlt.aps.cd15.mapper.Cd15ImportLogManagementMapper;
import com.zlt.aps.cd15.service.Cd15ImportLogManagementService;
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
public class Cd15ImportLogManagementServiceImpl extends ServiceImpl<Cd15ImportLogManagementMapper, Cd15ImportLogManagement> implements Cd15ImportLogManagementService {

    @Resource
    private Cd15ImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<Cd15ImportLogManagementDto> selectImportLogManagementList(Cd15ImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
