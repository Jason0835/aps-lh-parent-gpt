package com.zlt.aps.gdyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import com.zlt.aps.gdyy.entity.GdyyParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 钢带压延参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface GdyyParamsService extends IService<GdyyParams> {
    /**
     * 查询钢带压延参数信息
     *
     * @param id 钢带压延参数信息ID
     * @return 钢带压延参数信息
     */
    public GdyyParams selectParamsById(Long id);

    /**
     * 查询钢带压延参数信息列表
     *
     * @param params 钢带压延参数信息     * @return 钢带压延参数信息集合
     */
    public List<GdyyParamsDto> selectParamsList(GdyyParams params);

    /**
     * 修改钢带压延参数信息
     *
     * @param params 钢带压延参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(GdyyParams params);

    /**
     * 校验钢带压延参数代码唯一
     *
     * @param params 钢带压延参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(GdyyParams params);
}
