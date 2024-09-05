package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportLogManagementDto;
import com.zlt.aps.cd90.entity.Cd90ImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface Cd90ImportLogManagementService extends IService<Cd90ImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<Cd90ImportLogManagementDto> selectImportLogManagementList(Cd90ImportLogManagementDto dto);
}
