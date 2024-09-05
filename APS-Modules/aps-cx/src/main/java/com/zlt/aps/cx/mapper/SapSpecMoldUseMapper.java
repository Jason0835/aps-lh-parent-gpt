package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;

/**
 * 规格使用模数Mapper接口
 * 
 * @author zlt
 * @date 2022-01-18
 */
public interface SapSpecMoldUseMapper 
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

    public List<SapSpecMoldUse> checkSapSpecMoldUseUnique(SapSpecMoldUse sapSpecMoldUse);


    /**
     * 新增规格使用模数
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 结果
     */
    public int insertSapSpecMoldUse(SapSpecMoldUse sapSpecMoldUse);

    /**
     * 修改规格使用模数
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 结果
     */
    public int updateSapSpecMoldUse(SapSpecMoldUse sapSpecMoldUse);

    /**
     * 删除规格使用模数
     * 
     * @param id 规格使用模数ID
     * @return 结果
     */
    public int deleteSapSpecMoldUseById(Long id);

    /**
     * 批量删除规格使用模数
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteSapSpecMoldUseByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<SapSpecMoldUse> list);
}
