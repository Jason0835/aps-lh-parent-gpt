package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import com.zlt.aps.cx.mapper.CxChangeLhMachineMapper;
import com.zlt.aps.cx.service.CxChangeLhMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成型排程硫化机台调整Service业务层处理
 *
 * @author chen
 * @date 2022-04-15
 */
@Service
public class CxChangeLhMachineServiceImpl implements CxChangeLhMachineService {
    @Autowired
    private CxChangeLhMachineMapper cxChangeLhMachineMapper;

    /**
     * 查询成型排程硫化机台调整
     *
     * @param id 成型排程硫化机台调整ID
     * @return 成型排程硫化机台调整
     */
    @Override
    public CxChangeLhMachine selectCxChangeLhMachineById(Long id) {
        return cxChangeLhMachineMapper.selectCxChangeLhMachineById(id);
    }

    /**
     * 查询成型排程硫化机台调整列表
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 成型排程硫化机台调整
     */
    @Override
    public List<CxChangeLhMachine> selectCxChangeLhMachineList(CxChangeLhMachine cxChangeLhMachine) {
        return cxChangeLhMachineMapper.selectCxChangeLhMachineList(cxChangeLhMachine);
    }

    /**
     * 新增成型排程硫化机台调整
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 结果
     */
    @Override
    public int insertCxChangeLhMachine(CxChangeLhMachine cxChangeLhMachine) {
        cxChangeLhMachine.setBaseVale(null);
        return cxChangeLhMachineMapper.insertCxChangeLhMachine(cxChangeLhMachine);
    }

    /**
     * 修改成型排程硫化机台调整
     *
     * @param cxChangeLhMachine 成型排程硫化机台调整
     * @return 结果
     */
    @Override
    public int updateCxChangeLhMachine(CxChangeLhMachine cxChangeLhMachine) {
        cxChangeLhMachine.setBaseVale(cxChangeLhMachine.getId());
        return cxChangeLhMachineMapper.updateCxChangeLhMachine(cxChangeLhMachine);
    }

    /**
     * 批量删除成型排程硫化机台调整
     *
     * @param ids 需要删除的成型排程硫化机台调整ID
     * @return 结果
     */
    @Override
    public int deleteCxChangeLhMachineByIds(Long[] ids) {
        return cxChangeLhMachineMapper.deleteCxChangeLhMachineByIds(ids);
    }

    /**
     * 删除成型排程硫化机台调整信息
     *
     * @param id 成型排程硫化机台调整ID
     * @return 结果
     */
    @Override
    public int deleteCxChangeLhMachineById(Long id) {
        return cxChangeLhMachineMapper.deleteCxChangeLhMachineById(id);
    }

    /**
     * 校验成型排程硫化机台调整唯一性
     */
    @Override
    public String checkCxChangeLhMachineUnique(CxChangeLhMachine cxChangeLhMachine) {
        if (cxChangeLhMachine == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxChangeLhMachine> list = cxChangeLhMachineMapper.selectCxChangeLhMachineList(cxChangeLhMachine);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int batchSaveCxChangeLhMachine(List<CxChangeLhMachine> list) {
        return cxChangeLhMachineMapper.batchSaveCxChangeLhMachine(list);
    }
}
