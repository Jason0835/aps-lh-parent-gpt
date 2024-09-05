package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.xwyy.api.domain.dto.XwyyExportLogManagementDto;
import com.zlt.aps.xwyy.entity.XwyyExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface XwyyExportLogManagementService extends IService<XwyyExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<XwyyExportLogManagementDto> selectExportLogManagementList(XwyyExportLogManagementDto dto);

}
