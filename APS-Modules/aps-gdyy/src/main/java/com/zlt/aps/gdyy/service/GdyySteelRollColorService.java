package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyySteelRollColorDto;
import com.zlt.aps.gdyy.entity.GdyySteelRollColor;

import java.util.List;

/**
 * <p>
 * 帘布大卷颜色提示信息表 服务类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
public interface GdyySteelRollColorService extends IService<GdyySteelRollColor> {
    /**
     * 根据条件查询大卷颜色提示信息列表
     *
     * @return
     */
    List<GdyySteelRollColorDto> listGdyySteelRollColor(GdyySteelRollColorDto dto);

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveGdyySteelRollColor(GdyySteelRollColor entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteGdyySteelRollColor(Long[] ids);

    /**
     * 根据大卷编号判断胶料组号是否已经存在
     */
    String checkGdyySteelRollColor(GdyySteelRollColorDto dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyySteelRollColorDto> list, boolean updateSupport, Long importLogId);
}
