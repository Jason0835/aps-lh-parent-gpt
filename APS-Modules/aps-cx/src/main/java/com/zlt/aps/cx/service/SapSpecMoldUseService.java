package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 规格使用模数Service接口
 * 
 * @author zlt
 * @date 2022-01-18
 */
public interface SapSpecMoldUseService
{
    /**
     * 查询规格使用模数
     * 
     * @param id 规格使用模数ID
     * @return 规格使用模数
     */
    public SapSpecMoldUse selectSapSpecMoldUseById(Long id);

    /**
     * 查询规格使用模数列表
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 规格使用模数集合
     */
    public List<SapSpecMoldUse> selectSapSpecMoldUseList(SapSpecMoldUse sapSpecMoldUse);

    public List<SapSpecMoldUse> getSpecDesc(SapSpecMoldUse sapSpecMoldUse);

    /**
     * 新增规格使用模数
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 结果
     */
    @Transactional
    public int insertSapSpecMoldUse(SapSpecMoldUse sapSpecMoldUse);

    /**
     * 修改规格使用模数
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 结果
     */
    @Transactional
    public int updateSapSpecMoldUse(SapSpecMoldUse sapSpecMoldUse);

    /**
     * 批量删除规格使用模数
     * 
     * @param ids 需要删除的规格使用模数ID
     * @return 结果
     */
    @Transactional
    public int deleteSapSpecMoldUseByIds(Long[] ids);

    /**
     * 删除规格使用模数信息
     * 
     * @param id 规格使用模数ID
     * @return 结果
     */
    @Transactional
    public int deleteSapSpecMoldUseById(Long id);

    /**
     * 校验规格使用模数唯一性
     */
    public String checkSapSpecMoldUseUnique(SapSpecMoldUse sapSpecMoldUse);

    /**
     * 导入规格使用模数数据
     */
    @Transactional
    public AjaxResult importData(List<SapSpecMoldUse> list, boolean updateSupport, Long importLogId);
}
