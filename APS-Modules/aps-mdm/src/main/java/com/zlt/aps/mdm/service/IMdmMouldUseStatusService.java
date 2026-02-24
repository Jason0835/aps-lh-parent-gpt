package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldUseStatus;
import com.zlt.aps.mdm.api.domain.vo.MdmMouldUseStatusVo;
import com.zlt.aps.mdm.api.domain.vo.PeriodInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;


/**
 * 模具可用状态Service接口
 *
 * @author leo
 * @date 2021-08-27
 */
public interface IMdmMouldUseStatusService {
    /**
     * 查询模具可用状态
     *
     * @param id 模具可用状态主键
     * @return 模具可用状态
     */
    public MdmMouldUseStatus selectMouldUseStatusById(Long id);

    /**
     * 查询模具可用状态列表
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 模具可用状态集合
     */
    public List<MdmMouldUseStatus> selectMouldUseStatusList(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 查询模具可用状态列表(带物料号)
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 模具可用状态集合
     */
    public List<MdmMouldUseStatus> selectMouldUseStatusListForProductCode(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 新增模具可用状态
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 结果
     */
    public int insertMouldUseStatus(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 修改模具可用状态
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 结果
     */
    public int updateMouldUseStatus(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 批量删除模具可用状态
     *
     * @param ids 需要删除的模具可用状态主键集合
     * @return 结果
     */
    public int deleteMouldUseStatusByIds(Long[] ids);

    /**
     * 删除模具可用状态信息
     *
     * @param id 模具可用状态主键
     * @return 结果
     */
    public int deleteMouldUseStatusById(Long id);

    /**
     * 校验模具可用状态唯一性
     */
    public String checkMouldUseStatusUnique(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 复制记录
     */
    @Transactional
    public AjaxResult copy(PeriodInfo vo);

    /**
     * 合并记录
     */
    @Transactional
    public AjaxResult merge(PeriodInfo vo);

    /**
     * excel导入
     */
    @Transactional
    public AjaxResult importData(List<MdmMouldUseStatus> list, boolean updateSupport, Long importLogId);

    /**
     * 查询统计
     */
    MdmMouldUseStatusVo listTotal(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 根据分厂，年，月查询模具可用状态
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    List<MdmMouldUseStatus> queryByFactoryCodeYearMonth(String factoryCode, int year, int month, Set<String> mouldCodes);
}
