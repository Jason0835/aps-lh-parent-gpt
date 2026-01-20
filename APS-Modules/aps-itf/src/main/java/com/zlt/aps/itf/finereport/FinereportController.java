package com.zlt.aps.itf.finereport;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.itf.finereport.vo.FinereportParams;
import com.zlt.aps.itf.util.PostMethodUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帆软报表接口
 *
 * @author zlt
 * @since 2025/12/19
 */
@Api(tags = "帆软报表接口")
@RestController
@RequestMapping("/finereport")
public class FinereportController {
    @Value("${finereport.url.login}")
    private String FINEREPORT_LOGIN_URL;
    @Value("${finereport.userName}")
    private String FINEREPORT_USER_NAME;
    @Value("${finereport.password}")
    private String FINEREPORT_PASSWORD;
    @Value("${finereport.url.inventoryAgeAnalysis}")
    private String INVENTORY_AGE_ANALYSIS_URL;
    @Value("${finereport.url.singleTireTotalWeight}")
    private String SINGLE_TIRE_TOTAL_WEIGHT_URL;
    @Value("${finereport.url.factoryMoldingMachine}")
    private String FACTORY_MOLDING_MACHINE_URL;
    @Value("${finereport.url.factoryVulcanizingMachine}")
    private String FACTORY_VULCANIZING_MACHINE_URL;
    @Value("${finereport.url.productionStructure}")
    private String PRODUCTION_STRUCTURE_URL;
    @Value("${finereport.url.productionYear}")
    private String PRODUCTION_YEAR_URL;
    @Value("${finereport.url.orderOffset}")
    private String ORDER_OFFSET_URL;
    @Value("${finereport.url.reportView}")
    private String REPORT_VIEW_URL;

    /**
     * 帆软报表预览公共页面
     *
     * @return 结果集合
     */
    @ApiOperation("报表预览公共页面")
    @PostMapping("/reportView")
    public AjaxResult reportView(FinereportParams params) {
        String rptUrl = REPORT_VIEW_URL + "/" + params.getReportCode();
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    @ApiOperation("库龄分析报表")
    @GetMapping("/inventoryAgeAnalysis")
    @ResponseBody
    public AjaxResult inventoryAgeAnalysis() {
        String rptUrl = INVENTORY_AGE_ANALYSIS_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 查询单胎总重报表
     *
     * @return 结果集合
     */
    @ApiOperation("查询单胎总重报表")
    @GetMapping("/singleTireTotalWeight")
    public AjaxResult singleTireTotalWeight() {
        String rptUrl = SINGLE_TIRE_TOTAL_WEIGHT_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 越南工厂成型机数据报表
     *
     * @return 结果集合
     */
    @ApiOperation("越南工厂成型机数据报表")
    @GetMapping("/factoryMoldingMachine")
    public AjaxResult factoryMoldingMachine() {
        String rptUrl = FACTORY_MOLDING_MACHINE_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 越南工厂硫化机数据报表
     *
     * @return 结果集合
     */
    @ApiOperation("越南工厂硫化机数据报表")
    @GetMapping("/factoryVulcanizingMachine")
    public AjaxResult factoryVulcanizingMachine() {
        String rptUrl = FACTORY_VULCANIZING_MACHINE_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 越南工厂结构在机数据报表
     *
     * @return 结果集合
     */
    @ApiOperation("越南工厂结构在机数据报表")
    @GetMapping("/productionStructure")
    public AjaxResult productionStructure() {
        String rptUrl = PRODUCTION_STRUCTURE_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 越南工厂年度产量报表
     *
     * @return 结果集合
     */
    @ApiOperation("越南工厂年度产量报表")
    @GetMapping("/productionYear")
    public AjaxResult productionYear() {
        String rptUrl = PRODUCTION_YEAR_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 越南工厂结构在机数据报表
     *
     * @return 结果集合
     */
    @ApiOperation("订单冲减分配报表")
    @GetMapping("/orderOffset")
    public AjaxResult orderOffset() {
        String rptUrl = ORDER_OFFSET_URL;
        // 模拟登录帆软服务，获取token
        AjaxResult loginResult = this.loginFinereport();
        if (AjaxResultUtils.checkAjaxError(loginResult)) {
            return loginResult;
        }
        // 拼接报表url
        String token = String.valueOf(loginResult.get(AjaxResult.DATA_TAG));
        Object realUrl = StringUtils.join(rptUrl, "?preview=true&fine_auth_token=", token); // 拼接url，需要使用Object接收，ajaxResult才会放到data里
        return AjaxResult.success(realUrl);
    }

    /**
     * 登录帆软服务器获取token
     *
     * @return
     */
    private AjaxResult loginFinereport() {
        JSONObject loginParams = new JSONObject();
        loginParams.put("username", FINEREPORT_USER_NAME);
        loginParams.put("password", FINEREPORT_PASSWORD);
        String result = PostMethodUtils.sendPost(FINEREPORT_LOGIN_URL, loginParams.toJSONString(), null);
        // 校验数据格式是否合法
        if (StringUtils.isEmpty(result) || !JSONValidator.from(result).validate()) {
            String errorMsg = "loginFinereport 返回数据格式校验失败：" + result;
            return AjaxResult.error(errorMsg);
        }
        JSONObject ajaxResult = JSONObject.parseObject(result);
        JSONObject data = ajaxResult.getJSONObject("data");
        if (data != null) {
            return AjaxResult.success(data.get("accessToken"));
        }
        String errorMsg = "loginFinereport 返回数据格式校验失败：" + result;
        return AjaxResult.error(errorMsg);
    }
}
