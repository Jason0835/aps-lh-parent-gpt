package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;

import java.util.List;

/**
 * 15°裁断机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface Cd15MachineInfoMapper {
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
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验15°裁断机台唯一性
     */
    public List<Cd15MachineInfo> checkMachineCodeUnique(Cd15MachineInfo machineInfo);
    public List<Cd15MachineInfo> checkMachineNameUnique(Cd15MachineInfo machineInfo);

    public List<Cd15MachineInfo> selectMachineInfoList2(Cd15MachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15MachineInfo> list);
}
