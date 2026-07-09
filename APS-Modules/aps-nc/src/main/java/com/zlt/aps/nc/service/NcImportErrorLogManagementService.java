package com.zlt.aps.nc.service;

import com.zlt.aps.nc.api.domain.dto.NcImportErrorLogManagementDto;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface NcImportErrorLogManagementService extends IDocService<NcImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     * @param id 工序导入日志管理id     * @return 工序导入日志管理错误日志集合
     */
    List<NcImportErrorLogManagementDto> selectImportErrorLogManagementList(NcImportErrorLogManagementDto dto);
}
