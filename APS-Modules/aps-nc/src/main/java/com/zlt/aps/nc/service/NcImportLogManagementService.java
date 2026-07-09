package com.zlt.aps.nc.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.nc.api.domain.dto.NcImportLogManagementDto;
import com.zlt.aps.nc.entity.NcImportLogManagement;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2026-06-07
 */
public interface NcImportLogManagementService extends IService<NcImportLogManagement> {
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理 * @return 工序导入日志管理集合
     */
    List<NcImportLogManagementDto> selectImportLogManagementList(NcImportLogManagementDto dto);
}
