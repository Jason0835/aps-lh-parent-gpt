package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroup;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupForExcel;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 成型机组Service接口
 * 
 * @author zlt
 * @date 2021-12-16
 */
public interface CxMachineGroupService
{
    /**
     * 查询成型机组
     * 
     * @param id 成型机组ID
     * @return 成型机组
     */
    public CxMachineGroup selectCxMachineGroupById(Long id);

    /**
     * 查询成型机组列表
     * 
     * @param cxMachineGroup 成型机组
     * @return 成型机组集合
     */
    public List<CxMachineGroup> selectCxMachineGroupList(CxMachineGroup cxMachineGroup);

    public List<CxMachineGroupForExcel> selectCxMachineGroup4Excel(CxMachineGroup cxMachineGroup);

    /**
     * 新增成型机组
     * 
     * @param cxMachineGroup 成型机组
     * @return 结果
     */
    @Transactional
    public int insertCxMachineGroup(CxMachineGroup cxMachineGroup);

    /**
     * 修改成型机组
     * 
     * @param cxMachineGroup 成型机组
     * @return 结果
     */
    @Transactional
    public int updateCxMachineGroup(CxMachineGroup cxMachineGroup);

    /**
     * 批量删除成型机组
     * 
     * @param ids 需要删除的成型机组ID
     * @return 结果
     */
    @Transactional
    public int deleteCxMachineGroupByIds(Long[] ids);

    /**
     * 删除成型机组信息
     * 
     * @param id 成型机组ID
     * @return 结果
     */
    @Transactional
    public int deleteCxMachineGroupById(Long id);

    /**
     * 校验成型机组唯一性
     */
    public String checkCxMachineGroupUnique(CxMachineGroup cxMachineGroup);

    /**
     * 导入成型机组数据
     */
    @Transactional
    public AjaxResult importData(List<CxMachineGroupForExcel> list, boolean updateSupport, Long importLogId);
}
