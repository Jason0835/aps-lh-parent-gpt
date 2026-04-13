package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 结构整车胎面配置Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ICxStructureTreadConfigService extends IDocService<CxStructureTreadConfig> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<CxStructureTreadConfig> list);
}
