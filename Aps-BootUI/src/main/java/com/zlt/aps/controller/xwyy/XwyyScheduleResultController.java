package com.zlt.aps.controller.xwyy;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.config.Global;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto2;
import com.zlt.aps.xwyy.api.domain.entity.XwyyDayFinishQty;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.api.service.IXwyyMachineInfoService;
import com.zlt.aps.xwyy.api.service.IXwyyScheduleResultService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 纤维压延排程结果Controller
 *
 * @author chen
 * @date 2021-07-06
 */
@Api(tags = "纤维压延排程结果")
@Controller
@RequestMapping("/xwyy/scheduleResult")
public class XwyyScheduleResultController extends BaseController {

    private final String prefix = "xwyy/scheduleResult";
    @Autowired
    private IXwyyScheduleResultService iXwyyScheduleResultService;
    @Autowired
    private IXwyyMachineInfoService iXwyyMachineInfoService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("xwyy:scheduleResult:view")
    @ApiOperation("跳转到纤维压延排程结果首页")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/scheduleResult";
    }

    /**
     * 跳转至插单页面
     */
    @ApiOperation("跳转到纤维压延排程结果插单页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("minDate", DateUtils.parseDateToStr( "yyyy-MM-dd", new Date()));  //当前日期
        mmap.put("scheduleResult", new XwyyScheduleResultDto());
        return prefix + "/insertOrder";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取纤维压延排程结果信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("scheduleResult", iXwyyScheduleResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转到转机台页面
     *
     * @return 结果
     */
    @ApiOperation("转机台")
    @GetMapping("/changeMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        // 编辑类型为转机台
        mmap.put("editType", "1");
        mmap.put("scheduleResult", iXwyyScheduleResultService.getInfo(id));
        return prefix + "/changeQtyOrMachine";
    }

    /**
     * 跳转至转机台
     */
    @GetMapping("/batchChangeMachine/{ids}")
    public String batchChangeMachine(@PathVariable("ids") String ids, ModelMap mmap) {
        String[] split = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String s : split) {
            idList.add(Long.valueOf(s));
        }
        XwyyScheduleResultDto scheduleResult = new XwyyScheduleResultDto();
        scheduleResult.setIds2(idList);
        mmap.put("selectList", iXwyyScheduleResultService.getInfos(scheduleResult));
        return prefix + "/changeQtyOrMachine2";
    }

    /**
     * 跳转到调计划量页面
     *
     * @return 结果
     */
    @ApiOperation("调计划量")
    @GetMapping("/changeQty/{id}")
    public String changeQty(@PathVariable("id") String idAndMaxBigRollCode, ModelMap mmap) {
        // 编辑类型为调计划量
        String[] strings = idAndMaxBigRollCode.split(",");
        mmap.put("editType", "2");
        XwyyScheduleResultDto info = iXwyyScheduleResultService.getInfo(Long.valueOf(strings[0]));
        if (strings.length > 1) {
            info.setMaxBigRollCode(strings[1]);
        }
        mmap.put("scheduleResult", info);
        return prefix + "/changeQtyOrMachine";
    }

    /**
     * 弹出自动排程日期选择框
     *
     * @return 结果
     */
    @ApiOperation("弹出自动排程日期选择框")
    @GetMapping("/toAutoPlan")
    public String toAutoPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/autoPlan";
    }

    /**
     * 跳转到选机台
     *
     * @return 结果
     */
    @ApiOperation("跳转到选机台")
    @GetMapping("/chooseMachine/{id}")
    public String chooseMachine(@PathVariable("id") String idAndRowIndex, ModelMap mmap) {
        String[] idAndRowIndexArr = idAndRowIndex.split(",");
        XwyyScheduleResultDto dto = iXwyyScheduleResultService.getInfo(Long.valueOf(idAndRowIndexArr[0]));
        XwyyMachineInfo machineInfo = new XwyyMachineInfo();
        machineInfo.setStatus("0");
        List<XwyyMachineInfo> machineInfoList = iXwyyMachineInfoService.exportList(machineInfo);
        Map<String, XwyyMachineInfo> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(item -> item.getId() + "", item -> item));

        if (StringUtils.isNotEmpty(dto.getMachineId())) {
            List<XwyyMachineInfo> newMachineInfoList = new ArrayList<>();
            String[] machineIds = dto.getMachineId().split(",");
            for (String item : machineIds) {
                if (machineCodeMap.get(item) != null) {
                    newMachineInfoList.add(machineCodeMap.get(item));
                }
            }
            mmap.put("machineInfoList", newMachineInfoList);
        } else {
            mmap.put("machineInfoList", machineInfoList);
        }
        mmap.put("id", idAndRowIndexArr[0]);
        mmap.put("rowIndex", idAndRowIndexArr[1]);
        mmap.put("publishSuccessCount", idAndRowIndexArr[2]);
        return prefix + "/chooseMachine";
    }

    /**
     * 选机台
     *
     * @return 结果
     */
    @ApiOperation("选机台")
    @PostMapping("/chooseMachine")
    @ResponseBody
    public AjaxResult chooseMachine(XwyyScheduleResultDto dto) {
        return iXwyyScheduleResultService.chooseMachine(dto);
    }

    /**
     * 根据条件查询纤维压延排程结果列表
     */
    @ApiOperation("根据条件查询纤维压延排程结果列表")
    @RequiresPermissions("xwyy:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyScheduleResultDto dto) {
        //设置默认排程日期,这里在后端设置会有问题
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        return iXwyyScheduleResultService.list(dto);
    }

    /**
     * 修改或新增纤维压延排程结果
     */
    @ApiOperation("修改或新增纤维压延排程结果（id为空则新增，id不为空则修改）")
    @RequiresPermissions({"xwyy:scheduleResult:edit", "xwyy:scheduleResult:insertOrder", "xwyy:scheduleResult:changeMachine", "xwyy:scheduleResult:changeQty"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(XwyyScheduleResultDto dto) {
        if (dto.getId() == null) {
            double class1Plan = dto.getNightPlanQty() == null ? 0d : dto.getNightPlanQty();
            double class2Plan = dto.getDayPlanQty() == null ? 0d : dto.getDayPlanQty();
            double class3Plan = dto.getFac2Class1Plan() == null ? 0d : dto.getFac2Class1Plan();
            double class4Plan = dto.getFac2Class2Plan() == null ? 0d : dto.getFac2Class2Plan();
            double class5Plan = dto.getFac2Class3Plan() == null ? 0d : dto.getFac2Class3Plan();
            double class6Plan = dto.getFac5Class1Plan() == null ? 0d : dto.getFac5Class1Plan();
            double class7Plan = dto.getFac5Class2Plan() == null ? 0d : dto.getFac5Class2Plan();
            double class8Plan = dto.getFac5Class3Plan() == null ? 0d : dto.getFac5Class3Plan();
            // 若插单量为0报错
            if ((class1Plan + class2Plan + class3Plan + class4Plan + class5Plan + class6Plan + class7Plan + class8Plan) == 0d) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.qty.zero"));
            }
            dto.setDataSource("1");
        }
        return iXwyyScheduleResultService.edit(dto);
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @RequiresPermissions("xwyy:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(XwyyScheduleResultDto dto) {
        return iXwyyScheduleResultService.changeMachine(dto);
    }

    /**
     * 转机台
     */
    @ApiOperation("转机台")
    @PostMapping("/batchChangeMachine/{machineId}")
    @ResponseBody
    public AjaxResult batchChangeMachine(@PathVariable("machineId") String machineId, String selects) {
        List<XwyyScheduleResultDto> scheduleResultList = JSON.parseArray(selects, XwyyScheduleResultDto.class);
        XwyyScheduleResultDto query = new XwyyScheduleResultDto();
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (XwyyScheduleResultDto scheduleResult : scheduleResultList) {
            query.setId(scheduleResult.getId());
            query.setScheduleDate(scheduleResult.getScheduleDate());
            query.setMachineId(machineId);
            query.setBigRollCode(scheduleResult.getBigRollCode());
            Boolean unique = iXwyyScheduleResultService.checkUnique(query);
            if (!unique) {
                if (sb1.length() > 0) {
                    sb1.append(",").append(query.getBigRollCode());
                } else {
                    sb1.append(query.getBigRollCode());
                }
                continue;
            }
            scheduleResult.setMachineId(machineId);
            AjaxResult result = iXwyyScheduleResultService.changeMachine(scheduleResult);
            if (result.get(GatewayConstants.MSG_TAG).equals(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"))) {
                if (sb2.length() > 0) {
                    sb2.append(",").append(query.getBigRollCode());
                } else {
                    sb2.append(query.getBigRollCode());
                }
            }
        }
        if (sb1.length() > 0) {
            sb1.append(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        if (sb2.length() > 0) {
            sb2.append(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById2"));
        }
        sb1.append(sb2);
        if (sb1.length() > 0) {
            return AjaxResult.error(sb1.toString());
        }
        return AjaxResult.success();
    }

    /**
     * 调量
     */
    @ApiOperation("调量")
    @RequiresPermissions("xwyy:scheduleResult:changeQty")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(XwyyScheduleResultDto dto) {
        return iXwyyScheduleResultService.changeQty(dto);
    }

    /**
     * 删除纤维压延排程结果
     */
    @ApiOperation("删除纤维压延排程结果（id不为空）")
    @RequiresPermissions("xwyy:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String removeList) {
        List<XwyyScheduleResultDto> list = JSON.parseArray(removeList, XwyyScheduleResultDto.class);
        return iXwyyScheduleResultService.remove(list);
    }

    /**
     * 导出纤维压延排程结果
     */
    @ApiOperation("导出纤维压延排程结果")
    @RequiresPermissions("xwyy:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyScheduleResultDto dto) throws IOException {
        //若是没传日期则默认查询当日排程
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        dto.setYear(DateFormatUtils.format(dto.getScheduleDate(), "yyyy"));
        dto.setMonth(DateFormatUtils.format(dto.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iXwyyScheduleResultService.exportData(dto);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.scheduleResult.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data,fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }

    /**
     * 生成自动排程前校验
     *
     * @param dto 日期
     * @return 响应
     */
    @ApiOperation("校验选择的日期是否已经生成排程记录")
    @PostMapping("/validateAutoPlan")
    @ResponseBody
    public AjaxResult validateAutoPlan(XwyyScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        int releasingOrTimeoutByDate = iXwyyScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean unique = iXwyyScheduleResultService.checkUnique(dto);
        if (unique) {
            // 未生成，直接生成
            return AjaxResult.success("2");
        } else {
            // 排程记录已生成，弹窗提示，确认后重新生成
            return AjaxResult.success("1");
        }
    }

    /**
     * 校验记录唯一性
     *
     * @param dto 日期及钢丝圈代码
     * @return 是否唯一
     */
    @ApiOperation("校验记录唯一性")
    @PostMapping("/checkScheduleResultUnique")
    @ResponseBody
    public String checkUnique(XwyyScheduleResultDto dto) {
        // 根据传入的日期查询是否已经生成排程记录
        Boolean unique = iXwyyScheduleResultService.checkUnique(dto);
        if (!unique) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("xwyy:scheduleResult:autoPlan")
    @PostMapping("/autoPlan")
    @ResponseBody
    public AjaxResult autoPlan(XwyyScheduleResultDto dto) {
        // 用户点击过确定重新生成排程记录,或已有权限重新生成排程记录
        //TODO 执行自动排程算法
        return iXwyyScheduleResultService.autoPlan(dto);
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("xwyy:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(XwyyScheduleResultDto dto) {
        if (dto.getScheduleDate() == null) {
            dto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        return iXwyyScheduleResultService.publish(dto);
    }

    /**
     * 插单校验
     */
    @PostMapping("/validateAdd")
    @ResponseBody
    public AjaxResult validateAdd(XwyyScheduleResultDto dto) {
        int releasingOrTimeoutByDate = iXwyyScheduleResultService.isReleasingOrTimeoutByDate(dto);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean unique = iXwyyScheduleResultService.checkUnique(dto);
        if (unique) {
            return AjaxResult.success("0");
        }
        return AjaxResult.success();
    }

    /**
     * 跳转到公共排程结果导入页面
     * @param mmap 用于存放当前模块前缀路径
     */
    @GetMapping("/toImport")
    public String toImport(ModelMap mmap){
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("prefix", prefix);
        return "common/importData";
    }

    /**
     * 跳转到公共排程结果导入页面
     * @param mmap 用于存放当前模块前缀路径
     */
    @GetMapping("/toImport2")
    public String toImport2(ModelMap mmap){
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        mmap.put("prefix", prefix);
        return "common/importData2";
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String lang = AuthorizationUtils.getLang();  //国际化编码
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.XWYY_EN_TEMP : ApsBootConstant.XWYY_ZH_TEMP);  //根据国际化获取导入模板名称
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "xwyy/" + tempName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.scheduleResult.modelName");
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils4UI.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     * @throws Exception
     */
    @RequiresPermissions("xwyy:scheduleResult:import")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        XwyyScheduleResultDto entity=new XwyyScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iXwyyScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iXwyyScheduleResultService.isPublish(entity);
        if (isPublish) {
            return  AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.data.column.xwyy.scheduleResult.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in= new ByteArrayInputStream(data);
        ExcelUtil<XwyyScheduleResultDto> util = new ExcelUtil<>(XwyyScheduleResultDto.class);
        List<XwyyScheduleResultDto> list = util.importExcel(in, 1);

        AjaxResult ajaxResult = iXwyyScheduleResultService.importData(list, importLog.getId(), DateFormatUtils.format(scheduleDate,"yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
    /**
     * 数据导入
     *
     * @param file
     * @return
     * @throws Exception
     */
    @RequiresPermissions("xwyy:scheduleResult:import2")
    @ApiOperation("数据导入")
    @PostMapping("/importScheduleData2")
    @ResponseBody
    public AjaxResult importData2(MultipartFile file, Date scheduleDate) throws Exception {
        if (scheduleDate.before(DateUtils.getNowDate("yyyy-MM-dd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.scheduleDateError"));
        }
        //若当天存在发布成功、发布中、超时失败的记录则不予导入
        XwyyScheduleResultDto entity=new XwyyScheduleResultDto();
        entity.setScheduleDate(scheduleDate);
        int releasingOrTimeoutByDate = iXwyyScheduleResultService.isReleasingOrTimeoutByDate(entity);
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutByDate"));
        }
        Boolean isPublish = iXwyyScheduleResultService.isPublish(entity);
        if (isPublish) {
            return  AjaxResult.error(I18nUtil.getMessage("ui.biz.alter.publishedNotImport"));
        }
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.data.column.xwyy.scheduleResult.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in= new ByteArrayInputStream(data);
        ExcelUtil<XwyyScheduleResultDto2> util = new ExcelUtil<>(XwyyScheduleResultDto2.class);
        List<XwyyScheduleResultDto2> list = util.importExcel(in, 1);
        List<XwyyScheduleResultDto> newList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            newList = list.stream().map(a -> {
                XwyyScheduleResultDto result = new XwyyScheduleResultDto();
                BeanUtils.copyProperties(a, result);
                return result;
            }).collect(Collectors.toList());
        }
        AjaxResult ajaxResult = iXwyyScheduleResultService.importData(newList, importLog.getId(), DateFormatUtils.format(scheduleDate,"yyyy-MM-dd"));
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 跳转到更改发布状态页面
     */
    @GetMapping("/toChangeReleaseStatus")
    public String changeReleaseStatus(ModelMap map, Date scheduleDate) {
        map.put("prefix", prefix);
        map.put("scheduleDate", scheduleDate);
        return "common/changeReleaseStatus";
    }

    /**
     * 更改发布状态
     */
    @ApiOperation("更改发布状态")
    @RequiresRoles("admin")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(XwyyScheduleResultDto entity) {
        Date scheduleDate = entity.getScheduleDate();
        if (scheduleDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.chooseScheduleDate"));
        }
        return iXwyyScheduleResultService.changeReleaseStatus(entity);
    }

    /**
     * 根据帘布大卷代号获取帘线大卷标准长度
     * @param bigRollCode 帘布大卷代号
     * @return 帘线大卷标准长度
     */
    @ApiOperation("根据帘布大卷代号获取帘线大卷标准长度")
    @RequiresPermissions("xwyy:scheduleResult:insertOrder")
    @PostMapping("/getActClothLength")
    @ResponseBody
    public AjaxResult getActClothLength(String bigRollCode){
        return iXwyyScheduleResultService.getActClothLength(bigRollCode);
    }

    /**
     * 跳转到归并中夜班页面
     */
    @GetMapping("/toCombinationMiddleAndNight")
    public String toCombinationMiddleAndNight(String ids, Date scheduleDate, ModelMap modelMap){
        modelMap.put("prefix", prefix);
        modelMap.put("ids", ids);
        modelMap.put("scheduleDate", scheduleDate);
        return "common/combinationMiddleAndNight";
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @ApiOperation("中夜班归并")
    @RequiresPermissions("xwyy:xwyyScheduleResult:combinationMiddleAndNight")
    @PostMapping("/combinationMiddleAndNight")
    @ResponseBody
    public AjaxResult combinationMiddleAndNight(String ids, String classifiedShift) {
        Long[] arr = Convert.toLongArray(ids);
        return iXwyyScheduleResultService.combinationMiddleAndNight(arr, classifiedShift);
    }

    /**
     * 完成量下载模板
     *
     * @param response 下载
     * @throws IOException 异常
     */
    @ApiOperation("完成量下载模板")
    @GetMapping("/importFinishQtyTemplate")
    @ResponseBody
    public AjaxResult importFinishQtyTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName");
        ExcelUtil<XwyyDayFinishQty> util = new ExcelUtil<>(XwyyDayFinishQty.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 完成量数据导入
     *
     * @param file 要导入的文件
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("xwyy:finishQty:import")
    @ApiOperation("完成量数据导入")
    @PostMapping("/importFinishQty")
    @ResponseBody
    public AjaxResult importFinishQty(MultipartFile file) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data,
                ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.data.column.dayFinishQty.modelName"),
                file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<XwyyDayFinishQty> util = new ExcelUtil<>(XwyyDayFinishQty.class);
        List<XwyyDayFinishQty> list = util.importExcel(in);

        AjaxResult ajaxResult = iXwyyScheduleResultService.importFinishQty(list, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    @ResponseBody
    public AjaxResult getSummaryVo(XwyyScheduleResultDto scheduleResult) {
        return iXwyyScheduleResultService.getSummaryVo(scheduleResult);
    }

    public String importFilePath = Global.getUploadPath();

    @RequiresPermissions("xwyy:halfYyImportBak:importExcelToListAndExport")
    @ApiOperation("导出线下计划导入列表")
    @PostMapping({"/importExcelToListAndExport"})
    @ResponseBody
    public void importExcelToListAndExport(@RequestPart("file") MultipartFile file, HttpServletResponse response) throws IOException {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        String modelName = I18nUtil.getMessage("ui.data.column.halfYyExportData.modelName");
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(modelName);
        context.setProcedureCode("0");
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        byte[] excelBytes = iXwyyScheduleResultService.importExcelToListAndExport(context);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, modelName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 将线下排程模板的昨日计划、昨日库存，导入到系统
     *
     * @param file 导入文件
     */
    @RequiresPermissions("xwyy:halfYyImportBak:importExcelToLastDayPlanAndStock")
    @ApiOperation("线下模板导入昨日计划")
    @PostMapping({"/importExcelToLastDayPlanAndStock"})
    @ResponseBody
    public AjaxResult importExcelToLastDayPlanAndStock(@RequestPart("file") MultipartFile file, boolean updateSupport) throws IOException {
        String message = I18nUtil.getMessage("ui.data.column.halfYyImportBak.modelName");
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(message);
        context.setProcedureCode("0");
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iXwyyScheduleResultService.importExcelToLastDayPlanAndStock(context, true);
        return ajaxResult;
    }
}
