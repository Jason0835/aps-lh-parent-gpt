package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqSpecifyMachine;
import com.zlt.bill.common.service.IDocService;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;

/**
 * 钢丝圈定点机台Service接口
 *
 * @author zlt
 * @date 2026-07-08
 */
@FeignClient(contextId = "iGsqSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqSpecifyMachineService extends IDocService<GsqSpecifyMachine> {

    /**
     * 校验"钢丝圈代码+生产线"组合唯一性
     *
     * @param entity 实体
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqSpecifyMachine entity);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqSpecifyMachine> list, boolean updateSupport, Long importLogId);
}
