package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 斜裁机台基础信息业务接口。
 */
public interface ICd15MachineInfoService extends IDocService<Cd15MachineInfo> {

    /**
     * 校验同工厂机台编号唯一。
     *
     * @param machineInfo 斜裁机台信息
     * @return UserConstants.UNIQUE 或 UserConstants.NOT_UNIQUE
     */
    String checkUnique(Cd15MachineInfo machineInfo);

    /**
     * 校验机台裁断模式及对应能力配置。
     *
     * @param machineInfo 斜裁机台信息
     * @return 校验失败结果，校验通过返回null
     */
    AjaxResult validateForSave(Cd15MachineInfo machineInfo);

    /**
     * 导入斜裁机台数据。
     *
     * @param list 导入数据
     * @param updateSupport 已存在数据是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15MachineInfo> list, boolean updateSupport, Long importLogId);
}
