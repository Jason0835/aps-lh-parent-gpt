package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.tq.api.domain.dto.TqImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface TqImportErrorLogManagementService extends IService<TqImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     * @param id 工序导入日志管理id     * @return 工序导入日志管理错误日志集合
     */
    List<TqImportErrorLogManagementDto> selectImportErrorLogManagementList(TqImportErrorLogManagementDto dto);
}
