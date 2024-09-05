package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportLogManagementDto;
import com.zlt.aps.cd15.entity.Cd15ImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface Cd15ImportLogManagementService extends IService<Cd15ImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<Cd15ImportLogManagementDto> selectImportLogManagementList(Cd15ImportLogManagementDto dto);
}
