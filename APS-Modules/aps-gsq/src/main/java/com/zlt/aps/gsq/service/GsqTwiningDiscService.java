package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import com.zlt.aps.gsq.entity.GsqTwiningDisc;

import java.util.List;

/**
 * <p>
 * 钢丝圈缠绕盘表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface GsqTwiningDiscService extends IService<GsqTwiningDisc> {

    /**
     * 根据条件查询缠绕盘列表
     *
     * @return
     */
    List<GsqTwiningDiscDto> listTwiningDisc(GsqTwiningDiscDto dto);

    /**
     * 保存缠绕盘信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveTwiningDisc(GsqTwiningDisc entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteTwiningDisc(Long[] ids);

    /**
     * 根据code判断缠绕盘代号是否已经存在
     */
    String checkSerialNumberUnique(GsqTwiningDiscDto dto);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqTwiningDiscDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
