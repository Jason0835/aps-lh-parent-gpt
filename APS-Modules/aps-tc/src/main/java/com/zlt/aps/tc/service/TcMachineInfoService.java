package com.zlt.aps.tc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;

import java.util.List;

/**
 * 胎侧机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface TcMachineInfoService {
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
     * @param tTcMachineInfo 胎侧机台信息
     * @return 胎侧机台信息集合
     */
    public List<TcMachineInfo> selectMachineInfoList(TcMachineInfo tTcMachineInfo);

    /**
     * 新增胎侧机台信息
     *
     * @param tTcMachineInfo 胎侧机台信息
     * @return 结果
     */
    public int insertMachineInfo(TcMachineInfo tTcMachineInfo);

    /**
     * 修改胎侧机台信息
     *
     * @param tTcMachineInfo 胎侧机台信息
     * @return 结果
     */
    public int updateMachineInfo(TcMachineInfo tTcMachineInfo);

    /**
     * 批量删除胎侧机台信息
     *
     * @param ids 需要删除的胎侧机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(TcMachineInfo tTcMachineInfo);

    /**
     * 根据胎侧、口型板查询机台信息
     *
     * @param tTcMachineInfo 胎侧机台信息
     * @return 胎侧机台信息集合
     */
    public List<TcMachineInfo> selectMachineInfoList2(TcMachineInfo tTcMachineInfo);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcMachineInfo> list, boolean updateSupport, Long importLogId);

}
