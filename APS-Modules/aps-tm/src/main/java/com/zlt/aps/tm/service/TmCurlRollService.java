package com.zlt.aps.tm.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.dto.TmCurlRollDto;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;

/**
 * <p>
 * 胎面卷曲信息表 服务类
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface TmCurlRollService extends IService<TmCurlRoll> {

    /**
     * 根据条件查询胎面卷曲信息列表
     *
     * @return
     */
    List<TmCurlRoll> listCurlRoll(TmCurlRoll dto);

    /**
     * 保存胎面卷曲信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveCurlRoll(TmCurlRoll entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteCurlRoll(Long[] ids);

    /**
     * 根据code判断胎面卷曲代号是否已经存在
     */
    String checkCurlRollCodeUnique(TmCurlRoll dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<TmCurlRollDto> list, boolean updateSupport, Long importLogId);
}
