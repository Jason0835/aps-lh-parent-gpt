package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyOriginalLineSpec;

import java.util.List;

/**
 * <p>
 * 纤维压延原线规格管理 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface XwyyOriginalLineSpecService extends IService<XwyyOriginalLineSpec> {

    /**
     * 根据条件查询原线规格管理列表
     *
     * @return
     */
    List<XwyyOriginalLineSpec> listOriginalLineSpec(XwyyOriginalLineSpec dto);

    /**
     * 保存原线规格管理信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveOriginalLineSpec(XwyyOriginalLineSpec entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteOriginalLineSpec(Long[] ids);

    /**
     * 根据code判断代号是否已经存在
     */
    String checkOriginalLineSpecCodeUnique(XwyyOriginalLineSpec dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<XwyyOriginalLineSpec> list, boolean updateSupport, Long importLogId);
}
