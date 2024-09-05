package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;

import java.util.List;

/**
 * 15°裁断机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface Cd15MachineInfoService {
    /**
     * 查询15°裁断机台信息
     *
     * @param id 15°裁断机台信息ID
     * @return 15°裁断机台信息
     */
    public Cd15MachineInfo selectMachineInfoById(Long id);

    /**
     * 查询15°裁断机台信息列表
     *
     * @param machineInfo 15°裁断机台信息
     * @return 15°裁断机台信息集合
     */
    public List<Cd15MachineInfo> selectMachineInfoList(Cd15MachineInfo machineInfo);

    /**
     * 新增15°裁断机台信息
     *
     * @param machineInfo 15°裁断机台信息
     * @return 结果
     */
    public int insertMachineInfo(Cd15MachineInfo machineInfo);

    /**
     * 修改15°裁断机台信息
     *
     * @param machineInfo 15°裁断机台信息
     * @return 结果
     */
    public int updateMachineInfo(Cd15MachineInfo machineInfo);

    /**
     * 批量删除15°裁断机台信息
     *
     * @param ids 需要删除的15°裁断机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(Cd15MachineInfo machineInfo);

    public List<Cd15MachineInfo> selectMachineInfoList2(Cd15MachineInfo machineInfo);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<Cd15MachineInfo> list, boolean updateSupport, Long importLogId);
}
