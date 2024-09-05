package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqParamsDto;
import com.zlt.aps.tq.entity.TqParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胎圈参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TqParamsService extends IService<TqParams> {
    /**
     * 查询胎圈参数信息
     *
     * @param id 胎圈参数信息ID
     * @return 胎圈参数信息
     */
    public TqParams selectParamsById(Long id);

    /**
     * 查询胎圈参数信息列表
     *
     * @param params 胎圈参数信息     * @return 胎圈参数信息集合
     */
    public List<TqParamsDto> selectParamsList(TqParams params);

    /**
     * 修改胎圈参数信息
     *
     * @param params 胎圈参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(TqParams params);

    /**
     * 校验胎圈参数代码唯一
     *
     * @param params 胎圈参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(TqParams params);
}
