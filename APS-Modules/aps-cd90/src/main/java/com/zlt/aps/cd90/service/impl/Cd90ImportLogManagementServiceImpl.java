package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportLogManagementDto;
import com.zlt.aps.cd90.entity.Cd90ImportLogManagement;
import com.zlt.aps.cd90.mapper.Cd90ImportLogManagementMapper;
import com.zlt.aps.cd90.service.Cd90ImportLogManagementService;
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
public class Cd90ImportLogManagementServiceImpl extends ServiceImpl<Cd90ImportLogManagementMapper, Cd90ImportLogManagement> implements Cd90ImportLogManagementService {

    @Resource
    private Cd90ImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<Cd90ImportLogManagementDto> selectImportLogManagementList(Cd90ImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
