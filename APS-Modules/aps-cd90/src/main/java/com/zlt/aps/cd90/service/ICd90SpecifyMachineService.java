package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 直裁定点机台业务接口。
 */
public interface ICd90SpecifyMachineService extends IDocService<Cd90SpecifyMachine> {

    /**
     * 校验同一工厂下定点机台配置是否唯一。
     *
     * @param specifyMachine 定点机台配置
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd90SpecifyMachine specifyMachine);

    /**
     * 导入直裁定点机台数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd90SpecifyMachine> list, boolean updateSupport, Long importLogId);
}
