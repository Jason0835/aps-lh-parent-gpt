package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.tc.api.domain.dto.TcExportLogManagementDto;
import com.zlt.aps.tc.entity.TcExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface TcExportLogManagementService extends IService<TcExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<TcExportLogManagementDto> selectExportLogManagementList(TcExportLogManagementDto dto);

}
