package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 斜裁卷曲长度业务接口。
 */
public interface ICd15CurlLengthService extends IDocService<Cd15CurlLength> {

    /**
     * 校验同工厂钢带代码唯一。
     *
     * @param entity 斜裁卷曲长度
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd15CurlLength entity);

    /**
     * 导入斜裁卷曲长度数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15CurlLength> list, boolean updateSupport, Long importLogId);
}