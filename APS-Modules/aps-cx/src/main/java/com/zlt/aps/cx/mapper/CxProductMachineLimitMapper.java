package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxProductMachineLimit;

/**
 * 成型投产班次同机台硫化班次限定设置Mapper接口
 * 
 * @author zlt
 * @date 2022-01-08
 */
public interface CxProductMachineLimitMapper 
{
    /**
     * 查询成型投产班次同机台硫化班次限定设置
     * 
     * @param id 成型投产班次同机台硫化班次限定设置ID
     * @return 成型投产班次同机台硫化班次限定设置
     */
    public CxProductMachineLimit selectCxProductMachineLimitById(Long id);

    /**
     * 查询成型投产班次同机台硫化班次限定设置列表
     * 
     * @param cxProductMachineLimit 成型投产班次同机台硫化班次限定设置
     * @return 成型投产班次同机台硫化班次限定设置集合
     */
    public List<CxProductMachineLimit> selectCxProductMachineLimitList(CxProductMachineLimit cxProductMachineLimit);

    public List<CxProductMachineLimit> checkCxProductMachineLimitUnique(CxProductMachineLimit cxProductMachineLimit);

    /**
     * 新增成型投产班次同机台硫化班次限定设置
     * 
     * @param cxProductMachineLimit 成型投产班次同机台硫化班次限定设置
     * @return 结果
     */
    public int insertCxProductMachineLimit(CxProductMachineLimit cxProductMachineLimit);

    /**
     * 修改成型投产班次同机台硫化班次限定设置
     * 
     * @param cxProductMachineLimit 成型投产班次同机台硫化班次限定设置
     * @return 结果
     */
    public int updateCxProductMachineLimit(CxProductMachineLimit cxProductMachineLimit);

    /**
     * 删除成型投产班次同机台硫化班次限定设置
     * 
     * @param id 成型投产班次同机台硫化班次限定设置ID
     * @return 结果
     */
    public int deleteCxProductMachineLimitById(Long id);

    /**
     * 批量删除成型投产班次同机台硫化班次限定设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxProductMachineLimitByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxProductMachineLimit> list);
}
