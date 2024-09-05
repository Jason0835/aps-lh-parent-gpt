package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.api.domain.dto.CxSpecColorDto;
import com.zlt.aps.cx.entity.CxSpecColor;
import com.zlt.aps.cx.service.CxSpecColorService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规格字体颜色设置Controller
 *
 * @author chen
 * @date 2021-08-21
 */
@RestController
@RequestMapping("/specColor")
public class CxSpecColorController extends BaseController {
    @Autowired
    private CxSpecColorService cxSpecColorService;

    /**
     * 查询规格字体颜色设置列表
     */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxSpecColorDto dto) {
        CxSpecColor cxSpecColor = new CxSpecColor();
        BeanUtils.copyProperties(dto, cxSpecColor);
        startPage();
        cxSpecColor.setOrderStr(orderStr());
        List<CxSpecColorDto> list = cxSpecColorService.selectCxSpecColorList(cxSpecColor);
        return getDataTable(list);
    }

    /**
     * 获取规格字体颜色设置详细信息
     */
    @GetMapping(value = "/{id}")
    public CxSpecColorDto getInfo(@PathVariable("id") Long id) {
        return cxSpecColorService.selectCxSpecColorById(id);
    }

    /**
     * 新增规格字体颜色设置
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxSpecColorDto dto) {
        CxSpecColor cxSpecColor = new CxSpecColor();
        BeanUtils.copyProperties(dto, cxSpecColor);
        return toAjax(cxSpecColorService.insertCxSpecColor(cxSpecColor));
    }

    /**
     * 修改规格字体颜色设置
     */
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxSpecColorDto dto) {
        CxSpecColor cxSpecColor = new CxSpecColor();
        BeanUtils.copyProperties(dto, cxSpecColor);
        return toAjax(cxSpecColorService.updateCxSpecColor(cxSpecColor));
    }

    /**
     * 删除规格字体颜色设置
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cxSpecColorService.deleteCxSpecColorByIds(ids));
    }

    /**
     * 导出规格字体颜色设置列表
     */
    @PostMapping("/getList")
    public List<CxSpecColorDto> getList(@RequestBody CxSpecColorDto dto) {
        CxSpecColor cxSpecColor = new CxSpecColor();
        BeanUtils.copyProperties(dto, cxSpecColor);
        startPage();
        cxSpecColor.setOrderStr(orderStr());
        return cxSpecColorService.selectCxSpecColorList(cxSpecColor);
    }

    /**
     * 校验规格字体颜色设置唯一性
     */
    @ApiOperation("校验规格字体颜色设置唯一性")
    @PostMapping("/checkCxSpecColorUnique")
    public String checkCxSpecColorUnique(@RequestBody CxSpecColorDto dto) {
        CxSpecColor cxSpecColor = new CxSpecColor();
        BeanUtils.copyProperties(dto, cxSpecColor);
        return cxSpecColorService.checkCxSpecColorUnique(cxSpecColor);
    }

    /**
     * 根据集合导入数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxSpecColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxSpecColorService.importData(list, updateSupport, importLogId);
    }
}
