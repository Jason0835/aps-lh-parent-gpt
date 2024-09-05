package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;

import java.util.List;

/**
 * 钢丝圈机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface GsqMachineInfoService {
    /**
     * 查询钢丝圈机台信息
     *
     * @param id 钢丝圈机台信息ID
     * @return 钢丝圈机台信息
     */
    public GsqMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询钢丝圈机台信息列表
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 钢丝圈机台信息集合
     */
    public List<GsqMachineInfo> selectMachineInfoList(GsqMachineInfo machineInfo);

    /**
     * 新增钢丝圈机台信息
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 结果
     */
    public int insertMachineInfo(GsqMachineInfo machineInfo);

    /**
     * 修改钢丝圈机台信息
     *
     * @param machineInfo 钢丝圈机台信息
     * @return 结果
     */
    public int updateMachineInfo(GsqMachineInfo machineInfo);

    /**
     * 批量删除钢丝圈机台信息
     *
     * @param ids 需要删除的钢丝圈机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(GsqMachineInfo machineInfo);

    public List<GsqMachineInfo> listMachineInfo(GsqMachineInfo machineInfo);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqMachineInfo> list, boolean updateSupport, Long importLogId);
}
