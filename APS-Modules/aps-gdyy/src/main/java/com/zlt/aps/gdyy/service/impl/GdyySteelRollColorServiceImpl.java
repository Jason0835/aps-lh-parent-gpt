package com.zlt.aps.gdyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyySteelRollColorDto;
import com.zlt.aps.gdyy.entity.GdyySteelRollColor;
import com.zlt.aps.gdyy.mapper.GdyySteelRollColorMapper;
import com.zlt.aps.gdyy.service.GdyySteelRollColorService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 帘布大卷颜色提示信息表 服务实现类
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Service
public class GdyySteelRollColorServiceImpl extends ServiceImpl<GdyySteelRollColorMapper, GdyySteelRollColor> implements GdyySteelRollColorService {

    @Resource
    private GdyySteelRollColorMapper gdyySteelRollColorMapper;

    /**
     * 根据条件大卷颜色提示列表
     *
     * @return
     */
    public List<GdyySteelRollColorDto> listGdyySteelRollColor(GdyySteelRollColorDto dto) {
        return gdyySteelRollColorMapper.listGwyySteelRollColor(dto);
    }

    /**
     * 保存大卷颜色提示信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGdyySteelRollColor(GdyySteelRollColor entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        if (entity.getColorCode() == null || entity.getColorCode() == "") {
            entity.setColorCode("#000000");//无值设置默认黑色
        }
        this.saveOrUpdate(entity);
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteGdyySteelRollColor(Long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            GdyySteelRollColor entity = new GdyySteelRollColor();
            entity.setId(ids[i]);
            entity.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            entity.setUpdateTime(new Date());
            this.updateById(entity);
        }
    }

    /**
     * 根据大卷编号判断是否已经存在
     */
    public String checkGdyySteelRollColor(GdyySteelRollColorDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBigRollCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<GdyySteelRollColor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("BIG_ROLL_CODE", dto.getBigRollCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<GdyySteelRollColor> list = gdyySteelRollColorMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }


    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GdyySteelRollColorDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        //做校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GdyySteelRollColorDto> importList = new ArrayList<>();
		// 按业务主键分组
		Map<String, Long> groupMap = list.stream()
				.collect(Collectors.groupingBy(GdyySteelRollColorDto::getBigRollCode, Collectors.counting()));
        for (int i = 0; i < list.size(); i++) {
            GdyySteelRollColorDto entity = list.get(i);
			// excel内业务主键唯一校验
			Long hasValue = groupMap.get(entity.getBigRollCode());
			if (hasValue > 1) {
				entity.setId(-999L);
				String columnName = I18nUtil.getMessage("ui.steelRollColor.column.bigRollCode");
				addImportErrorLog(importLogId, i + 2,
						String.format(I18nUtil.getMessage("ui.data.column.all.conflictRecord"), columnName),
						importErrorLogs);
                failureNum++;
				continue;
			}
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (CollectionUtils.isNotEmpty(validated)) {
            	entity.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                if (StringUtils.isBlank(entity.getColorCode())) {
                    entity.setColorCode("#000000");
                }
                importList.add(entity);
            }
        }
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用merge即可
                if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    gdyySteelRollColorMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        GdyySteelRollColorDto excelItem = list.get(i);
                        //过滤错误的记录
                        if (excelItem.getId() != null && excelItem.getId() == -999L) {
                            continue;
                        }
                        // 唯一性校验
                        String unic = checkGdyySteelRollColor(excelItem);
                        if (unic.equals(UserConstants.UNIQUE)) {
                            //不存在插入
                            successNum++;
                            GdyySteelRollColor color = new GdyySteelRollColor();
                            BeanUtils.copyProperties(excelItem, color);
                            color.setBaseVale(null);
                            gdyySteelRollColorMapper.insert(color);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.color.message.unique"), importErrorLogs);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 执行sql失败，插入导入失败记录
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
