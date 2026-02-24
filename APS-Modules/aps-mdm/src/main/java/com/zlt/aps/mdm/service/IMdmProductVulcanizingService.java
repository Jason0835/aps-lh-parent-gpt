package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmProductVulcanizing;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 基础数据-硫化机正在生产品种Service接口
 *
 * @author hsc
 * @date 2021-09-01
 */
public interface IMdmProductVulcanizingService {

    /**
     * 查询基础数据-硫化机正在生产品种
     *
     * @param id 基础数据-硫化机正在生产品种主键
     * @return 基础数据-硫化机正在生产品种
     */
    public MdmProductVulcanizing selectDocProductVulcanizationById(Long id);

    /**
     * 查询基础数据-硫化机正在生产品种列表
     *
     * @param docProductVulcanization 基础数据-硫化机正在生产品种
     * @return 基础数据-硫化机正在生产品种集合
     */
    public List<MdmProductVulcanizing> selectDocProductVulcanizationList(MdmProductVulcanizing docProductVulcanization);

    /**
     * 新增基础数据-硫化机正在生产品种
     *
     * @param docProductVulcanization 基础数据-硫化机正在生产品种
     * @return 结果
     */
    @Transactional
    public int insertDocProductVulcanization(MdmProductVulcanizing docProductVulcanization);

    /**
     * 修改基础数据-硫化机正在生产品种
     *
     * @param docProductVulcanization 基础数据-硫化机正在生产品种
     * @return 结果
     */
    @Transactional
    public int updateDocProductVulcanization(MdmProductVulcanizing docProductVulcanization);

    /**
     * 批量删除基础数据-硫化机正在生产品种
     *
     * @param ids 需要删除的基础数据-硫化机正在生产品种主键集合
     * @return 结果
     */

    @Transactional
    public int deleteDocProductVulcanizationByIds(Long[] ids);

    /**
     * 校验基础数据-硫化机正在生产品种唯一性
     */
    public String checkDocProductVulcanizationUnique(MdmProductVulcanizing docProductVulcanization);

    /**
     * 导入基础数据-硫化机正在生产品种数据
     */
    @Transactional
    public AjaxResult importData(List<MdmProductVulcanizing> list, boolean updateSupport, Long importLogId);
}
