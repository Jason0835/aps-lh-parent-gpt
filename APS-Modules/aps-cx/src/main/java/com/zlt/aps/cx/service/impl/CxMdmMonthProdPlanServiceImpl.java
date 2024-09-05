package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictTypeService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.api.domain.entity.*;
import com.zlt.aps.cx.mapper.ConstructionInfoMapper;
import com.zlt.aps.cx.mapper.CxEstimateExceedShortMapper;
import com.zlt.aps.cx.mapper.CxMdmMonthProdPlanMapper;
import com.zlt.aps.cx.service.CxMachineInfoService;
import com.zlt.aps.cx.service.MdmMonthProdPlanService;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 主计划月度生产计划Service业务层处理
 *
 * @author zlt
 * @date 2021-09-15
 */
@Service
public class CxMdmMonthProdPlanServiceImpl implements MdmMonthProdPlanService {
    @Autowired
    private CxMdmMonthProdPlanMapper mdmMonthProdPlanMapper;

    @Autowired
    private CxEstimateExceedShortMapper cxEstimateExceedShortMapper;

    @Autowired
    private CxMachineInfoService machineInfoService;

    @Autowired
    ISysDictTypeService iSysDictTypeService;

    @Autowired
    MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;

    @Autowired
    private ConstructionInfoMapper constructionInfoMapper;

    /**
     * 查询主计划月度生产计划
     *
     * @param id 主计划月度生产计划ID
     * @return 主计划月度生产计划
     */
    @Override
    public MdmMonthProdPlan selectMdmMonthProdPlanById(Long id) {
        return mdmMonthProdPlanMapper.selectMdmMonthProdPlanById(id);
    }

    /**
     * 查询主计划月度生产计划列表
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 主计划月度生产计划
     */
    @Override
    public List<MdmMonthProdPlan> selectMdmMonthProdPlanList(MdmMonthProdPlan mdmMonthProdPlan) {
        return mdmMonthProdPlanMapper.selectMdmMonthProdPlanList(mdmMonthProdPlan);
    }

    /**
     * 新增主计划月度生产计划
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    @Override
    public AjaxResult insertMdmMonthProdPlan(MdmMonthProdPlan mdmMonthProdPlan) {
        String mainPlanMonth = DateFormatUtils.format(mdmMonthProdPlan.getMainPlanMonth(), "yyyyMM");
        String year = mainPlanMonth.substring(0, 4);
        String month = mainPlanMonth.substring(4);
        String isFinal = mdmMonthProdPlan.getIsFinamized();
        String planMainVersion = year + month + System.currentTimeMillis();
        planMainVersion = mdmMonthPlanAmountSumService.getApsMainPlanVersion(planMainVersion, year, month, isFinal);
        mdmMonthProdPlan.setBaseVale(null);
        mdmMonthProdPlan.setMonthPlanApsVersion(planMainVersion);
        mdmMonthProdPlanMapper.insertMdmMonthProdPlan(mdmMonthProdPlan);
        //todo 调用

        return AjaxResult.success();
    }

    /**
     * 修改主计划月度生产计划
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMdmMonthProdPlan(MdmMonthProdPlan mdmMonthProdPlan) {
        MdmMonthProdPlan mdmMonthProdPlan0= mdmMonthProdPlanMapper.selectMdmMonthProdPlanById(mdmMonthProdPlan.getId());
        if(!compare(mdmMonthProdPlan0.getBomDataVersion(),mdmMonthProdPlan.getBomDataVersion())){
            int a=mdmMonthProdPlanMapper.updateMdmMonthProdPlan(mdmMonthProdPlan);
            mdmMonthProdPlan.setBaseVale(mdmMonthProdPlan.getId());
            try{
                mdmMonthPlanAmountSumService.recalculateByApsVersion(mdmMonthProdPlan0.getMonthPlanApsVersion());
            }catch (Exception e){
                e.printStackTrace();
                return -999;
            }
            return a;
        }
        return 1;
    }

    /**
     * 修改主计划月度生产计划
     *
     * @param mdmMonthProdPlan 主计划月度生产计划
     * @return 结果
     */
    @Override
    public int updateExpectedExcessArrears(MdmMonthProdPlan mdmMonthProdPlan) {
       return mdmMonthProdPlanMapper.updateMdmMonthProdPlan(mdmMonthProdPlan);
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    /**
     * 批量删除主计划月度生产计划
     *
     * @param ids 需要删除的主计划月度生产计划ID
     * @return 结果
     */
    @Override
    public int deleteMdmMonthProdPlanByIds(Long[] ids) {
        return mdmMonthProdPlanMapper.deleteMdmMonthProdPlanByIds(ids);
    }

    /**
     * 删除主计划月度生产计划信息
     *
     * @param id 主计划月度生产计划ID
     * @return 结果
     */
    @Override
    public int deleteMdmMonthProdPlanById(Long id) {
        return mdmMonthProdPlanMapper.deleteMdmMonthProdPlanById(id);
    }

    /**
     * 校验主计划月度生产计划唯一性
     */
    @Override
    public String checkMdmMonthProdPlanUnique(MdmMonthProdPlan mdmMonthProdPlan) {
        if (mdmMonthProdPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MdmMonthProdPlan> list = mdmMonthProdPlanMapper.selectMdmMonthProdPlanList(mdmMonthProdPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入主计划月度生产计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult importData(byte[] data, String mainPlanMonth, boolean updateSupport, Long importLogId, boolean isFinamized,Map<String, String> dictMap) throws Exception {

        List<MdmMonthProdPlan> list=parseObject(data,mainPlanMonth,dictMap);

        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmMonthProdPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            MdmMonthProdPlan entity = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 4, entity);
            if (CollectionUtils.isEmpty(validated)) {
                //时间校验
                if (entity.getBeginDate()!=null && entity.getEndDate()!=null) {
                    if(entity.getBeginDate().after(entity.getEndDate())){
                        failureNum++;
                        addImportErrorLog(importLogId, i + 4, I18nUtil.getMessage("ui.data.column.beginDateCanNotGreatThanEndDate"), importErrorLogs);
                        continue;
                    }
                }
                //日期范围和每日计划量不能同时为空
                if (entity.getBeginDate()==null && entity.getEndDate()==null) {
                    if(!hasQty(entity)){
                        failureNum++;
                        addImportErrorLog(importLogId, i + 4, I18nUtil.getMessage("ui.data.column.dateAndDailyQtyCanNotAllNull"), importErrorLogs);
                        continue;
                    }
                }
                successNum++;
                entity.setDataSource("0");
                entity.setBaseVale(null);
                importList.add(entity);
            } else {
                failureNum++;
                importErrorLogs.addAll(validated);
            }
        }

        // 导入接入 importList（导入集合） isFinamized(是否定稿) mainPlanMonth(导入年月 String yyyy-MM)
        // 导入日期格式校验
        if (StringUtils.isBlank(mainPlanMonth)) {
            String message = I18nUtil.getMessage("ui.error.message.column.mainPlanMonthError");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        try {
            DateUtils.parseDate(mainPlanMonth, DateUtils.YYYY_MM);
        } catch (ParseException e) {
            String message = I18nUtil.getMessage("ui.error.message.column.mainPlanMonthError");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        // 导入数据不为空，则调用导入接口
        if (CollectionUtils.isNotEmpty(importList)) {
            // 构建接口参数
            // 主计划年份
            String year = mainPlanMonth.substring(0, mainPlanMonth.indexOf("-"));
            // 主计划月份
            String month = mainPlanMonth.substring(mainPlanMonth.indexOf("-") + 1, mainPlanMonth.length());
            // 定稿标识
            Integer isFinal = isFinamized ? 0 : 1;
            // 主计划明细类型转换成接口可接收的类型
            List<com.zlt.aps.common.engine.domain.MdmMonthProdPlan> prodList = importList.stream()
                    .map(plan -> this.changeMdmMonthProdPlan(plan)).collect(Collectors.toList());
            // 调用导入接口
            mdmMonthPlanAmountSumService.importMonthPlan(year, month, isFinal, prodList);
        }

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 判断1-30日是否存在计划量
     * @param mdmMonthProdPlan
     * @return
     */
    public boolean hasQty(MdmMonthProdPlan mdmMonthProdPlan){
        boolean hasQty=false;
        if(mdmMonthProdPlan.getProductQty1()!=null && mdmMonthProdPlan.getProductQty1()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty2()!=null && mdmMonthProdPlan.getProductQty2()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty3()!=null && mdmMonthProdPlan.getProductQty3()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty4()!=null && mdmMonthProdPlan.getProductQty4()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty5()!=null && mdmMonthProdPlan.getProductQty5()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty6()!=null && mdmMonthProdPlan.getProductQty6()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty7()!=null && mdmMonthProdPlan.getProductQty7()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty8()!=null && mdmMonthProdPlan.getProductQty8()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty9()!=null && mdmMonthProdPlan.getProductQty9()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty10()!=null && mdmMonthProdPlan.getProductQty10()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty11()!=null && mdmMonthProdPlan.getProductQty11()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty12()!=null && mdmMonthProdPlan.getProductQty12()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty13()!=null && mdmMonthProdPlan.getProductQty13()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty14()!=null && mdmMonthProdPlan.getProductQty14()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty15()!=null && mdmMonthProdPlan.getProductQty15()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty16()!=null && mdmMonthProdPlan.getProductQty16()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty17()!=null && mdmMonthProdPlan.getProductQty17()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty18()!=null && mdmMonthProdPlan.getProductQty18()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty19()!=null && mdmMonthProdPlan.getProductQty19()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty20()!=null && mdmMonthProdPlan.getProductQty20()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty21()!=null && mdmMonthProdPlan.getProductQty21()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty22()!=null && mdmMonthProdPlan.getProductQty22()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty23()!=null && mdmMonthProdPlan.getProductQty23()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty24()!=null && mdmMonthProdPlan.getProductQty24()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty25()!=null && mdmMonthProdPlan.getProductQty25()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty26()!=null && mdmMonthProdPlan.getProductQty26()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty27()!=null && mdmMonthProdPlan.getProductQty27()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty28()!=null && mdmMonthProdPlan.getProductQty28()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty29()!=null && mdmMonthProdPlan.getProductQty29()>0){  return hasQty=true; };
        if(mdmMonthProdPlan.getProductQty30()!=null && mdmMonthProdPlan.getProductQty30()>0){  return hasQty=true; };
        return hasQty;
    }

    /**
     * 解析到对象 MdmMonthProdPlan
     * @param data
     * @param mainPlanMonth
     * @param dictMap
     * @return List<MdmMonthProdPlan>
     * @throws Exception
     */
    public List<MdmMonthProdPlan> parseObject(byte[] data,String mainPlanMonth,Map<String, String> dictMap)throws Exception{

        List<MdmMonthProdPlan> list=new ArrayList<>();
        if (data==null){
            return list;
        }
        InputStream in = new ByteArrayInputStream(data);
        if (in==null){
            return list;
        }
        Workbook workbook=WorkbookFactory.create(in);
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();

        Calendar ca = Calendar.getInstance();
        ca.setTime(DateUtils.parseDate(mainPlanMonth,"yyyy-MM"));
        int lastDay = ca.getActualMaximum(Calendar.DATE);

        if (rows > 0) {
            for (int i = 3; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String val0 =getCellValue(row.getCell(0));
                String val1 =getCellValue(row.getCell(1));
                String val2 =getCellValue(row.getCell(2));
                String val3 =getCellValue(row.getCell(3));
                String val4 =getCellValue(row.getCell(4));
                String val5 =getCellValue(row.getCell(5));
                String val6 =getCellValue(row.getCell(6));
                String val7 =getCellValue(row.getCell(7));
                String val8 =getCellValue(row.getCell(8));
                String val10 =getCellValue(row.getCell(10));
                String val11 =getCellValue(row.getCell(11));
                String val15 =getCellValue(row.getCell(15));
                String val16 =getCellValue(row.getCell(16));
                String val17 =getCellValue(row.getCell(17));
                String val18 =getCellValue(row.getCell(18));
                String val19 =getCellValue(row.getCell(19));
                String val20 =getCellValue(row.getCell(20));
                String val21 =getCellValue(row.getCell(21));
                String val22 =getCellValue(row.getCell(22));
                String val23 =getCellValue(row.getCell(23));
                String val24 =getCellValue(row.getCell(24));
                String val25 =getCellValue(row.getCell(25));
                String val26 =getCellValue(row.getCell(26));
                String val27 =getCellValue(row.getCell(27));
                String val28 =getCellValue(row.getCell(28));
                String val29 =getCellValue(row.getCell(29));
                String val30 =getCellValue(row.getCell(30));
                String val31 =getCellValue(row.getCell(31));
                String val32 =getCellValue(row.getCell(32));
                String val33 =getCellValue(row.getCell(33));
                String val34 =getCellValue(row.getCell(34));
                String val35 =getCellValue(row.getCell(35));
                String val36 =getCellValue(row.getCell(36));
                String val37 =getCellValue(row.getCell(37));
                String val38 =getCellValue(row.getCell(38));
                String val39 =getCellValue(row.getCell(39));
                String val40 =getCellValue(row.getCell(40));
                String val41 =getCellValue(row.getCell(41));
                String val42 =getCellValue(row.getCell(42));
                String val43 =getCellValue(row.getCell(43));
                String val44 =getCellValue(row.getCell(44));
                String val45 =getCellValue(row.getCell(45));
                String val46 =getCellValue(row.getCell(46));
                String val47 =getCellValue(row.getCell(47));

                MdmMonthProdPlan mdmMonthProdPlan=new MdmMonthProdPlan();
                mdmMonthProdPlan.setMaterialCode(getStringValue(val0));
                mdmMonthProdPlan.setSpecDesc(getStringValue(val1));
                mdmMonthProdPlan.setEmbryoCode(getStringValue(val2));
                mdmMonthProdPlan.setSpecDimension(getBigdecimalValue(val3));
                mdmMonthProdPlan.setQualityGrade(getStringValue(val4));
                mdmMonthProdPlan.setStorageLocation(dictMap==null?null:dictMap.get(val5));
                mdmMonthProdPlan.setSpecialRequirements(getStringValue(val6));
                mdmMonthProdPlan.setTheoryProductionPlan(getLongValue(val7));
                mdmMonthProdPlan.setExpectedExcessArrears(getLongValue(val8));
//                mdmMonthProdPlan.setPlanModifyQty(getLongValue(val10));
                mdmMonthProdPlan.setActualArrangement(getLongValue(val11));
                mdmMonthProdPlan.setRemark(getStringValue(val15));
                mdmMonthProdPlan.setBeginDate(getDateValue(mainPlanMonth,val16,lastDay));
                mdmMonthProdPlan.setEndDate(getDateValue(mainPlanMonth,val17,lastDay));
                mdmMonthProdPlan.setProductQty1(getLongValue(val18));
                mdmMonthProdPlan.setProductQty2(getLongValue(val19));
                mdmMonthProdPlan.setProductQty3(getLongValue(val20));
                mdmMonthProdPlan.setProductQty4(getLongValue(val21));
                mdmMonthProdPlan.setProductQty5(getLongValue(val22));
                mdmMonthProdPlan.setProductQty6(getLongValue(val23));
                mdmMonthProdPlan.setProductQty7(getLongValue(val24));
                mdmMonthProdPlan.setProductQty8(getLongValue(val25));
                mdmMonthProdPlan.setProductQty9(getLongValue(val26));
                mdmMonthProdPlan.setProductQty10(getLongValue(val27));
                mdmMonthProdPlan.setProductQty11(getLongValue(val28));
                mdmMonthProdPlan.setProductQty12(getLongValue(val29));
                mdmMonthProdPlan.setProductQty13(getLongValue(val30));
                mdmMonthProdPlan.setProductQty14(getLongValue(val31));
                mdmMonthProdPlan.setProductQty15(getLongValue(val32));
                mdmMonthProdPlan.setProductQty16(getLongValue(val33));
                mdmMonthProdPlan.setProductQty17(getLongValue(val34));
                mdmMonthProdPlan.setProductQty18(getLongValue(val35));
                mdmMonthProdPlan.setProductQty19(getLongValue(val36));
                mdmMonthProdPlan.setProductQty20(getLongValue(val37));
                mdmMonthProdPlan.setProductQty21(getLongValue(val38));
                mdmMonthProdPlan.setProductQty22(getLongValue(val39));
                mdmMonthProdPlan.setProductQty23(getLongValue(val40));
                mdmMonthProdPlan.setProductQty24(getLongValue(val41));
                mdmMonthProdPlan.setProductQty25(getLongValue(val42));
                mdmMonthProdPlan.setProductQty26(getLongValue(val43));
                mdmMonthProdPlan.setProductQty27(getLongValue(val44));
                mdmMonthProdPlan.setProductQty28(getLongValue(val45));
                mdmMonthProdPlan.setProductQty29(getLongValue(val46));
                mdmMonthProdPlan.setProductQty30(getLongValue(val47));
                list.add(mdmMonthProdPlan);
            }
        }
        return list;
    }

    public String getCellValue(Cell cell){
        Object val=null;
        if(cell==null){
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr=val+"";
            if(valStr.indexOf("E")>=0){
                BigDecimal realValue=  new BigDecimal(valStr);
                val=realValue.toPlainString();
            }
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            val = cell.getBooleanCellValue();
        }
        if(val==null){
            return null;
        }
        return val+"";
    }

    public String getStringValue(String val){
        String stringVal=null;
        if (val==null){
            return stringVal;
        }
        try{
            stringVal=val+"";
        }catch (Exception e){
            return null;
        }
        return stringVal;
    }

    public Long getLongValue(String val){
        Long longVal=null;
        if (val==null){
            return longVal;
        }
        try{
            if(val.endsWith(".0")){
                val=val.substring(0,val.indexOf(".0"));
            }
            longVal=Long.valueOf(val);
        }catch (Exception e){
            return null;
        }
        return longVal;
    }

    public BigDecimal getBigdecimalValue(String val){
        BigDecimal bigDecimalVal=null;
        if (val==null){
            return bigDecimalVal;
        }
        try{
            bigDecimalVal=new BigDecimal(val);
        }catch (Exception e){
            return null;
        }
        return bigDecimalVal;
    }

    public Date getDateValue(String mainPlanMonth,String val,int lastDay){
        Date dateVal=null;
        if (val==null){
            return null;
        }
        try{
            if(val.endsWith(".0")){
                val=val.substring(0,val.indexOf(".0"));
            }
            if(Integer.valueOf(val)>lastDay){
                val=lastDay+"";
            }
            String bd= mainPlanMonth+"-"+val;
            dateVal=DateUtils.parseDate(bd, DateUtils.YYYY_MM_DD);
        }catch (Exception e){
            return null;
        }
        return dateVal;
    }

    /**
     * 主计划明细类型转换，符合导入接口的要求
     * 同时要将空数值都转换成0，防止空指针错误
     *
     * @param plan
     * @return
     */
    private com.zlt.aps.common.engine.domain.MdmMonthProdPlan changeMdmMonthProdPlan(MdmMonthProdPlan plan) {
        com.zlt.aps.common.engine.domain.MdmMonthProdPlan prodPlan = new com.zlt.aps.common.engine.domain.MdmMonthProdPlan();
        BeanUtils.copyProperties(plan, prodPlan);
        // 去掉胎胚编号的前后空格
        prodPlan.setEmbryoCode(StringUtils.trim(prodPlan.getEmbryoCode()));
        prodPlan.setProductQty1(Optional.ofNullable(plan.getProductQty1()).orElse(0L).intValue());
        prodPlan.setProductQty2(Optional.ofNullable(plan.getProductQty2()).orElse(0L).intValue());
        prodPlan.setProductQty3(Optional.ofNullable(plan.getProductQty3()).orElse(0L).intValue());
        prodPlan.setProductQty4(Optional.ofNullable(plan.getProductQty4()).orElse(0L).intValue());
        prodPlan.setProductQty5(Optional.ofNullable(plan.getProductQty5()).orElse(0L).intValue());
        prodPlan.setProductQty6(Optional.ofNullable(plan.getProductQty6()).orElse(0L).intValue());
        prodPlan.setProductQty7(Optional.ofNullable(plan.getProductQty7()).orElse(0L).intValue());
        prodPlan.setProductQty8(Optional.ofNullable(plan.getProductQty8()).orElse(0L).intValue());
        prodPlan.setProductQty9(Optional.ofNullable(plan.getProductQty9()).orElse(0L).intValue());
        prodPlan.setProductQty10(Optional.ofNullable(plan.getProductQty10()).orElse(0L).intValue());
        prodPlan.setProductQty11(Optional.ofNullable(plan.getProductQty11()).orElse(0L).intValue());
        prodPlan.setProductQty12(Optional.ofNullable(plan.getProductQty12()).orElse(0L).intValue());
        prodPlan.setProductQty13(Optional.ofNullable(plan.getProductQty13()).orElse(0L).intValue());
        prodPlan.setProductQty14(Optional.ofNullable(plan.getProductQty14()).orElse(0L).intValue());
        prodPlan.setProductQty15(Optional.ofNullable(plan.getProductQty15()).orElse(0L).intValue());
        prodPlan.setProductQty16(Optional.ofNullable(plan.getProductQty16()).orElse(0L).intValue());
        prodPlan.setProductQty17(Optional.ofNullable(plan.getProductQty17()).orElse(0L).intValue());
        prodPlan.setProductQty18(Optional.ofNullable(plan.getProductQty18()).orElse(0L).intValue());
        prodPlan.setProductQty19(Optional.ofNullable(plan.getProductQty19()).orElse(0L).intValue());
        prodPlan.setProductQty20(Optional.ofNullable(plan.getProductQty20()).orElse(0L).intValue());
        prodPlan.setProductQty21(Optional.ofNullable(plan.getProductQty21()).orElse(0L).intValue());
        prodPlan.setProductQty22(Optional.ofNullable(plan.getProductQty22()).orElse(0L).intValue());
        prodPlan.setProductQty23(Optional.ofNullable(plan.getProductQty23()).orElse(0L).intValue());
        prodPlan.setProductQty24(Optional.ofNullable(plan.getProductQty24()).orElse(0L).intValue());
        prodPlan.setProductQty25(Optional.ofNullable(plan.getProductQty25()).orElse(0L).intValue());
        prodPlan.setProductQty26(Optional.ofNullable(plan.getProductQty26()).orElse(0L).intValue());
        prodPlan.setProductQty27(Optional.ofNullable(plan.getProductQty27()).orElse(0L).intValue());
        prodPlan.setProductQty28(Optional.ofNullable(plan.getProductQty28()).orElse(0L).intValue());
        prodPlan.setProductQty29(Optional.ofNullable(plan.getProductQty29()).orElse(0L).intValue());
        prodPlan.setProductQty30(Optional.ofNullable(plan.getProductQty30()).orElse(0L).intValue());
        prodPlan.setProductQty31(Optional.ofNullable(plan.getProductQty31()).orElse(0L).intValue());
        prodPlan.setSpecDimension(Optional.ofNullable(plan.getSpecDimension()).orElse(new BigDecimal(0)).doubleValue());
        prodPlan.setExpectedExcessArrears(Optional.ofNullable(plan.getExpectedExcessArrears()).orElse(0L));
        prodPlan.setTheoryProductionPlan(Optional.ofNullable(plan.getTheoryProductionPlan()).orElse(0L));
        prodPlan.setActualArrangement(Optional.ofNullable(plan.getActualArrangement()).orElse(0L));
        prodPlan.setPlanModifyQty(Optional.ofNullable(plan.getPlanModifyQty()).orElse(0L));
        prodPlan.setBalance(Optional.ofNullable(plan.getBalance()).orElse(0L));
        return prodPlan;
    }

    /**
     * 预计超欠产导出
     */
    public List<CxMdmMonthProdPlan1> expectedExport(MdmMonthProdPlan mdmMonthProdPlan) {
        return mdmMonthProdPlanMapper.expectedExport(mdmMonthProdPlan);
    }

    /**
     * 超欠产导出
     */
    public List<CxMdmMonthProdPlan2> overProdExport(MdmMonthProdPlan mdmMonthProdPlan) {
        return mdmMonthProdPlanMapper.overProdExport(mdmMonthProdPlan);
    }

    /**
     * 下发主计划
     */
    @Transactional
    public AjaxResult issuePlan(MdmMonthProdPlan mdmMonthProdPlan, Map<String, String> map) {
        List<MdmMonthProdPlan> list = mdmMonthProdPlanMapper.issuePlan(mdmMonthProdPlan);
        //删除本月度主计划
        CxEstimateExceedShort cxEstimateExceedShort = new CxEstimateExceedShort();
        cxEstimateExceedShort.setYear(Long.valueOf(mdmMonthProdPlan.getYear()));
        cxEstimateExceedShort.setMonth(Long.valueOf(mdmMonthProdPlan.getMonth()));
        cxEstimateExceedShortMapper.deleteByMonth(cxEstimateExceedShort);
        for (MdmMonthProdPlan item : list) {
            CxEstimateExceedShort entity = new CxEstimateExceedShort();
            entity.setYear(item.getYear() == null ? null : Long.valueOf(item.getYear()));
            entity.setMonth(item.getMonth() == null ? null : Long.valueOf(item.getMonth()));
            entity.setProductCode(item.getMaterialCode());
            entity.setLv(StringUtils.isBlank(item.getQualityGrade()) || map.get(item.getQualityGrade()) == null ? "" : map.get(item.getQualityGrade()).substring(0, 1));
            entity.setLevelCode(item.getQualityGrade());
            entity.setStorTypeDesc(map.get(item.getStorageLocation()));
            entity.setStorType(item.getStorageLocation());
            entity.setExpectedExcessArrears(item.getExpectedExcessArrears());
            cxEstimateExceedShortMapper.insertCxEstimateExceedShort(entity);
        }
        return AjaxResult.success();
    }

    /**
     * 查询月计划甘特图数据
     */
    public List<Gante> getMonthPlanGanteData(Gante gante) {
        //机台甘特图
        List<Gante> newGanteList=new ArrayList<>();
        List<Gante> ganteList = mdmMonthProdPlanMapper.getMonthPlanGanteData(gante);
        int scheduleMonth = DateUtils.getMonth(gante.getScheduleDate());
        //构造开始日、结束日
        if (CollectionUtils.isNotEmpty(ganteList)) {
            for (Gante item : ganteList) {
                //判断是否跨月
                int startMonth = DateUtils.getMonth(item.getStartDate());
                int endMonth = DateUtils.getMonth(item.getEndDate());
                if (startMonth != scheduleMonth && endMonth != scheduleMonth) {
                    continue;
                }
                //构造开始日、结束日
                String startDay = DateUtils.getDay(item.getStartDate())+"";
                String endDay = DateUtils.getDay(item.getEndDate())+"";
                item.setStartDay(startDay);
                item.setEndDay(endDay);

                //判断是否跨月
                if (startMonth != endMonth && endMonth == scheduleMonth) { //月初跨月
                    item.setStartDay("1");
                }
                if (startMonth != endMonth && startMonth == scheduleMonth) {  //月末跨月
                    item.setEndDay(DateUtils.getLastDay(item.getStartDate()));
                }
                newGanteList.add(item);
            }
        }
        return newGanteList;
    }

    /**
     * 查询月计划柱状图数据
     */
    public Map<String,List<Integer>> dailyChart(String scheduleDate){
        Map<String,List<Integer>> map=new HashMap<>();
        MdmMonthProdPlan query=new MdmMonthProdPlan();
        query.setMonth(scheduleDate);
        List<MdmMonthProdPlan> dailyChartList = mdmMonthProdPlanMapper.dailyChart(query);
        List<Integer> dayList=new ArrayList<>();
        List<Integer> moldsList=new ArrayList<>();
        List<Integer> productionList=new ArrayList<>();
        for (MdmMonthProdPlan item : dailyChartList) {
            dayList.add(DateUtils.getDay(item.getBeginDate()));
            moldsList.add(item.getActualArrangement().intValue());
            productionList.add(item.getTheoryProductionPlan().intValue());
        }
        map.put("date",dayList);
        map.put("molds",moldsList);
        map.put("production",productionList);
        return map;
    }


}
