package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.tm.api.domain.dto.TmImportLogManagementDto;
import com.zlt.aps.tm.entity.TmImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface TmImportLogManagementService extends IService<TmImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<TmImportLogManagementDto> selectImportLogManagementList(TmImportLogManagementDto dto);
}
