package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.gsq.api.domain.dto.GsqImportLogManagementDto;
import com.zlt.aps.gsq.entity.GsqImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface GsqImportLogManagementService extends IService<GsqImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<GsqImportLogManagementDto> selectImportLogManagementList(GsqImportLogManagementDto dto);
}
