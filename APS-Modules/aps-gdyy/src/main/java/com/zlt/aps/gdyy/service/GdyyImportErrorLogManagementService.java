package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface GdyyImportErrorLogManagementService extends IService<GdyyImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     * @param id 工序导入日志管理id     * @return 工序导入日志管理错误日志集合
     */
    List<GdyyImportErrorLogManagementDto> selectImportErrorLogManagementList(GdyyImportErrorLogManagementDto dto);
}
