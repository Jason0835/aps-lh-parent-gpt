package com.zlt.aps.dj.service.impl;

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
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.mapper.DjParamsMapper;
import com.zlt.aps.dj.service.DjParamsService;
import com.zlt.common.utils.PubUtil;

/**
 * 垫胶参数信息Service业务层处理
 *
 * @author zlt
 * @date 2026-06-11
 */
@Service
public class DjParamsServiceImpl extends ServiceImpl<DjParamsMapper, DjParams> implements DjParamsService {
    @Autowired
    private DjParamsMapper paramsMapper;

    /**
     * 查询垫胶参数信息
     *
     * @param id 垫胶参数信息ID
     * @return 垫胶参数信息
     */
    @Override
    public DjParams selectParamsById(Long id) {
        LambdaQueryWrapper<DjParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DjParams::getId, id);
        return paramsMapper.selectOne(wrapper);
    }

    /**
     * 查询垫胶参数信息列表
     *
     * @param params 垫胶参数信息
     * @return 垫胶参数信息
     */
    @Override
    public List<DjParams> selectParamsList(DjParams params) {
        QueryWrapper<DjParams> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(params.getFactoryCode()), "FACTORY_CODE", params.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(params.getProductTypeCode()), "PRODUCT_TYPE_CODE",
                params.getProductTypeCode());
        wrapper.eq(StringUtils.isNotBlank(params.getParamCode()), "PARAM_CODE", params.getParamCode());
        wrapper.eq("IS_DELETE", ApsConstant.DEL_FLAG_NORMAL);
        return paramsMapper.selectList(wrapper);
    }

    /**
     * 修改垫胶参数信息
     *
     * @param params 垫胶参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(DjParams params) {
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
     * @param params 垫胶参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(DjParams params) {
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();

        QueryWrapper<DjParams> queryWrapper = new QueryWrapper<>();
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
     * 根据条件查询垫胶参数
     *
     * @param factoryCode     工厂编码
     * @param productTypeCode 产品品类
     * @param paramCode       参数编码
     * @return 垫胶参数信息
     */
    @Override
    public DjParams getParamsByCondition(String factoryCode, String productTypeCode, String paramCode) {
        LambdaQueryWrapper<DjParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DjParams::getFactoryCode, factoryCode);
        wrapper.eq(DjParams::getProductTypeCode, productTypeCode);
        wrapper.eq(DjParams::getParamCode, paramCode);
        wrapper.eq(DjParams::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        List<DjParams> list = paramsMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }
}