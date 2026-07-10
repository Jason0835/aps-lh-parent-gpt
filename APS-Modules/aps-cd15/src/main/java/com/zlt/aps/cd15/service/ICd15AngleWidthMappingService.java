package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * CD15角度宽度对应关系服务层
 */
public interface ICd15AngleWidthMappingService extends IDocService<Cd15AngleWidthMapping> {
    /**
     * 校验唯一性
     * @param entity 实体
     * @return 唯一返回UNIQUE，否则返回NOT_UNIQUE
     */
    String checkUnique(Cd15AngleWidthMapping entity);

    /**
     * 导入数据
     * @param list 数据列表
     * @param updateSupport 是否更新已存在数据
     * @param importLogId 导入日志ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15AngleWidthMapping> list, boolean updateSupport, Long importLogId);
}
