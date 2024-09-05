package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;

import java.util.List;

/**
 * 胎侧机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TcMachineInfoMapper {
    /**
     * 查询胎侧机台信息
     *
     * @param id 胎侧机台信息ID
     * @return 胎侧机台信息
     */
    public TcMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询胎侧机台信息列表
     *
     * @param machineInfo 胎侧机台信息
     * @return 胎侧机台信息集合
     */
    public List<TcMachineInfo> selectMachineInfoList(TcMachineInfo machineInfo);

    /**
     * 新增胎侧机台信息
     *
     * @param machineInfo 胎侧机台信息
     * @return 结果
     */
    public int insertMachineInfo(TcMachineInfo machineInfo);

    /**
     * 修改胎侧机台信息
     *
     * @param machineInfo 胎侧机台信息
     * @return 结果
     */
    public int updateMachineInfo(TcMachineInfo machineInfo);

    /**
     * 批量删除胎侧机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验胎侧机台唯一性
     */
    public List<TcMachineInfo> checkMachineCodeUnique(TcMachineInfo machineInfo);

    public List<TcMachineInfo> checkMachineNameUnique(TcMachineInfo machineInfo);

    /**
     * 根据胎侧、口型板查询机台信息
     *
     * @param machineInfo 胎侧机台信息
     * @return 胎侧机台信息集合
     */
    public List<TcMachineInfo> selectMachineInfoList2(TcMachineInfo machineInfo);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcMachineInfo> list);

}
