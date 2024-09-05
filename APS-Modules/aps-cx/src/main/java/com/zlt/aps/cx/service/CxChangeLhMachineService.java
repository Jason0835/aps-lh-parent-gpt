package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型排程硫化机台调整Service接口
 *
 * @author chen
 * @date 2022-04-15
 */
public interface CxChangeLhMachineService {
    /**
     * 查询成型排程硫化机台调整
     *
     * @param id 成型排程硫化机台调整ID
     * @return 成型排程硫化机台调整
     */
    public CxChangeLhMachine selectCxChangeLhMachineById(Long id);

    /**
     * 查询成型排程硫化机台调整列表
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 成型排程硫化机台调整集合
     */
    public List<CxChangeLhMachine> selectCxChangeLhMachineList(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 新增成型排程硫化机台调整
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 结果
     */
    @Transactional
    public int insertCxChangeLhMachine(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 修改成型排程硫化机台调整
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 结果
     */
    @Transactional
    public int updateCxChangeLhMachine(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 批量删除成型排程硫化机台调整
     *
     * @param ids 需要删除的成型排程硫化机台调整ID
     * @return 结果
     */
    @Transactional
    public int deleteCxChangeLhMachineByIds(Long[] ids);

    /**
     * 删除成型排程硫化机台调整信息
     *
     * @param id 成型排程硫化机台调整ID
     * @return 结果
     */
    @Transactional
    public int deleteCxChangeLhMachineById(Long id);

    /**
     * 校验成型排程硫化机台调整唯一性
     */
    public String checkCxChangeLhMachineUnique(CxChangeLhMachine cxChangeLhMachine);

    public int batchSaveCxChangeLhMachine(List<CxChangeLhMachine> list);
}
