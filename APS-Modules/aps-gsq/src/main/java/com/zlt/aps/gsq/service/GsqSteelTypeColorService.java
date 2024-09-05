package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqSteelTypeColorDto;
import com.zlt.aps.gsq.entity.GsqSteelTypeColor;

import java.util.List;

/**
 * <p>
 * 钢丝圈颜色提示信息表 服务类
 * </p>
 */
public interface GsqSteelTypeColorService extends IService<GsqSteelTypeColor> {
    /**
     * 根据条件查询大卷颜色提示信息列表
     *
     * @return
     */
    List<GsqSteelTypeColorDto> listGsqSteelTypeColor(GsqSteelTypeColorDto dto);

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGsqSteelTypeColor(GsqSteelTypeColor entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGsqSteelTypeColor(Long[] ids);

    /**
     * 根据大卷编号判断胶料组号是否已经存在
     */
    String checkGsqSteelTypeColor(GsqSteelTypeColorDto dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqSteelTypeColorDto> list, boolean updateSupport, Long importLogId);
}
