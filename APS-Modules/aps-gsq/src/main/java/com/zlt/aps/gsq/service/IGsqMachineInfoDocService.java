package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢丝圈机台信息Service接口（新规范，继承IDocService）
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface IGsqMachineInfoDocService extends IDocService<GsqMachineInfo> {

    /**
     * 校验机台编号唯一性
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 校验结果
     */
    String checkUnique(GsqMachineInfo machineInfo);

    /**
     * 校验机台编号唯一性
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 校验结果
     */
    String checkMachineCodeUnique(GsqMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 机台列表
     */
    List<GsqMachineInfo> listMachineInfo(GsqMachineInfo machineInfo);

    /**
     * 查询钢丝圈机台信息列表
     *
     * @param machineInfo 查询条件
     * @return 机台列表
     */
    List<GsqMachineInfo> selectMachineInfoList(GsqMachineInfo machineInfo);

}
