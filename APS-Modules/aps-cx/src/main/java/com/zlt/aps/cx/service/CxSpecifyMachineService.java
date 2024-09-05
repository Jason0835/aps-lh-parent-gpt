package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxSpecifyMachine;

import java.util.List;


/**
 * 定点机台Service接口
 *
 * @author zlt
 * @date 2021-07-21
 */
public interface CxSpecifyMachineService {
    /**
     * 查询定点机台
     *
     * @param id 定点机台ID
     * @return 定点机台
     */
    public CxSpecifyMachine selectCxSpecifyMachine1ById(Long id);

    /**
     * 查询定点机台列表
     *
     * @param cxSpecifyMachine 定点机台
     * @return 定点机台集合
     */
    public List<CxSpecifyMachine> selectCxSpecifyMachine1List(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 新增定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    public int insertCxSpecifyMachine1(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 修改定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    public int updateCxSpecifyMachine1(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 批量删除定点机台
     *
     * @param ids 需要删除的定点机台ID
     * @return 结果
     */
    public int deleteCxSpecifyMachine1ByIds(Long[] ids);

    /**
     * 删除定点机台信息
     *
     * @param id 定点机台ID
     * @return 结果
     */
    public int deleteCxSpecifyMachine1ById(Long id);

    /**
     * 校验定点机台唯一性
     */
    public String checkCxSpecifyMachine1Unique(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxSpecifyMachine> list, boolean updateSupport, Long importLogId);
}
