package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 * 
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface Cd15ImportErrorLogManagementService extends IService<Cd15ImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     * @param id 工序导入日志管理id     * @return 工序导入日志管理错误日志集合
     */
    List<Cd15ImportErrorLogManagementDto> selectImportErrorLogManagementList(Cd15ImportErrorLogManagementDto dto);
}
