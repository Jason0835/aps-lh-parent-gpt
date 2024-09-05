package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyBigRollColorDto;
import com.zlt.aps.xwyy.entity.XwyyBigRollColor;

import java.util.List;

/**
 * <p>
 * 帘布大卷颜色提示信息表 服务类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
public interface XwyyBigRollColorService extends IService<XwyyBigRollColor> {
    /**
     * 根据条件查询大卷颜色提示信息列表
     *
     * @return
     */
    List<XwyyBigRollColorDto> listXwyyBigRollColor(XwyyBigRollColorDto dto);

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveXwyyBigRollColor(XwyyBigRollColor entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids
     */
    void deleteXwyyBigRollColor(Long[] ids);

    /**
     * 根据大卷编号判断胶料组号是否已经存在
     */
    String checkXwyyBigRollColor(XwyyBigRollColorDto dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyyBigRollColorDto> list, boolean updateSupport, Long importLogId);
}
