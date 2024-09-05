package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqAssistSpec;

import java.util.List;

/**
 * <p>
 * 胎圈外协规格管理 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TqAssistSpecService extends IService<TqAssistSpec> {

    /**
     * 根据条件查询外协规格管理列表
     *
     * @return
     */
    List<TqAssistSpec> listAssistSpec(TqAssistSpec dto);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveAssistSpec(TqAssistSpec entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteAssistSpec(Long[] ids);

    /**
     * 根据code判断代号是否已经存在
     */
    String checkAssistSpecCodeUnique(TqAssistSpec dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqAssistSpec> list, boolean updateSupport, Long importLogId);
}
