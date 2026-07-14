package com.zlt.aps.nc.service.impl;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.nc.api.domain.entity.NcParams;
import com.zlt.aps.nc.mapper.NcParamsMapper;
import com.zlt.aps.nc.service.NcParamsService;
import com.zlt.common.utils.PubUtil;

/**
 * 内衬参数信息Service业务层处理
 *
 * @author zlt
 * @date 2026-06-25
 */
@Service
public class NcParamsServiceImpl extends ServiceImpl<NcParamsMapper, NcParams> implements NcParamsService {
    @Autowired
    private NcParamsMapper paramsMapper;

    /**
     * 查询内衬参数信息
     *
     * @param id 内衬参数信息ID
     * @return 内衬参数信息
     */
    @Override
    public NcParams selectParamsById(Long id) {
        LambdaQueryWrapper<NcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NcParams::getId, id);
        return paramsMapper.selectOne(wrapper);
    }

    /**
     * 查询内衬参数信息列表
     *
     * @param params 内衬参数信息
     * @return 内衬参数信息
     */
    @Override
    public List<NcParams> selectParamsList(NcParams params) {
        QueryWrapper<NcParams> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(params.getFactoryCode()), "FACTORY_CODE", params.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(params.getProductTypeCode()), "PRODUCT_TYPE_CODE",
                params.getProductTypeCode());
        wrapper.eq(StringUtils.isNotBlank(params.getParamCode()), "PARAM_CODE", params.getParamCode());
        wrapper.eq("IS_DELETE", ApsConstant.DEL_FLAG_NORMAL);
        return paramsMapper.selectList(wrapper);
    }

    /**
     * 修改内衬参数信息
     *
     * @param params 内衬参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(NcParams params) {
        if (!ObjectUtils.allNotNull(params.getId(), params.getParamCode(), params.getParamName(),
                params.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }
        String unique = checkParamsCodeUnique(params);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        paramsMapper.updateById(params);
        return AjaxResult.success();
    }

    /**
     * 校验参数代码唯一性
     *
     * @param params 内衬参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(NcParams params) {
        QueryWrapper<NcParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(params.getFieldValueByFieldName("id")), "ID",
                params.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", params.getFactoryCode());
        queryWrapper.eq("PARAM_CODE", params.getParamCode());
        if (paramsMapper.exists(queryWrapper)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 根据条件查询内衬参数
     *
     * @param factoryCode     工厂编码
     * @param productTypeCode 产品品类
     * @param paramCode       参数编码
     * @return 内衬参数信息
     */
    @Override
    public NcParams getParamsByCondition(String factoryCode, String productTypeCode, String paramCode) {
        LambdaQueryWrapper<NcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NcParams::getFactoryCode, factoryCode);
        wrapper.eq(NcParams::getProductTypeCode, productTypeCode);
        wrapper.eq(NcParams::getParamCode, paramCode);
        wrapper.eq(NcParams::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        List<NcParams> list = paramsMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }
}
