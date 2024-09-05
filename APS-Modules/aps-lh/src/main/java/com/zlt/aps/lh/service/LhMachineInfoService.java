package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;

import java.util.List;

/**
 * 硫化机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface LhMachineInfoService {
    /**
     * 查询硫化机台信息
     *
     * @param id 硫化机台信息ID
     * @return 硫化机台信息
     */
    public LhMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询硫化机台信息列表
     *
     * @param machineInfo 硫化机台信息
     * @return 硫化机台信息集合
     */
    public List<LhMachineInfo> selectMachineInfoList(LhMachineInfo machineInfo);

    /**
     * 新增硫化机台信息
     *
     * @param machineInfo 硫化机台信息
     * @return 结果
     */
    public int insertMachineInfo(LhMachineInfo machineInfo);

    /**
     * 修改硫化机台信息
     *
     * @param machineInfo 硫化机台信息
     * @return 结果
     */
    public int updateMachineInfo(LhMachineInfo machineInfo);

    /**
     * 批量删除硫化机台信息
     *
     * @param ids 需要删除的硫化机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(LhMachineInfo machineInfo);

    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    List<LhMachineInfo> listMachineInfo(LhMachineInfo machineInfo);

    /**
     * 导入数据
     */
    AjaxResult importData(List<LhMachineInfo> list, boolean updateSupport, Long importLogId);
}
