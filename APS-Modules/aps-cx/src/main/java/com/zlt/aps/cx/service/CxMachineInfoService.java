package com.zlt.aps.cx.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;

import java.util.List;

/**
 * 成型机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface CxMachineInfoService {
    /**
     * 查询成型机台信息
     *
     * @param id 成型机台信息ID
     * @return 成型机台信息
     */
    public CxMachineInfo selectCxMachineInfoById(Long id);

    /**
     * 查询成型机台信息列表
     *
     * @param cxMachineInfo 成型机台信息
     * @return 成型机台信息集合
     */
    public List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo);

    public List<CxMachineInfo> listOrderByName(CxMachineInfo cxMachineInfo);

    public List<CxMachineInfo> selectCxMachineInfoList2(CxMachineInfo cxMachineInfo);


    /**
     * 获取其他半部件机台列表
     * @param cxMachineInfo
     * @return
     */
    List<CxMachineInfo> getOrtherMachineInfo(CxMachineInfo cxMachineInfo);

    /**
     * 新增成型机台信息
     *
     * @param cxMachineInfo 成型机台信息
     * @return 结果
     */
    public int insertCxMachineInfo(CxMachineInfo cxMachineInfo);

    /**
     * 修改成型机台信息
     *
     * @param cxMachineInfo 成型机台信息
     * @return 结果
     */
    public int updateCxMachineInfo(CxMachineInfo cxMachineInfo);

    /**
     * 批量删除成型机台信息
     *
     * @param ids 需要删除的成型机台信息ID
     * @return 结果
     */
    public int deleteCxMachineInfoByIds(Long[] ids);

    /**
     * 删除成型机台信息信息
     *
     * @param id 成型机台信息ID
     * @return 结果
     */
    public int deleteCxMachineInfoById(Long id);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(CxMachineInfo cxMachineInfo);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxMachineInfo> list, boolean updateSupport, Long importLogId);

}
