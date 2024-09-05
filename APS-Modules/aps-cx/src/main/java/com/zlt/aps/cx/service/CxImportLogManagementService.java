package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.api.domain.dto.CxImportLogManagementDto;
import com.zlt.aps.cx.entity.CxImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface CxImportLogManagementService extends IService<CxImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<CxImportLogManagementDto> selectImportLogManagementList(CxImportLogManagementDto dto);
}
