package com.zlt.aps.controller.cx;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.service.ICd15ScheduleResultService;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.service.ICd90ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.entity.CxHalfPartConversion;
import com.zlt.aps.cx.api.service.ICxHalfPartConversionService;
import com.zlt.aps.cx.api.service.ICxScheduleResultService;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.api.service.IGdyyScheduleResultService;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.api.service.IGsqScheduleResultService;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.api.service.INcScheduleResultService;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.service.ITcScheduleResultService;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.service.ITmScheduleResultService;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.api.service.ITqScheduleResultService;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.service.IXwyyScheduleResultService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * 半部件规格换算Controller
 *
 * @author zlt
 * @date 2022-01-20
 */
@Api(tags = "半部件规格换算")
@Controller
@RequestMapping("/cx/conversion")
public class CxHalfPartConversionController extends BaseController {

    @Autowired
    private ICxHalfPartConversionService iCxHalfPartConversionService;
    @Autowired
    private ICxScheduleResultService iCxScheduleResultService;
    @Autowired
    private ITmScheduleResultService iTmScheduleResultService;
    @Autowired
    private ITcScheduleResultService iTcScheduleResultService;
    @Autowired
    private INcScheduleResultService iNcScheduleResultService;
    @Autowired
    private ITqScheduleResultService iTqScheduleResultService;
    @Autowired
    private IGsqScheduleResultService iGsqScheduleResultService;
    @Autowired
    private ICd15ScheduleResultService iCd15ScheduleResultService;
    @Autowired
    private IGdyyScheduleResultService iGdyyScheduleResultService;
    @Autowired
    private ICd90ScheduleResultService iCd90ScheduleResultService;
    @Autowired
    private IXwyyScheduleResultService iXwyyScheduleResultService;

    private final String prefix = "cx/conversion";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:conversion:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", iCxScheduleResultService.selectMaxScheduleDate());
        return prefix + "/conversion";
    }

    @GetMapping("/toChooseMachine")
    public String toChooseMachine(String halfPartType, String machineId, ModelMap modelMap){
        modelMap.put("halfPartType", halfPartType);
        modelMap.put("machineId", machineId);
        CxHalfPartConversion queryParams = new CxHalfPartConversion();
        queryParams.setHalfPartType(halfPartType);
        List<CxHalfPartConversion> machineInfoList = iCxHalfPartConversionService.getMachineInfoListByHalfPartType(queryParams);
        modelMap.put("machineInfoList", machineInfoList);
        return prefix + "/chooseMachine";
    }

    /**
     * 根据条件查询半部件规格换算列表
     */
    @ApiOperation("根据条件查询半部件规格换算列表")
    @RequiresPermissions("cx:conversion:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxHalfPartConversion entity) {

        return iCxHalfPartConversionService.list(entity);
    }

    /**
     * 保存半部件规格换算记录
     */
    @ApiOperation("保存半部件规格换算记录")
    @RequiresPermissions("cx:conversion:save")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxHalfPartConversion entity) {
        // 除了钢带压延，其余排程机台id都不能为空
        if (StringUtil.isBlank(entity.getMachineId()) && !"8".equals(entity.getHalfPartType())) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.machineIdIsNull"), entity.getId());
        }
        // 查询唯一键是否已存在，存在则报错
        Long id = iCxHalfPartConversionService.getScheduleResultByParams(entity);
        if (ObjectUtils.isNotEmpty(id)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"), id);
        }
        Date scheduleDate = entity.getScheduleDate();
        String halfPartType = entity.getHalfPartType();
        return saveRecord(entity, entity.getId(), scheduleDate, halfPartType);
    }

    /**
     * 批量保存半部件规格换算记录
     */
    @ApiOperation("批量保存半部件规格换算记录")
    @RequiresPermissions("cx:conversion:save")
    @PostMapping("/batchSave")
    @ResponseBody
    public AjaxResult batchSave(String listStr, Date scheduleDate) {
        List<CxHalfPartConversion> list = JSON.parseArray(listStr, CxHalfPartConversion.class);
        List<Map<String, String>> result = new ArrayList<>();
        for (CxHalfPartConversion entity : list) {
            Map<String, String> mesMap = new HashMap<>();
            String halfPartType = entity.getHalfPartType();
            // 除了钢带压延，其余排程机台id都不能为空
            if (StringUtil.isBlank(entity.getMachineId()) && !"8".equals(halfPartType)) {
                mesMap.put("halfPartType", halfPartType);
                mesMap.put("code", String.valueOf(AjaxResult.Type.ERROR.value()));
                mesMap.put("msg", I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.machineIdIsNull"));
                mesMap.put("id", String.valueOf(entity.getId()));
                result.add(mesMap);
                continue;
            }
            entity.setScheduleDate(scheduleDate);
            // 查询唯一键是否已存在，存在则报错，跳过当前记录
            Long id = iCxHalfPartConversionService.getScheduleResultByParams(entity);
            if (ObjectUtils.isNotEmpty(id)) {
                mesMap.put("halfPartType", halfPartType);
                mesMap.put("code", String.valueOf(AjaxResult.Type.ERROR.value()));
                mesMap.put("msg", I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
                mesMap.put("id", String.valueOf(id));
                result.add(mesMap);
                continue;
            }
            // 根据半部件类型调用对应接口
            AjaxResult ajaxResult = saveRecord(entity, entity.getId(), scheduleDate, halfPartType);
            mesMap.put("halfPartType", halfPartType);
            mesMap.put("code", String.valueOf(ajaxResult.get(AjaxResult.CODE_TAG)));
            mesMap.put("msg", String.valueOf(ajaxResult.get(AjaxResult.MSG_TAG)));
            mesMap.put("id", String.valueOf(ajaxResult.get(AjaxResult.DATA_TAG)));
            result.add(mesMap);
        }
        return AjaxResult.success(result);
    }

    /**
     * 发布半部件规格换算记录
     */
    @ApiOperation("发布半部件规格换算记录")
    @RequiresPermissions("cx:conversion:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(CxHalfPartConversion entity) {
        // 除了钢带压延，其余排程机台id都不能为空
        if (StringUtil.isBlank(entity.getMachineId()) && !"8".equals(entity.getHalfPartType())) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.machineIdIsNull"), entity.getId());
        }
        // 根据唯一键查询是否有对应记录，将id置空为了不排除本身
        entity.setId(null);
        Long id = iCxHalfPartConversionService.getScheduleResultByParams(entity);
        if (ObjectUtils.isEmpty(id)) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.notScheduleResult"), id);
        }
        Date scheduleDate = entity.getScheduleDate();
        return publishRecord(entity, id, scheduleDate);
    }

    /**
     * 批量发布半部件规格换算记录
     */
    @ApiOperation("批量发布半部件规格换算记录")
    @RequiresPermissions("cx:conversion:publish")
    @PostMapping("/batchPublish")
    @ResponseBody
    public AjaxResult batchPublish(String listStr, Date scheduleDate) {
        List<CxHalfPartConversion> list = JSON.parseArray(listStr, CxHalfPartConversion.class);
        List<Map<String, String>> result = new ArrayList<>();
        for (CxHalfPartConversion entity : list) {
            Map<String, String> mesMap = new HashMap<>();
            String halfPartType = entity.getHalfPartType();
            // 除了钢带压延，其余排程机台id都不能为空
            if (StringUtil.isBlank(entity.getMachineId()) && !"8".equals(halfPartType)) {
                mesMap.put("halfPartType", halfPartType);
                mesMap.put("code", String.valueOf(AjaxResult.Type.ERROR.value()));
                mesMap.put("msg", I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.machineIdIsNull"));
                mesMap.put("id", String.valueOf(entity.getId()));
                result.add(mesMap);
                continue;
            }
            entity.setScheduleDate(scheduleDate);
            // 根据唯一键查询是否有对应记录，将id置空为了不排除本身
            entity.setId(null);
            Long id = iCxHalfPartConversionService.getScheduleResultByParams(entity);
            if (ObjectUtils.isEmpty(id)) {
                mesMap.put("halfPartType", halfPartType);
                mesMap.put("code", String.valueOf(AjaxResult.Type.ERROR.value()));
                mesMap.put("msg", I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.notScheduleResult"));
                mesMap.put("id", String.valueOf(id));
                result.add(mesMap);
                continue;
            }
            // 根据半部件类型调用对应接口
            AjaxResult ajaxResult = publishRecord(entity, id, scheduleDate);
            mesMap.put("halfPartType", halfPartType);
            mesMap.put("code", String.valueOf(ajaxResult.get(AjaxResult.CODE_TAG)));
            mesMap.put("msg", String.valueOf(ajaxResult.get(AjaxResult.MSG_TAG)));
            mesMap.put("id", String.valueOf(id));
            result.add(mesMap);
        }
        return AjaxResult.success(result);
    }

    /**
     * 保存排程记录
     * @param entity 要保存的半部件换算记录
     * @param id 半部件换算记录对应排程id
     * @param scheduleDate 排程日期
     * @param halfPartType 半部件类型
     * @return 结果
     */
    private AjaxResult saveRecord(CxHalfPartConversion entity, Long id, Date scheduleDate, String halfPartType) {
        AjaxResult ajaxResult;
        Double class1Plan = entity.getClass1Plan() == null ? 0 : entity.getClass1Plan();
        Double class2Plan = entity.getClass2Plan() == null ? 0 : entity.getClass2Plan();
        Double class3Plan = entity.getClass3Plan() == null ? 0 : entity.getClass3Plan();
        String machineId = entity.getMachineId();
        switch (halfPartType) {
            case "1":
                // 胎面
                TmScheduleResult tmScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    tmScheduleResult = iTmScheduleResultService.getInfo(id);
                    tmScheduleResult.setMachineId(machineId);
                    tmScheduleResult.setDayPlanQty(class1Plan);
                    tmScheduleResult.setNightPlanQty(class2Plan);
                    tmScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    tmScheduleResult.setIsRelease(tmScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iTmScheduleResultService.changeQty(tmScheduleResult);
                } else {
                    tmScheduleResult = new TmScheduleResult();
                    tmScheduleResult.setScheduleDate(scheduleDate);
                    tmScheduleResult.setTreadCode(entity.getHalfPartCode());
                    tmScheduleResult.setMachineId(machineId);
                    tmScheduleResult.setDayPlanQty(class1Plan);
                    tmScheduleResult.setNightPlanQty(class2Plan);
                    tmScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    tmScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    tmScheduleResult.setDataSource("1");
                    ajaxResult = iTmScheduleResultService.add(tmScheduleResult);
                }
                break;
            case "2":
                // 胎侧
                TcScheduleResult tcScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    tcScheduleResult = iTcScheduleResultService.getInfo(id);
                    tcScheduleResult.setMachineId(machineId);
                    tcScheduleResult.setDayPlanQty(class1Plan);
                    tcScheduleResult.setNightPlanQty(class2Plan);
                    tcScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    tcScheduleResult.setIsRelease(tcScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iTcScheduleResultService.changeQty(tcScheduleResult);
                } else {
                    tcScheduleResult = new TcScheduleResult();
                    tcScheduleResult.setScheduleDate(scheduleDate);
                    tcScheduleResult.setSidewallCode(entity.getHalfPartCode());
                    tcScheduleResult.setMachineId(machineId);
                    tcScheduleResult.setDayPlanQty(class1Plan);
                    tcScheduleResult.setNightPlanQty(class2Plan);
                    tcScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    tcScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    tcScheduleResult.setDataSource("1");
                    ajaxResult = iTcScheduleResultService.add(tcScheduleResult);
                }
                break;
            case "3":
                // 内衬
                NcScheduleResult ncScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    ncScheduleResult = iNcScheduleResultService.getInfo(id);
                    ncScheduleResult.setMachineId(machineId);
                    ncScheduleResult.setDayPlanQty(class1Plan);
                    ncScheduleResult.setNightPlanQty(class2Plan);
                    ncScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    ncScheduleResult.setIsRelease(ncScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iNcScheduleResultService.changeQty(ncScheduleResult);
                } else {
                    ncScheduleResult = new NcScheduleResult();
                    ncScheduleResult.setScheduleDate(scheduleDate);
                    ncScheduleResult.setLiningCode(entity.getHalfPartCode());
                    ncScheduleResult.setMachineId(machineId);
                    ncScheduleResult.setDayPlanQty(class1Plan);
                    ncScheduleResult.setNightPlanQty(class2Plan);
                    ncScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    ncScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    ncScheduleResult.setDataSource("1");
                    ajaxResult = iNcScheduleResultService.add(ncScheduleResult);
                }
                break;
            case "4":
                // 胎圈
                TqScheduleResultDto tqScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    tqScheduleResult = iTqScheduleResultService.getInfo(id);
                    tqScheduleResult.setMachineId(machineId);
                    tqScheduleResult.setMidPlanQty(class1Plan);
                    tqScheduleResult.setNightPlanQty(class2Plan);
                    tqScheduleResult.setDayPlanQty(class3Plan);
                    Double class4Plan = entity.getClass4Plan() == null ? 0 : entity.getClass4Plan();
                    tqScheduleResult.setNextMidPlanQty(class4Plan);
                    tqScheduleResult.setDailyTotalQty(class1Plan + class2Plan + class3Plan + class4Plan);
                    tqScheduleResult.setIsRelease(tqScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iTqScheduleResultService.changeQty(tqScheduleResult);
                } else {
                    tqScheduleResult = new TqScheduleResultDto();
                    tqScheduleResult.setScheduleDate(scheduleDate);
                    tqScheduleResult.setBeadCode(entity.getHalfPartCode());
                    tqScheduleResult.setMachineId(machineId);
                    tqScheduleResult.setMidPlanQty(class1Plan);
                    tqScheduleResult.setNightPlanQty(class2Plan);
                    tqScheduleResult.setDayPlanQty(class3Plan);
                    Double class4Plan = entity.getClass4Plan() == null ? 0 : entity.getClass4Plan();
                    tqScheduleResult.setNextMidPlanQty(class4Plan);
                    tqScheduleResult.setDailyTotalQty(class1Plan + class2Plan + class3Plan + class4Plan);
                    tqScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    tqScheduleResult.setDataSource("1");
                    ajaxResult = iTqScheduleResultService.edit(tqScheduleResult);
                }
                break;
            case "5":
                // 钢丝圈
                GsqScheduleResultDto gsqScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    gsqScheduleResult = iGsqScheduleResultService.getInfo(id);
                    gsqScheduleResult.setMachineId(machineId);
                    gsqScheduleResult.setMidPlanQty(class1Plan);
                    gsqScheduleResult.setNightPlanQty(class2Plan);
                    gsqScheduleResult.setDayPlanQty(class3Plan);
                    gsqScheduleResult.setDailyTotalQty(class1Plan + class2Plan + class3Plan);
                    gsqScheduleResult.setIsRelease(gsqScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iGsqScheduleResultService.changeQty(gsqScheduleResult);
                } else {
                    gsqScheduleResult = new GsqScheduleResultDto();
                    gsqScheduleResult.setScheduleDate(scheduleDate);
                    gsqScheduleResult.setSteelRingCode(entity.getHalfPartCode());
                    gsqScheduleResult.setMachineId(machineId);
                    gsqScheduleResult.setMidPlanQty(class1Plan);
                    gsqScheduleResult.setNightPlanQty(class2Plan);
                    gsqScheduleResult.setDayPlanQty(class3Plan);
                    gsqScheduleResult.setDailyTotalQty(class1Plan + class2Plan + class3Plan);
                    gsqScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    gsqScheduleResult.setDataSource("1");
                    ajaxResult = iGsqScheduleResultService.add(gsqScheduleResult);
                }
                break;
            case "6":
                // 1#钢带 CD15
            case "7":
                // 2#钢带
                Cd15ScheduleResult cd15ScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    cd15ScheduleResult = iCd15ScheduleResultService.getInfo(id);
                    cd15ScheduleResult.setMachineId(machineId);
                    cd15ScheduleResult.setDayPlanQty1(class1Plan);
                    cd15ScheduleResult.setNightPlanQty1(class2Plan);
                    cd15ScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    cd15ScheduleResult.setIsRelease(cd15ScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iCd15ScheduleResultService.changeQty(cd15ScheduleResult);
                } else {
                    cd15ScheduleResult = new Cd15ScheduleResult();
                    cd15ScheduleResult.setScheduleDate(scheduleDate);
                    cd15ScheduleResult.setSteelStripCode1(entity.getHalfPartCode());
                    cd15ScheduleResult.setSteelStripCode2(entity.getSteelStripCode2());
                    cd15ScheduleResult.setMachineId(machineId);
                    cd15ScheduleResult.setDayPlanQty1(class1Plan);
                    cd15ScheduleResult.setNightPlanQty1(class2Plan);
                    cd15ScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    cd15ScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    cd15ScheduleResult.setDataSource("1");
                    ajaxResult = iCd15ScheduleResultService.add(cd15ScheduleResult);
                }
                break;
            case "8":
                // 钢压大卷 GDYY
                GdyyScheduleResultDto gdyyScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    gdyyScheduleResult = iGdyyScheduleResultService.getInfo(id);
                    gdyyScheduleResult.setClass1Plan(class1Plan);
                    gdyyScheduleResult.setClass2Plan(class2Plan);
                    gdyyScheduleResult.setClass3Plan(class3Plan);
                    gdyyScheduleResult.setDailyTotalQty(class1Plan + class2Plan + class3Plan);
                    gdyyScheduleResult.setIsRelease(gdyyScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iGdyyScheduleResultService.changeQty(gdyyScheduleResult);
                } else {
                    gdyyScheduleResult = new GdyyScheduleResultDto();
                    gdyyScheduleResult.setScheduleDate(scheduleDate);
                    gdyyScheduleResult.setBigRollCode(entity.getHalfPartCode());
                    gdyyScheduleResult.setClass1Plan(class1Plan);
                    gdyyScheduleResult.setClass2Plan(class2Plan);
                    gdyyScheduleResult.setClass3Plan(class3Plan);
                    gdyyScheduleResult.setDailyTotalQty(class1Plan + class2Plan + class3Plan);
                    gdyyScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    gdyyScheduleResult.setDataSource("1");
                    ajaxResult = iGdyyScheduleResultService.edit(gdyyScheduleResult);
                }
                break;
            case "9":
                // 1#胎体布 CD90
            case "10":
                // 2#胎体布
            case "11":
                // 3#胎体布
                Cd90ScheduleResult cd90ScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    cd90ScheduleResult = iCd90ScheduleResultService.getInfo(id);
                    cd90ScheduleResult.setMachineId(machineId);
                    cd90ScheduleResult.setDayPlanQty(class1Plan);
                    cd90ScheduleResult.setNightPlanQty(class2Plan);
                    cd90ScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    cd90ScheduleResult.setIsRelease(cd90ScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iCd90ScheduleResultService.changeQty(cd90ScheduleResult);
                } else {
                    cd90ScheduleResult = new Cd90ScheduleResult();
                    cd90ScheduleResult.setScheduleDate(scheduleDate);
                    cd90ScheduleResult.setClothCode(entity.getHalfPartCode());
                    cd90ScheduleResult.setMachineId(machineId);
                    cd90ScheduleResult.setDayPlanQty(class1Plan);
                    cd90ScheduleResult.setNightPlanQty(class2Plan);
                    cd90ScheduleResult.setDailyTotalQty(class1Plan + class2Plan);
                    cd90ScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    cd90ScheduleResult.setDataSource("1");
                    ajaxResult = iCd90ScheduleResultService.add(cd90ScheduleResult);
                }
                break;
            case "12":
                // 帘布大卷 XWYY
                XwyyScheduleResultDto xwyyScheduleResult;
                if (ObjectUtils.isNotEmpty(id)) {
                    xwyyScheduleResult = iXwyyScheduleResultService.getInfo(id);
                    xwyyScheduleResult.setMachineId(machineId);
                    xwyyScheduleResult.setDayPlanQty(class1Plan);
                    xwyyScheduleResult.setNightPlanQty(class2Plan);
                    double totalqty = class1Plan + class2Plan;
                    xwyyScheduleResult.setDailyTotalQty(totalqty);
                    xwyyScheduleResult.setTotalPlan(totalqty);
                    xwyyScheduleResult.setIsRelease(xwyyScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                    ajaxResult = iXwyyScheduleResultService.changeQty(xwyyScheduleResult);
                } else {
                    xwyyScheduleResult = new XwyyScheduleResultDto();
                    xwyyScheduleResult.setScheduleDate(scheduleDate);
                    xwyyScheduleResult.setBigRollCode(entity.getHalfPartCode());
                    xwyyScheduleResult.setMachineId(machineId);
                    xwyyScheduleResult.setDayPlanQty(class1Plan);
                    xwyyScheduleResult.setNightPlanQty(class2Plan);
                    double totalqty = class1Plan + class2Plan;
                    xwyyScheduleResult.setDailyTotalQty(totalqty);
                    xwyyScheduleResult.setTotalPlan(totalqty);
                    xwyyScheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
                    xwyyScheduleResult.setDataSource("1");
                    ajaxResult = iXwyyScheduleResultService.edit(xwyyScheduleResult);
                }
                break;
            default:
                ajaxResult = AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.notHalfPartType"));
                break;
        }
        Long updateId = ObjectUtils.isNotEmpty(id) ? id : iCxHalfPartConversionService.getScheduleResultByParams(entity);
        return ajaxResult.put(AjaxResult.DATA_TAG, updateId);
    }

    /**
     * 发布排程记录
     * @param entity 要发布的半部件规格记录
     * @param id 半部件规格记录对应排程id
     * @param scheduleDate 排程日期
     * @return 结果
     */
    private AjaxResult publishRecord(CxHalfPartConversion entity, Long id, Date scheduleDate) {
        AjaxResult ajaxResult;// 根据半部件类型调用对应接口
        switch (entity.getHalfPartType()) {
            case "1":
                // 胎面
                TmScheduleResult tmScheduleResult = new TmScheduleResult();
                tmScheduleResult.setIds(new Long[]{id});
                tmScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iTmScheduleResultService.publish(tmScheduleResult);
                break;
            case "2":
                // 胎侧
                TcScheduleResult tcScheduleResult = new TcScheduleResult();
                tcScheduleResult.setIds(new Long[]{id});
                tcScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iTcScheduleResultService.publish(tcScheduleResult);
                break;
            case "3":
                // 内衬
                NcScheduleResult ncScheduleResult = new NcScheduleResult();
                ncScheduleResult.setIds(new Long[]{id});
                ncScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iNcScheduleResultService.publish(ncScheduleResult);
                break;
            case "4":
                // 胎圈
                TqScheduleResultDto tqScheduleResult = new TqScheduleResultDto();
                tqScheduleResult.setIds(new Long[]{id});
                tqScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iTqScheduleResultService.publish(tqScheduleResult);
                break;
            case "5":
                // 钢丝圈
                GsqScheduleResultDto gsqScheduleResult = new GsqScheduleResultDto();
                gsqScheduleResult.setIds(new Long[]{id});
                gsqScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iGsqScheduleResultService.publish(gsqScheduleResult);
                break;
            case "6":
                // 1#钢带 CD15
            case "7":
                // 2#钢带
                Cd15ScheduleResult cd15ScheduleResult = new Cd15ScheduleResult();
                cd15ScheduleResult.setIds(new Long[]{id});
                cd15ScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iCd15ScheduleResultService.publish(cd15ScheduleResult);
                break;
            case "8":
                // 钢压大卷 GDYY
                GdyyScheduleResultDto gdyyScheduleResult = new GdyyScheduleResultDto();
                gdyyScheduleResult.setIds(new Long[]{id});
                gdyyScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iGdyyScheduleResultService.publish(gdyyScheduleResult);
                break;
            case "9":
                // 1#胎体布 CD90
            case "10":
                // 2#胎体布
            case "11":
                // 3#胎体布
                Cd90ScheduleResult cd90ScheduleResult = new Cd90ScheduleResult();
                cd90ScheduleResult.setIds(new Long[]{id});
                cd90ScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iCd90ScheduleResultService.publish(cd90ScheduleResult);
                break;
            case "12":
                // 帘布大卷 XWYY
                XwyyScheduleResultDto xwyyScheduleResult = new XwyyScheduleResultDto();
                xwyyScheduleResult.setIds(new Long[]{id});
                xwyyScheduleResult.setScheduleDate(scheduleDate);
                ajaxResult = iXwyyScheduleResultService.publish(xwyyScheduleResult);
                break;
            default:
                ajaxResult = AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxHalfPartConversion.notHalfPartType"), id);
                break;
        }
        return ajaxResult.put(AjaxResult.DATA_TAG, id);
    }
}
