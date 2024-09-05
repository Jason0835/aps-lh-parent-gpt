package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.tq.api.domain.dto.TqExportLogManagementDto;
import com.zlt.aps.tq.entity.TqExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface TqExportLogManagementService extends IService<TqExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<TqExportLogManagementDto> selectExportLogManagementList(TqExportLogManagementDto dto);

}
