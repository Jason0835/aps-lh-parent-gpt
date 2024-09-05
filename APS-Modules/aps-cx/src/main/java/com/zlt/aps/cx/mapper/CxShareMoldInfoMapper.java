package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxShareMoldInfo;

import java.util.List;

/**
 * 成型胎胚共用模具信息Mapper接口
 *
 * @author chen
 * @date 2022-03-22
 */
public interface CxShareMoldInfoMapper {
    /**
     * 查询成型胎胚共用模具信息
     *
     * @param id 成型胎胚共用模具信息ID
     * @return 成型胎胚共用模具信息
     */
    public CxShareMoldInfo selectCxShareMoldInfoById(Long id);

    /**
     * 查询成型胎胚共用模具信息列表
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 成型胎胚共用模具信息集合
     */
    public List<CxShareMoldInfo> selectCxShareMoldInfoList(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 新增成型胎胚共用模具信息
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 结果
     */
    public int insertCxShareMoldInfo(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 修改成型胎胚共用模具信息
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 结果
     */
    public int updateCxShareMoldInfo(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 删除成型胎胚共用模具信息
     *
     * @param id 成型胎胚共用模具信息ID
     * @return 结果
     */
    public int deleteCxShareMoldInfoById(Long id);

    /**
     * 批量删除成型胎胚共用模具信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxShareMoldInfoByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxShareMoldInfo> list);

    /**
     * 校验唯一性
     * @param cxShareMoldInfo 所属组别、胎胚代码
     * @return 查询到的记录条数
     */
    public int checkUnique(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 根据胎胚代码查询所属组别对应的所有胎胚代码信息（排除传入胎胚）
     * @param embryoCode 胎胚代码
     * @return 查询到的共用模具信息
     */
    public List<CxShareMoldInfo> selectShareMoldInfoListByEmbryoCode(String embryoCode);

    /**
     * 根据sap品号查询所属组别对应的所有sap品号信息（排除传入sap品号）
     * @param sapCode sap品号
     * @return 查询到的共用模具信息
     */
    public List<CxShareMoldInfo> selectShareMoldInfoListBySapCode(String sapCode);

    /**
     * 查询共用模具信息，并根据sap品号及胎胚关联硫化外胎施工表，获取规格规格信息
     * @return 查询到的集合数据（sap品号、胎胚代码、规格型号）
     */
    public List<CxShareMoldInfo> selectShareMoldInfoList();
}
