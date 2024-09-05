package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcAssistSpec;

import java.util.List;

/**
 * <p>
 * 内衬外协规格管理 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface NcAssistSpecService extends IService<NcAssistSpec> {

    /**
     * 根据条件查询外协规格管理列表
     *
     * @return
     */
    List<NcAssistSpec> listAssistSpec(NcAssistSpec dto);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveAssistSpec(NcAssistSpec entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteAssistSpec(Long[] ids);

    /**
     * 根据code判断代号是否已经存在
     */
    String checkAssistSpecCodeUnique(NcAssistSpec dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcAssistSpec> list, boolean updateSupport, Long importLogId);
}
