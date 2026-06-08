package com.zlt.aps.dj.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.dj.api.domain.dto.DjImportErrorLogManagementDto;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface DjImportErrorLogManagementService extends IService<DjImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     * @param id 工序导入日志管理id     * @return 工序导入日志管理错误日志集合
     */
    List<DjImportErrorLogManagementDto> selectImportErrorLogManagementList(DjImportErrorLogManagementDto dto);
}
