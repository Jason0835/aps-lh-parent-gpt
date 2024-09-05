package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.dto.Cd90BigRollDto;
import com.zlt.aps.cd90.entity.Cd90BigRoll;

import java.util.List;

/**
 * <p>
 * 90度裁断帘布大卷信息表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd90BigRollService extends IService<Cd90BigRoll> {

    /**
     * 根据条件查询帘布大卷信息列表
     *
     * @return
     */
    List<Cd90BigRollDto> listBigRoll(Cd90BigRollDto dto);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveBigRoll(Cd90BigRoll entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteBigRoll(Long[] ids);

    /**
     * 根据code判断帘布大卷代号是否已经存在
     */
    String checkBigRollCodeUnique(Cd90BigRollDto dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90BigRollDto> list, boolean updateSupport, Long importLogId);
}
