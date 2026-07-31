package com.zlt.aps.gsq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqParamsDto;
import com.zlt.aps.gsq.entity.GsqParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 钢丝圈参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface GsqParamsService extends IService<GsqParams> {
    /**
     * 查询钢丝圈参数信息
     *
     * @param id 钢丝圈参数信息ID
     * @return 钢丝圈参数信息
     */
    public GsqParams selectParamsById(Long id);

    /**
     * 查询钢丝圈参数信息列表
     *
     * @param params 钢丝圈参数信息     * @return 钢丝圈参数信息集合
     */
    public List<GsqParamsDto> selectParamsList(GsqParams params);

    /**
     * 修改钢丝圈参数信息
     *
     * @param params 钢丝圈参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(GsqParams params);

    /**
     * 校验钢丝圈参数代码唯一
     *
     * @param params 钢丝圈参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(GsqParams params);
}
