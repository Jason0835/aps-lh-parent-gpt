package com.zlt.aps.gsq.mapper;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;

import java.util.List;

/**
 * 钢丝圈机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface GsqMachineInfoMapper {
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
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验钢丝圈机台唯一性
     */
    public List<GsqMachineInfo> checkMachineCodeUnique(GsqMachineInfo machineInfo);
    public List<GsqMachineInfo> checkMachineNameUnique(GsqMachineInfo machineInfo);


    List<GsqMachineInfo> listMachineInfo(GsqMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqMachineInfo> list);
}
