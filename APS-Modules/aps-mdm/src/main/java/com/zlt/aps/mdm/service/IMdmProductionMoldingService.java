package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmProductionMolding;
import com.zlt.aps.mdm.api.domain.vo.MdmProductionMoldingPageVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分厂成型正在生产的品种Service接口
 *
 * @author hsc
 * @date 2021-08-30
 */
public interface IMdmProductionMoldingService {

    /**
     * 查询分厂成型正在生产的品种
     *
     * @param id 分厂成型正在生产的品种主键
     * @return 分厂成型正在生产的品种
     */
    public MdmProductionMolding selectFactoryProductionProductById(Long id);

    /**
     * 查询分厂成型正在生产的品种列表
     *
     * @param mdmProductionMolding 分厂成型正在生产的品种
     * @return 分厂成型正在生产的品种集合
     */
    public List<MdmProductionMolding> selectFactoryProductionProductList(MdmProductionMolding mdmProductionMolding);

    /**
     * 新增分厂成型正在生产的品种
     *
     * @param mdmProductionMolding 分厂成型正在生产的品种
     * @return 结果
     */
    @Transactional
    public int insertFactoryProductionProduct(MdmProductionMolding mdmProductionMolding);

    /**
     * 修改分厂成型正在生产的品种
     *
     * @param mdmProductionMolding 分厂成型正在生产的品种
     * @return 结果
     */
    @Transactional
    public int updateFactoryProductionProduct(MdmProductionMolding mdmProductionMolding);

    /**
     * 批量删除分厂成型正在生产的品种
     *
     * @param ids 需要删除的分厂成型正在生产的品种主键集合
     * @return 结果
     */

    @Transactional
    public int deleteFactoryProductionProductByIds(Long[] ids);

    /**
     * 删除分厂成型正在生产的品种信息
     *
     * @param id 分厂成型正在生产的品种主键
     * @return 结果
     */
//    @Transactional
//    public int deleteFactoryProductionProductById(Long id);

    /**
     * 校验分厂成型正在生产的品种唯一性
     */
    public String checkFactoryProductionProductUnique(MdmProductionMolding mdmProductionMolding);

    /**
     * 导入分厂成型正在生产的品种数据
     */
    @Transactional
    public AjaxResult importData(List<MdmProductionMolding> list, boolean updateSupport, Long importLogId);

    /**
     * 获取成型法
     */
    MdmProductionMoldingPageVo getMachineMethod(MdmProductionMoldingPageVo vo);
}
