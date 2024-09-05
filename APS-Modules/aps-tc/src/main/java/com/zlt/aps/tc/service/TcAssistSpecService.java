package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcAssistSpec;

import java.util.List;

/**
 * <p>
 * 胎侧外协规格管理 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TcAssistSpecService extends IService<TcAssistSpec> {

    /**
     * 根据条件查询外协规格管理列表
     *
     * @return
     */
    List<TcAssistSpec> listAssistSpec(TcAssistSpec dto);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveAssistSpec(TcAssistSpec entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteAssistSpec(Long[] ids);

    /**
     * 根据code判断代号是否已经存在
     */
    String checkAssistSpecCodeUnique(TcAssistSpec dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TcAssistSpec> list, boolean updateSupport, Long importLogId);
}
