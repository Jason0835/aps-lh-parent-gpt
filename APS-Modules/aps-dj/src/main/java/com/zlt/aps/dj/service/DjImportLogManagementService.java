package com.zlt.aps.dj.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.dj.api.domain.dto.DjImportLogManagementDto;
import com.zlt.aps.dj.api.domain.entity.DjImportLogManagement;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface DjImportLogManagementService extends IService<DjImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<DjImportLogManagementDto> selectImportLogManagementList(DjImportLogManagementDto dto);
}
