package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cd90.api.domain.dto.Cd90ExportLogManagementDto;
import com.zlt.aps.cd90.entity.Cd90ExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface Cd90ExportLogManagementService extends IService<Cd90ExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<Cd90ExportLogManagementDto> selectExportLogManagementList(Cd90ExportLogManagementDto dto);

}
