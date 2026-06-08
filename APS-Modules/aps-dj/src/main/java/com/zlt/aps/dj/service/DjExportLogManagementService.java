package com.zlt.aps.dj.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.dj.api.domain.dto.DjExportLogManagementDto;
import com.zlt.aps.dj.api.domain.entity.DjExportLogManagement;

/**
 * 工序导出日志管理Service接口
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
public interface DjExportLogManagementService extends IService<DjExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<DjExportLogManagementDto> selectExportLogManagementList(DjExportLogManagementDto dto);

}
