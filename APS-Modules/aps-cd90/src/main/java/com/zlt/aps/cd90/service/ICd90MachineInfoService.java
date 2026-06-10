package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 直裁机台基础信息业务接口。
 */
public interface ICd90MachineInfoService extends IDocService<Cd90MachineInfo> {

    /**
     * 校验同工厂机台编号唯一。
     *
     * @param machineInfo 直裁机台信息
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd90MachineInfo machineInfo);



    /**
     * 导入直裁机台数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd90MachineInfo> list, boolean updateSupport, Long importLogId);
}
