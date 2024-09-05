package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.tm.api.domain.dto.TmExportLogManagementDto;
import com.zlt.aps.tm.entity.TmExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface TmExportLogManagementService extends IService<TmExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<TmExportLogManagementDto> selectExportLogManagementList(TmExportLogManagementDto dto);

}
