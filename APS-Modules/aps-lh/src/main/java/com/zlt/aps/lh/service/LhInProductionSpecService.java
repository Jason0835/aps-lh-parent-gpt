package com.zlt.aps.lh.service;

import java.util.List;

import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 硫化机台当前生产规格Service接口
 *
 * @author chen
 * @date 2022-03-23
 */
public interface LhInProductionSpecService {
    /**
     * 查询硫化机台当前生产规格
     *
     * @param id 硫化机台当前生产规格ID
     * @return 硫化机台当前生产规格
     */
    public LhInProductionSpec selectLhInProductionSpecById(Long id);

    /**
     * 查询硫化机台当前生产规格列表
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 硫化机台当前生产规格集合
     */
    public List<LhInProductionSpec> selectLhInProductionSpecList(LhInProductionSpec lhInProductionSpec);

    /**
     * 新增硫化机台当前生产规格
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 结果
     */
    @Transactional
    public int insertLhInProductionSpec(LhInProductionSpec lhInProductionSpec);

    /**
     * 修改硫化机台当前生产规格
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 结果
     */
    @Transactional
    public int updateLhInProductionSpec(LhInProductionSpec lhInProductionSpec);

    /**
     * 批量删除硫化机台当前生产规格
     *
     * @param ids 需要删除的硫化机台当前生产规格ID
     * @return 结果
     */
    @Transactional
    public int deleteLhInProductionSpecByIds(Long[] ids);

    /**
     * 删除硫化机台当前生产规格信息
     *
     * @param id 硫化机台当前生产规格ID
     * @return 结果
     */
    @Transactional
    public int deleteLhInProductionSpecById(Long id);

    /**
     * 校验硫化机台当前生产规格唯一性
     */
    public String checkLhInProductionSpecUnique(LhInProductionSpec lhInProductionSpec);

    /**
     * 导入硫化机台当前生产规格数据
     */
    @Transactional
    public AjaxResult importData(List<LhInProductionSpec> list, boolean updateSupport, Long importLogId);
}
