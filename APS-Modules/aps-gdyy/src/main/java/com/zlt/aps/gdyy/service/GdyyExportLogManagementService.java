package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.gdyy.api.domain.dto.GdyyExportLogManagementDto;
import com.zlt.aps.gdyy.entity.GdyyExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface GdyyExportLogManagementService extends IService<GdyyExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<GdyyExportLogManagementDto> selectExportLogManagementList(GdyyExportLogManagementDto dto);

}
