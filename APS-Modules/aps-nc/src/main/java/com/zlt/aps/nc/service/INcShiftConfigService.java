package com.zlt.aps.nc.service;

import com.zlt.aps.nc.api.domain.entity.NcShiftConfig;
import com.zlt.bill.common.service.IDocService;

/**
 * 内衬班制配置Service接口
 *
 * @author zlt
 */
public interface INcShiftConfigService extends IDocService<NcShiftConfig> {

    /**
     * 获取当前开班的所有班次配置，按 SHIFT_ORDER 排序
     *
     * @return 班次配置列表
     */
    java.util.List<NcShiftConfig> listActiveShifts();
}
