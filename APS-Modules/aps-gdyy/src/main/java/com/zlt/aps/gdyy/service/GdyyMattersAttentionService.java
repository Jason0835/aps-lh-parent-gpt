package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyyMattersAttentionDto;
import com.zlt.aps.gdyy.entity.GdyyMattersAttention;

import java.util.List;

/**
 * <p>
 * 帘布大卷注意事项信息表 服务类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
public interface GdyyMattersAttentionService extends IService<GdyyMattersAttention> {
    /**
     * 根据条件查询大卷注意事项信息列表
     *
     * @return
     */
    List<GdyyMattersAttentionDto> listGdyyMattersAttention(GdyyMattersAttentionDto dto);

    /**
     * 保存大卷注意事项信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGdyyMattersAttention(GdyyMattersAttention entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGdyyMattersAttention(Long[] ids);
    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyyMattersAttention> list, boolean updateSupport, Long importLogId);
}
