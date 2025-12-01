package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.service.TokenService;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.GlueSpanSetting;
import com.zlt.mix.setting.mapper.GlueSpanSettingMapper;
import com.zlt.mix.setting.service.GlueSpanSettingService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 终炼母炼胶料跨区设置Service业务层处理
 *
 * @author chen
 * @date 2022-08-12
 */
@Service
public class GlueSpanSettingServiceImpl extends ServiceImpl<GlueSpanSettingMapper, GlueSpanSetting> implements GlueSpanSettingService {
    @Resource
    private GlueSpanSettingMapper glueSpanSettingMapper;
    @Resource
    private TokenService tokenService;

    /**
     * 查询终炼母炼胶料跨区设置列表
     *
     * @param glueSpanSetting 终炼母炼胶料跨区设置
     * @return 终炼母炼胶料跨区设置
     */
    @Override
    public List<GlueSpanSetting> selectGlueSpanSettingList(GlueSpanSetting glueSpanSetting) {
        return glueSpanSettingMapper.selectGlueSpanSettingList(glueSpanSetting);
    }

    /**
     * 保存终炼母炼胶料跨区设置信息（id为空则新增，id不为空则修改）
     */
    @Override
    public void saveGlueSpanSetting(GlueSpanSetting glueSpanSetting) {
        if (ZltConstant.NOT_UNIQUE.equals(checkGlueSpanSettingUnique(glueSpanSetting))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.glueSpanSetting.database.unique" ));
        }
        glueSpanSetting.setBaseValue(glueSpanSetting.getId());
        this.saveOrUpdate(glueSpanSetting);
    }

    /**
     * 批量删除终炼母炼胶料跨区设置
     *
     * @param ids 需要删除的终炼母炼胶料跨区设置ID
     * @return 结果
     */
    @Override
    public int deleteGlueSpanSettingByIds(Long[] ids)
    {
        return glueSpanSettingMapper.deleteGlueSpanSettingByIds(ids);
    }


    /**
     * 校验终炼母炼胶料跨区设置唯一性
     */
    @Override
    public String checkGlueSpanSettingUnique(GlueSpanSetting glueSpanSetting) {
        if (glueSpanSetting == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueSpanSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueSpanSetting::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(GlueSpanSetting::getEntrustMixArea, glueSpanSetting.getEntrustMixArea());
        queryWrapper.eq(GlueSpanSetting::getGlue, glueSpanSetting.getGlue());
        if (glueSpanSetting.getId() != null) {
            queryWrapper.ne(GlueSpanSetting::getId, glueSpanSetting.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<GlueSpanSetting> list = glueSpanSettingMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入终炼母炼胶料跨区设置数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<GlueSpanSetting> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueSpanSetting> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if(!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.glueSpanSettingMapper.listGlueSpanSettingNotUnique(list, importLogId, I18nUtil.getMessage("setting.glueSpanSetting.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getEntrustMixArea() + a.getGlue(), Collectors.counting()));
            Set<String> permissionSet = tokenService.getLoginUser().getPermissions().values().stream().findFirst().orElse(new HashSet<>());

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueSpanSetting glueSpanSetting = list.get(i);
                //exce中重复记录校验
                String entrustMixArea = glueSpanSetting.getEntrustMixArea();
                Long hasValue = groupMap.get(entrustMixArea + glueSpanSetting.getGlue());
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    glueSpanSetting.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.glueSpanSetting.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                // 委托密炼区与被委托密炼区不能相同校验
                if (ObjectUtils.compare(glueSpanSetting.getEntrustedMixArea(), entrustMixArea) == 0) {
                    glueSpanSetting.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("ui.message.spanSetting.sameMixArea");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                // 用户是否有对应密炼区的权限校验 (委托密炼区) 此处使用dictValue校验的 不为admin权限
                if (!permissionSet.contains(ZltConstant.ADMIN_PERMISSION) && StringUtils.isNotEmpty(entrustMixArea) && !permissionSet.contains(entrustMixArea)) {
                    glueSpanSetting.setId(-999L);
                    String message = String.format(I18nUtil.getMessage("setting.spanSetting.mixAreaPermission"), entrustMixArea);
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if(codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    glueSpanSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueSpanSetting); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && glueSpanSetting.getId() == null) {
                    glueSpanSetting.setBaseValue(null);
                    importList.add(glueSpanSetting);
                } else {
                    glueSpanSetting.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueSpanSettingMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if(!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueSpanSettingMapper.batchInsertGlueSpanSettingInfo(importList);  //批量插入
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
