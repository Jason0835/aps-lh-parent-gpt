package com.zlt.aps.dj.service;

import com.zlt.aps.dj.api.domain.entity.DjShiftConfig;
import com.zlt.bill.common.service.IDocService;

/**
 * 垫胶班制配置Service接口
 *
 * @author zlt
 */
public interface IDjShiftConfigService extends IDocService<DjShiftConfig> {

    /**
     * 获取当前开班的所有班次配置，按 SHIFT_ORDER 排序
     *
     * @return 班次配置列表
     */
    java.util.List<DjShiftConfig> listActiveShifts();
}
