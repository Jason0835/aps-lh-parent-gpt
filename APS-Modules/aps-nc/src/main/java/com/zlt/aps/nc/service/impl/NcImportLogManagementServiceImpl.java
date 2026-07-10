package com.zlt.aps.nc.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcImportLogManagementDto;
import com.zlt.aps.nc.entity.NcImportLogManagement;
import com.zlt.aps.nc.mapper.NcImportLogManagementMapper;
import com.zlt.aps.nc.service.NcImportLogManagementService;

/**
 * 工序导入日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class NcImportLogManagementServiceImpl extends ServiceImpl<NcImportLogManagementMapper, NcImportLogManagement> implements NcImportLogManagementService {

    @Autowired
    private NcImportLogManagementMapper ncImportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<NcImportLogManagementDto> selectImportLogManagementList(NcImportLogManagementDto dto) {
        LambdaQueryWrapper<NcImportLogManagement> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (StringUtils.isNotEmpty(dto.getProcedureCode())) {
                wrapper.like(NcImportLogManagement::getProcedureCode, dto.getProcedureCode());
            }
            if (StringUtils.isNotEmpty(dto.getFunctionCode())) {
                wrapper.like(NcImportLogManagement::getFunctionCode, dto.getFunctionCode());
            }
            if (StringUtils.isNotEmpty(dto.getFunctionName())) {
                wrapper.like(NcImportLogManagement::getFunctionName, dto.getFunctionName());
            }
            if (dto.getId() != null) {
                wrapper.eq(NcImportLogManagement::getId, dto.getId());
            }
        }
        wrapper.orderByDesc(NcImportLogManagement::getId);
        List<NcImportLogManagement> list = ncImportLogManagementMapper.selectList(wrapper);
        return list.stream().map(entity -> {
            NcImportLogManagementDto resultDto = new NcImportLogManagementDto();
            BeanUtils.copyProperties(entity, resultDto);
            return resultDto;
        }).collect(Collectors.toList());
    }
}
