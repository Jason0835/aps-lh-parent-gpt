package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 直裁卷曲长度业务接口。
 */
public interface ICd90CurlLengthService extends IDocService<Cd90CurlLength> {

    /**
     * 校验同工厂帘布代号唯一。
     *
     * @param entity 直裁卷曲长度
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd90CurlLength entity);

    /**
     * 导入直裁卷曲长度数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd90CurlLength> list, boolean updateSupport, Long importLogId);
}