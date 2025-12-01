package com.zlt.mix.setting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;
import com.zlt.mix.setting.mapper.SettingScheduleParamsMapper;
import com.zlt.mix.setting.service.SettingScheduleParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 密炼参数信息Service业务层处理
 *
 * @author Liam
 * @date 2022-03-14
 */
@Service
public class SettingScheduleParamsServiceImpl extends ServiceImpl<SettingScheduleParamsMapper, SettingScheduleParams> implements SettingScheduleParamsService {

    /**
     * 查询密炼参数信息列表
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 密炼参数信息列表
     */
    @Override
    public List<SettingScheduleParams> selectParamsList(SettingScheduleParams settingScheduleParams) {
        return baseMapper.selectParamsList(settingScheduleParams);
    }

    /**
     * 获取密炼参数信息详细信息
     *
     * @param id 密炼参数信息ID
     * @return 密炼参数信息
     */
    @Override
    public SettingScheduleParams selectParamsById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 修改密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    @Override
    public AjaxResult updateParams(SettingScheduleParams settingScheduleParams) {
        //基本参数校验
        if (!ObjectUtils.allNotNull(settingScheduleParams.getId(), settingScheduleParams.getParamCode(), settingScheduleParams.getParamName(), settingScheduleParams.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }

        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = settingScheduleParams.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(settingScheduleParams.getParamValue()).matches();
        }

        if (regularResult) {
            settingScheduleParams.setBaseValue(settingScheduleParams.getId());
            baseMapper.updateById(settingScheduleParams);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(settingScheduleParams.getErrorTips());
    }

    /**
     * 校验密炼参数信息信息唯一性（同一ID的数据也算作重复）
     */
    public String checkScheduleParamsUnique(SettingScheduleParams settingScheduleParams) {
        if (settingScheduleParams == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<SettingScheduleParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("MIX_AREA", settingScheduleParams.getMixArea());
        queryWrapper.eq("PARAM_CODE", settingScheduleParams.getParamCode());


        List<SettingScheduleParams> list = baseMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }


    /**
     * 复制密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    @Override
    public AjaxResult copyScheduleParams(SettingScheduleParams settingScheduleParams) {
        //基本参数校验
        if (!ObjectUtils.allNotNull(settingScheduleParams.getId(), settingScheduleParams.getParamCode(), settingScheduleParams.getParamName(), settingScheduleParams.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter" ));
        }

        //校验是否存在
        if (ZltConstant.NOT_UNIQUE.equals(checkScheduleParamsUnique(settingScheduleParams))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.database.unique" ));
        }

        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = settingScheduleParams.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(settingScheduleParams.getParamValue()).matches();
        }

        if (regularResult) {
            //初始化数据，插入到表中
            settingScheduleParams.setBaseValue(null);
            settingScheduleParams.setUpdateBy(null);
            settingScheduleParams.setUpdateTime(null);
            baseMapper.insert(settingScheduleParams);
        }

        return regularResult ? AjaxResult.success() : AjaxResult.error(settingScheduleParams.getErrorTips());
    }
    

    /**
     * 加载指定密炼区参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    @Override
    public AjaxResult selectParamsListMixArea(SettingScheduleParams settingScheduleParams) {
    	List<SettingScheduleParams> paramsList = baseMapper.selectParamsListMixArea(settingScheduleParams);
    	SettingScheduleParams params = CollectionUtil.firstElement(paramsList);
    	return AjaxResult.success(params);
    }
}
