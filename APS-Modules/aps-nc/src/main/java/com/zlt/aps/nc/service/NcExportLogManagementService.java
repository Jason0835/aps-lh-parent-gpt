package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.nc.api.domain.dto.NcExportLogManagementDto;
import com.zlt.aps.nc.entity.NcExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface NcExportLogManagementService extends IService<NcExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<NcExportLogManagementDto> selectExportLogManagementList(NcExportLogManagementDto dto);

}
