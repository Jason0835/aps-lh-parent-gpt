package com.zlt.aps.tq.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;

import java.util.List;

/**
 * 胎圈机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TqMachineInfoService {
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
     * @param ids 需要删除的胎圈机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(TqMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    List<TqMachineInfo> listMachineInfo(TqMachineInfo machineInfo);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqMachineInfo> list, boolean updateSupport, Long importLogId);
}
