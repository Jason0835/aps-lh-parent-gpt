package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;

import java.util.List;

/**
 * 成型排程硫化机台调整Mapper接口
 *
 * @author chen
 * @date 2022-04-15
 */
public interface CxChangeLhMachineMapper {
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
    public int insertCxChangeLhMachine(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 修改成型排程硫化机台调整
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 结果
     */
    public int updateCxChangeLhMachine(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 删除成型排程硫化机台调整
     *
     * @param id 成型排程硫化机台调整ID
     * @return 结果
     */
    public int deleteCxChangeLhMachineById(Long id);

    /**
     * 批量删除成型排程硫化机台调整
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxChangeLhMachineByIds(Long[] ids);

    /**
     * 批量保存成型排程硫化机台调整信息
     * @param list 保存集合
     * @return 保存成功记录数
     */
    public int batchSaveCxChangeLhMachine(List<CxChangeLhMachine> list);
}
