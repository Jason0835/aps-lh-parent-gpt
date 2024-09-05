package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachine;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;

import java.util.List;
import java.util.Map;

/**
 * 定点机台Service接口
 *
 * @author zlt
 * @date 2021-06-11
 */
public interface CxMatchingSpecifyMachineService {
    /**
     * 查询定点机台
     *
     * @param id 定点机台ID
     * @return 定点机台
     */
    public CxMatchingSpecifyMachine selectTSpecifyMachineById(Long id);

    /**
     * 查询定点机台列表
     *
     * @param cxSpecifyMachine 定点机台
     * @return 定点机台集合
     */
    public List<CxMatchingSpecifyMachine> selectTSpecifyMachineList(CxMatchingSpecifyMachine cxSpecifyMachine);

    /**
     * 新增定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    public int insertTSpecifyMachine(CxMatchingSpecifyMachine cxSpecifyMachine);

    /**
     * 修改定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    public int updateTSpecifyMachine(CxMatchingSpecifyMachine cxSpecifyMachine);

    /**
     * 批量删除定点机台
     *
     * @param ids 需要删除的定点机台ID
     * @return 结果
     */
    public int deleteTSpecifyMachineByIds(Long[] ids);

    /**
     * 删除定点机台信息
     *
     * @param id 定点机台ID
     * @return 结果
     */
    public int deleteTSpecifyMachineById(Long id);

    /**
     * 获取详情列表
     *
     * @param cxMatchingSpecifyMachineList
     * @return
     */
    public List<CxMatchingSpecifyMachineList> detailList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    /**
     * 根据ID获取
     *
     * @param id
     * @return
     */
    public CxMatchingSpecifyMachineList selectCxSpecifyMachineListById(Long id);

    /**
     * 详情添加
     *
     * @param en
     * @return
     */
    public int detailAdd(CxMatchingSpecifyMachineList en);

    /**
     * 详情编辑
     *
     * @param en
     * @return
     */
    public int detailEdit(CxMatchingSpecifyMachineList en);

    /**
     * 详情删除
     *
     * @param ids
     * @return
     */
    public int deleteDetailByIds(Long[] ids);

    /**
     * 详情列表
     *
     * @param cxMatchingSpecifyMachineList
     * @return
     */
    public List<CxMatchingSpecifyMachineList> viewList(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    /**
     * 校验定点机台唯一性
     */
    public List<CxMatchingSpecifyMachine> checkCxSpecifyMachineUnic(CxMatchingSpecifyMachine cxSpecifyMachine);

    /**
     * 校验定点机台详情唯一性
     */
    public List<CxMatchingSpecifyMachineList> checkCxSpecifyMachineDetailUnic(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);


    /**
     * 导入数据
     */
    AjaxResult importData(List<CxMatchingSpecifyMachine> list, boolean updateSupport, Long importLogId);

    /**
     * 导入数据
     */
    AjaxResult detailImportData(List<CxMatchingSpecifyMachineList> list, boolean updateSupport, Long importLogId);
}
