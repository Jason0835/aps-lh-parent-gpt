package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import com.zlt.bill.common.service.IDocService;

public interface ITqMachineChuckService extends IDocService<TqMachineChuck> {

    /**
     * 校验机台编码+寸口编码组合唯一性
     *
     * @param machineChuck 机台寸口对象（机台编码+寸口编码，编辑时携带ID排除自身）
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    String checkUnique(TqMachineChuck machineChuck);

    /**
     * 保存机台寸口对应（带唯一性校验与已删除记录复活处理）
     *
     * @param machineChuck 机台寸口对象（id为空新增，id不为空修改）
     * @return 操作结果
     */
    AjaxResult saveWithCheck(TqMachineChuck machineChuck);

    /**
     * 删除全部机台寸口对应（逻辑删除）
     */
    void deleteAllMachineChuck();
}
