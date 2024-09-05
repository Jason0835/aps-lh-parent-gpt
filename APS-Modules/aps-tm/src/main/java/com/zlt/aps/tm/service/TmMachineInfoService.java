package com.zlt.aps.tm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;

import java.util.List;

/**
 * 胎面机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TmMachineInfoService {
    /**
     * 查询胎面机台信息
     *
     * @param id 胎面机台信息ID
     * @return 胎面机台信息
     */
    public TmMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询胎面机台信息列表
     *
     * @param machineInfo 胎面机台信息
     * @return 胎面机台信息集合
     */
    public List<TmMachineInfo> selectMachineInfoList(TmMachineInfo machineInfo);

    /**
     * 新增胎面机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 结果
     */
    public int insertMachineInfo(TmMachineInfo machineInfo);

    /**
     * 修改胎面机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 结果
     */
    public int updateMachineInfo(TmMachineInfo machineInfo);

    /**
     * 批量删除胎面机台信息
     *
     * @param ids 需要删除的胎面机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(TmMachineInfo machineInfo);

    /**
     * 根据胎面和口型板获取对应机台信息
     *
     * @param machineInfo 胎面机台信息
     * @return 胎面机台信息集合
     */
    public List<TmMachineInfo> selectMachineInfoList2(TmMachineInfo machineInfo);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TmMachineInfo> list, boolean updateSupport, Long importLogId);

}
