package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxShareMoldInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型胎胚共用模具信息Service接口
 *
 * @author chen
 * @date 2022-03-22
 */
public interface CxShareMoldInfoService {
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
    @Transactional
    public int insertCxShareMoldInfo(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 修改成型胎胚共用模具信息
     *
     * @param cxShareMoldInfo 成型胎胚共用模具信息
     * @return 结果
     */
    @Transactional
    public int updateCxShareMoldInfo(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 批量删除成型胎胚共用模具信息
     *
     * @param ids 需要删除的成型胎胚共用模具信息ID
     * @return 结果
     */
    @Transactional
    public int deleteCxShareMoldInfoByIds(Long[] ids);

    /**
     * 删除成型胎胚共用模具信息信息
     *
     * @param id 成型胎胚共用模具信息ID
     * @return 结果
     */
    @Transactional
    public int deleteCxShareMoldInfoById(Long id);

    /**
     * 校验成型胎胚共用模具信息唯一性
     */
    public String checkCxShareMoldInfoUnique(CxShareMoldInfo cxShareMoldInfo);

    /**
     * 导入成型胎胚共用模具信息数据
     */
    @Transactional
    public AjaxResult importData(List<CxShareMoldInfo> list, boolean updateSupport, Long importLogId);

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
