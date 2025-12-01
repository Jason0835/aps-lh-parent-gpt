package com.zlt.mix.setting.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import javax.annotation.Resource;

import com.ruoyi.common.security.service.TokenService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.mapper.LhflSpanSettingMapper;
import com.zlt.mix.setting.api.domain.entity.LhflSpanSetting;
import com.zlt.mix.setting.service.LhflSpanSettingService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫磺辅料跨区设置Service业务层处理
 *
 * @author chen
 * @date 2022-08-12
 */
@Service
public class LhflSpanSettingServiceImpl extends ServiceImpl<LhflSpanSettingMapper, LhflSpanSetting> implements LhflSpanSettingService {
    @Resource
    private LhflSpanSettingMapper lhflSpanSettingMapper;

    @Resource
    private TokenService tokenService;

    /**
     * 查询硫磺辅料跨区设置列表
     *
     * @param lhflSpanSetting 硫磺辅料跨区设置
     * @return 硫磺辅料跨区设置
     */
    @Override
    public List<LhflSpanSetting> selectLhflSpanSettingList(LhflSpanSetting lhflSpanSetting) {
        return lhflSpanSettingMapper.selectLhflSpanSettingList(lhflSpanSetting);
    }

    /**
     * 保存硫磺辅料跨区设置信息（id为空则新增，id不为空则修改）
     *
     * @param lhflSpanSetting
     */
    @Override
    public void saveLhflSpanSetting(LhflSpanSetting lhflSpanSetting) {
        if (ZltConstant.NOT_UNIQUE.equals(checkLhflSpanSettingUnique(lhflSpanSetting))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.lhflSpanSetting.database.unique" ));
        }
        lhflSpanSetting.setBaseValue(lhflSpanSetting.getId());
        this.saveOrUpdate(lhflSpanSetting);
    }

    /**
     * 批量删除硫磺辅料跨区设置
     *
     * @param ids 需要删除的硫磺辅料跨区设置ID
     * @return 结果
     */
    @Override
    public int deleteLhflSpanSettingByIds(Long[] ids)
    {
        return lhflSpanSettingMapper.deleteLhflSpanSettingByIds(ids);
    }


    /**
     * 校验硫磺辅料跨区设置唯一性
     */
    @Override
    public String checkLhflSpanSettingUnique(LhflSpanSetting lhflSpanSetting) {
        if (lhflSpanSetting == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<LhflSpanSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LhflSpanSetting::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(LhflSpanSetting::getEntrustMixArea, lhflSpanSetting.getEntrustMixArea());
        queryWrapper.eq(LhflSpanSetting::getMaterialName, lhflSpanSetting.getMaterialName());
        if (lhflSpanSetting.getId() != null) {
            queryWrapper.ne(LhflSpanSetting::getId, lhflSpanSetting.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<LhflSpanSetting> list = lhflSpanSettingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入硫磺辅料跨区设置数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<LhflSpanSetting> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhflSpanSetting> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.lhflSpanSettingMapper.listLhflSpanSettingNotUnique(list, importLogId, I18nUtil.getMessage("setting.lhflSpanSetting.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getEntrustMixArea() + a.getMaterialName(), Collectors.counting()));
            Set<String> permissionSet = tokenService.getLoginUser().getPermissions().get(ZltConstant.MIX);

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                LhflSpanSetting lhflSpanSetting = list.get(i);
                //exce中重复记录校验
                String entrustMixArea = lhflSpanSetting.getEntrustMixArea();
                Long hasValue = groupMap.get(entrustMixArea + lhflSpanSetting.getMaterialName());
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    lhflSpanSetting.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.lhflSpanSetting.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                // 委托密炼区与被委托密炼区不能相同校验
                if (ObjectUtils.compare(lhflSpanSetting.getEntrustedMixArea(), entrustMixArea) == 0) {
                    lhflSpanSetting.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("ui.message.spanSetting.sameMixArea");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                // 用户是否有对应密炼区的权限校验 (委托密炼区) 此处使用dictValue校验的 不为admin才需要校验
                if (!permissionSet.contains(ZltConstant.ADMIN_PERMISSION) && StringUtils.isNotEmpty(entrustMixArea) && !permissionSet.contains(entrustMixArea)) {
                    lhflSpanSetting.setId(-999L);
                    String message = String.format(I18nUtil.getMessage("setting.spanSetting.mixAreaPermission"), entrustMixArea);
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    lhflSpanSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, lhflSpanSetting); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && lhflSpanSetting.getId() == null) {
                    lhflSpanSetting.setBaseValue(null);
                    importList.add(lhflSpanSetting);
                } else {
                    lhflSpanSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                lhflSpanSettingMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                lhflSpanSettingMapper.batchInsertLhflSpanSettingInfo(importList);  //批量插入
            }
        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
