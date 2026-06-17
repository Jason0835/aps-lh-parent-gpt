package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 15度裁断参数设置 服务层
 */
public interface ICd15ParamsService extends IDocService<Cd15Params> {
    /**
     * 校验唯一性
     * @param entity 实体
     * @return 唯一返回UNIQUE，否则返回NOT_UNIQUE
     */
    String checkUnique(Cd15Params entity);

    /**
     * 导入数据
     * @param list 数据列表
     * @param updateSupport 是否更新已存在数据
     * @param importLogId 导入日志ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15Params> list, boolean updateSupport, Long importLogId);
}