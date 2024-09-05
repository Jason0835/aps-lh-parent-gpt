package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.CxSpecColorDto;
import com.zlt.aps.cx.entity.CxSpecColor;
import com.zlt.aps.cx.mapper.CxSpecColorMapper;
import com.zlt.aps.cx.service.CxSpecColorService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 规格字体颜色设置Service业务层处理
 *
 * @author chen
 * @date 2021-08-21
 */
@Service
public class CxSpecColorServiceImpl extends ServiceImpl<CxSpecColorMapper, CxSpecColor> implements CxSpecColorService {
    @Autowired
    private CxSpecColorMapper cxSpecColorMapper;

    /**
     * 查询规格字体颜色设置
     *
     * @param id 规格字体颜色设置ID
     * @return 规格字体颜色设置
     */
    @Override
    public CxSpecColorDto selectCxSpecColorById(Long id) {
        return cxSpecColorMapper.selectCxSpecColorById(id);
    }

    /**
     * 查询规格字体颜色设置列表
     *
     * @param cxSpecColor 规格字体颜色设置
     * @return 规格字体颜色设置
     */
    @Override
    public List<CxSpecColorDto> selectCxSpecColorList(CxSpecColor cxSpecColor) {
        return cxSpecColorMapper.selectCxSpecColorList(cxSpecColor);
    }

    /**
     * 新增规格字体颜色设置
     *
     * @param cxSpecColor 规格字体颜色设置
     * @return 结果
     */
    @Override
    public int insertCxSpecColor(CxSpecColor cxSpecColor) {
        cxSpecColor.setBaseVale(null);
        return cxSpecColorMapper.insertCxSpecColor(cxSpecColor);
    }

    /**
     * 修改规格字体颜色设置
     *
     * @param cxSpecColor 规格字体颜色设置
     * @return 结果
     */
    @Override
    public int updateCxSpecColor(CxSpecColor cxSpecColor) {
        cxSpecColor.setBaseVale(cxSpecColor.getId());
        return cxSpecColorMapper.updateCxSpecColor(cxSpecColor);
    }

    /**
     * 批量删除规格字体颜色设置
     *
     * @param ids 需要删除的规格字体颜色设置ID
     * @return 结果
     */
    @Override
    public int deleteCxSpecColorByIds(Long[] ids) {
        return cxSpecColorMapper.deleteCxSpecColorByIds(ids);
    }

    /**
     * 删除规格字体颜色设置信息
     *
     * @param id 规格字体颜色设置ID
     * @return 结果
     */
    @Override
    public int deleteCxSpecColorById(Long id) {
        return cxSpecColorMapper.deleteCxSpecColorById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkCxSpecColorUnique(CxSpecColor cxSpecColor) {
        if (cxSpecColor == null) {
            return UserConstants.NOT_UNIQUE;
        }
        if (cxSpecColorMapper.checkCxSpecColorUnique(cxSpecColor) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxSpecColorDto> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxSpecColorDto> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(CxSpecColorDto::getSpecDesc, Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxSpecColorDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getSpecDesc());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.specColor.specDesc");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, dto);
            if (StringUtils.isBlank(dto.getColorCode())){
                dto.setColorCode("#000000");
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                dto.setBaseVale(null);
                importList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                cxSpecColorMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxSpecColorDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    CxSpecColor newItem = new CxSpecColor();
                    newItem.setSpecDesc(excelItem.getSpecDesc());
                    String unique = this.checkCxSpecColorUnique(newItem);
                    BeanUtils.copyProperties(excelItem, newItem);
                    if (StringUtils.isEmpty(newItem.getColorCode())) {
                        newItem.setColorCode("#000000");
                    }
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.saveOrUpdate(newItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.alert.specDescExist"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
