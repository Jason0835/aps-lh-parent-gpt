package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 斜裁定点机台业务接口。
 */
public interface ICd15SpecifyMachineService extends IDocService<Cd15SpecifyMachine> {

    /**
     * 校验同一工厂下定点机台配置是否唯一。
     *
     * @param specifyMachine 定点机台配置
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd15SpecifyMachine specifyMachine);

    /**
     * 导入斜裁定点机台数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15SpecifyMachine> list, boolean updateSupport, Long importLogId);
}