package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.InProductionSpec;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型机台当前生产规格Service接口
 *
 * @author chen
 * @date 2022-02-25
 */
public interface InProductionSpecService {
    /**
     * 查询成型机台当前生产规格
     *
     * @param id 成型机台当前生产规格ID
     * @return 成型机台当前生产规格
     */
    public InProductionSpec selectInProductionSpecById(Long id);

    /**
     * 查询成型机台当前生产规格列表
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 成型机台当前生产规格集合
     */
    public List<InProductionSpec> selectInProductionSpecList(InProductionSpec inProductionSpec);

    /**
     * 新增成型机台当前生产规格
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 结果
     */
    @Transactional
    public int insertInProductionSpec(InProductionSpec inProductionSpec);

    /**
     * 修改成型机台当前生产规格
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 结果
     */
    @Transactional
    public int updateInProductionSpec(InProductionSpec inProductionSpec);

    /**
     * 批量删除成型机台当前生产规格
     *
     * @param ids 需要删除的成型机台当前生产规格ID
     * @return 结果
     */
    @Transactional
    public int deleteInProductionSpecByIds(Long[] ids);

    /**
     * 删除成型机台当前生产规格信息
     *
     * @param id 成型机台当前生产规格ID
     * @return 结果
     */
    @Transactional
    public int deleteInProductionSpecById(Long id);

    /**
     * 校验成型机台当前生产规格唯一性
     */
    public String checkInProductionSpecUnique(InProductionSpec inProductionSpec);

    /**
     * 导入成型机台当前生产规格数据
     */
    @Transactional
    public AjaxResult importData(List<InProductionSpec> list, boolean updateSupport, Long importLogId);
}
