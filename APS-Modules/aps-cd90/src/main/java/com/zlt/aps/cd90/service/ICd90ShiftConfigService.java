package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 班次配置业务接口。
 */
public interface ICd90ShiftConfigService extends IDocService<Cd90ShiftConfig> {

    /**
     * 校验同工厂班次编码唯一。
     *
     * @param shiftConfig 班次配置信息
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd90ShiftConfig shiftConfig);

    /**
     * 导入班次配置数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd90ShiftConfig> list, boolean updateSupport, Long importLogId);
}