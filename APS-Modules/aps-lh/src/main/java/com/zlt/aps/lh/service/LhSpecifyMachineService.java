package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;

import java.util.List;


/**
 * 硫化定点机台信息Service接口
 *
 * @author zlt
 * @date 2021-07-21
 */
public interface LhSpecifyMachineService {
    /**
     * 查询硫化定点机台信息
     *
     * @param id 硫化定点机台信息ID
     * @return 硫化定点机台信息
     */
    public LhSpecifyMachine selectLhSpecifyMachineById(Long id);

    /**
     * 查询硫化定点机台信息列表
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 硫化定点机台信息集合
     */
    public List<LhSpecifyMachine> selectLhSpecifyMachineList(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 新增硫化定点机台信息
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 结果
     */
    public int insertLhSpecifyMachine(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 修改硫化定点机台信息
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 结果
     */
    public int updateLhSpecifyMachine(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 批量删除硫化定点机台信息
     *
     * @param ids 需要删除的硫化定点机台信息ID
     * @return 结果
     */
    public int deleteLhSpecifyMachineByIds(Long[] ids);

    /**
     * 删除硫化定点机台信息信息
     *
     * @param id 硫化定点机台信息ID
     * @return 结果
     */
    public int deleteLhSpecifyMachineById(Long id);

    /**
     * 校验硫化定点机台信息唯一性
     */
    public String checkLhSpecifyMachineUnique(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 导入数据
     */
    AjaxResult importData(List<LhSpecifyMachine> list, boolean updateSupport, Long importLogId);
}
