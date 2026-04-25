package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
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
     * 导入结构整车胎面配置数据
     *
     * @param list 导入数据
     * @param updateSupport 是否更新已存在数据
     * @param importLogId 导入日志ID
     * @return 导入结果
     */
    AjaxResult importData(List<CxStructureTreadConfig> list, boolean updateSupport, Long importLogId);

    /**
     * 校验工厂+结构唯一性
     *
     * @param entity 结构整车胎面配置
     * @return 唯一性校验结果
     */
    String checkUnique(CxStructureTreadConfig entity);

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<CxStructureTreadConfig> list);
}
