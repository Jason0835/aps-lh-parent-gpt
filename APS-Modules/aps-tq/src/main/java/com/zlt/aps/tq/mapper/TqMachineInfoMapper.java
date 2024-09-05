package com.zlt.aps.tq.mapper;


import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;

import java.util.List;

/**
 * 胎圈机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TqMachineInfoMapper {
    /**
     * 查询胎圈机台信息
     *
     * @param id 胎圈机台信息ID
     * @return 胎圈机台信息
     */
    public TqMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询胎圈机台信息列表
     *
     * @param machineInfo 胎圈机台信息
     * @return 胎圈机台信息集合
     */
    public List<TqMachineInfo> selectMachineInfoList(TqMachineInfo machineInfo);

    /**
     * 新增胎圈机台信息
     *
     * @param machineInfo 胎圈机台信息
     * @return 结果
     */
    public int insertMachineInfo(TqMachineInfo machineInfo);

    /**
     * 修改胎圈机台信息
     *
     * @param machineInfo 胎圈机台信息
     * @return 结果
     */
    public int updateMachineInfo(TqMachineInfo machineInfo);

    /**
     * 批量删除胎圈机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验胎圈机台唯一性
     */
    public List<TqMachineInfo> checkMachineCodeUnique(TqMachineInfo machineInfo);
    public List<TqMachineInfo> checkMachineNameUnique(TqMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    List<TqMachineInfo> listMachineInfo(TqMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqMachineInfo> list);
}
