package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.entity.ConstructionInfo;

import java.util.List;

/**
 * <p>
 * 施工信息表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface ConstructionInfoService extends IService<ConstructionInfo> {

    /**
     * 根据条件查询施工信息列表
     *
     * @return
     */
    List<ConstructionInfoDto> listConstructionInfo(ConstructionInfoDto dto);

    /**
     * 保存施工信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveConstructionInfo(ConstructionInfo entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteConstructionInfo(Long[] ids);

    /**
     * 验证胚胎代码唯一性
     */
    String checkEmbryoCodeUnique(ConstructionInfoDto dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<ConstructionInfoDto> list, boolean updateSupport, Long importLogId);
}
