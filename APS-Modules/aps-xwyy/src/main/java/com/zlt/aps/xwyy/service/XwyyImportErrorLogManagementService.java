package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface XwyyImportErrorLogManagementService extends IService<XwyyImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     * @param id 工序导入日志管理id     * @return 工序导入日志管理错误日志集合
     */
    List<XwyyImportErrorLogManagementDto> selectImportErrorLogManagementList(XwyyImportErrorLogManagementDto dto);
}
