package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.dto.NcCurlRollDto;
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;

import java.util.List;

/**
 * <p>
 * 内衬卷曲信息表 服务类
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface NcCurlRollService extends IService<NcCurlRoll> {

    /**
     * 根据条件查询内衬卷曲信息列表
     *
     * @return
     */
    List<NcCurlRoll> listCurlRoll(NcCurlRoll dto);

    /**
     * 保存内衬卷曲信息信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    void saveCurlRoll(NcCurlRoll entity);

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    void deleteCurlRoll(Long[] ids);

    /**
     * 根据code判断内衬卷曲代号是否已经存在
     */
    String checkCurlRollCodeUnique(NcCurlRoll dto);

    /**
     * 导入数据
     */
    AjaxResult importData(List<NcCurlRollDto> list, boolean updateSupport, Long importLogId);

    /**
     * 根据code查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    AjaxResult selectCurlLengthByCode(NcCurlRoll curlRoll);
}
