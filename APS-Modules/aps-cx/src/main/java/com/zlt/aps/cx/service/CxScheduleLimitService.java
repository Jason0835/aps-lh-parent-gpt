package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxScheduleLimitDto;
import com.zlt.aps.cx.entity.CxScheduleLimit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 成型排产限制信息维护 服务类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-16
 */
public interface CxScheduleLimitService extends IService<CxScheduleLimit> {

    /**
     * 查询成型排产限制信息维护列表
     *
     * @param limit 成型排产限制信息维护
     * @return 成型排产限制信息维护集合
     */
    public List<CxScheduleLimitDto> selectLimitList(CxScheduleLimit limit);

    /**
     * 查询成型排产限制信息维护列表
     *
     * @param id 要查询的id
     * @return 成型排产限制信息维护集合
     */
    public CxScheduleLimit selectLimitById(Long id);

    /**
     * 保存成型排产限制信息维护
     *
     * @param limit 成型排产限制信息维护
     */
    @Transactional
    void saveLimit(CxScheduleLimit limit);

    /**
     * 批量删除成型排产限制信息维护
     *
     * @param ids 需要删除的成型排产限制信息维护ID
     */
    @Transactional
    public void deleteLimitByIds(Long[] ids);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxScheduleLimitDto> list, boolean updateSupport, Long importLogId);
}
