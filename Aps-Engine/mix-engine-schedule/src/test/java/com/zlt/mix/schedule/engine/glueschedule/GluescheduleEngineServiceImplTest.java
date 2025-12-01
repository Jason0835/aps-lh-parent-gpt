package com.zlt.mix.schedule.engine.glueschedule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.rpc.ServiceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleEngineMapper;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineService;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;

@SpringBootTest
public class GluescheduleEngineServiceImplTest {
	@Autowired
	private GlueScheduleEngineService glueScheduleEngineService;
	@Autowired
	private GlueScheduleEngineMapper glueScheduleEngineMapper;

//	@Test
	public void autoPlan() {
//		glueScheduleEngineService.autoGlueSchedule(DateUtils.dateTime("yyyyMMdd", "20220630"), "M2");
		glueScheduleEngineService.autoGlueSchedule(DateUtils.dateTime("yyyyMMdd", "20220624"), "M4");
//		glueScheduleEngineService.autoGlueSchedule(DateUtils.dateTime("yyyyMMdd", "20220624"), "M2");
	}
	


//	@Test
	public void recaculate() {
		GlueScheduleResultVo params = new GlueScheduleResultVo();
		params.setScheduleDate(DateUtils.dateTime("yyyyMMdd", "20220624"));
		params.setGlue("HF627/31");
//		params.setIdList(Arrays.asList(new Long[] {549L}));
//		params.setScheduleDate();
		List<GlueScheduleResultVo> list = glueScheduleEngineMapper.selectScheduleResult(params);
		GlueScheduleResultVo result = list.get(0);
		result.setMidPlanQty(0D);
//		result.setMidProduceOrder(3);
//		glueScheduleEngineService.recaculateExpectTime(result);
	}
	
//	@Test
	public void inserOrder() {
		GlueScheduleResult scheduleResult = new GlueScheduleResultVo();
		scheduleResult.setScheduleDate(DateUtils.dateTime("yyyyMMdd", "20220705"));
		scheduleResult.setMixArea("M2");
		scheduleResult.setMachineCode("01013");
		scheduleResult.setGlue("HA610");
		scheduleResult.setRecipeType("281");
		scheduleResult.setMidPlanQty(22D);
		scheduleResult.setMidProduceOrder(20);
		scheduleResult.setNightPlanQty(33D);
		scheduleResult.setNightProduceOrder(30);
		scheduleResult.setDayPlanQty(44D);
		scheduleResult.setDayProduceOrder(40);
		scheduleResult.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_ADD);
//		glueScheduleEngineService.insertOrder(scheduleResult);
	}

//	@Test
	public void changeMachine() {
		GlueScheduleResultVo params = new GlueScheduleResultVo();
		params.setScheduleDate(DateUtils.dateTime("yyyyMMdd", "20220624"));
		params.setGlue("HF627/31");
		List<GlueScheduleResultVo> list = glueScheduleEngineMapper.selectScheduleResult(params);
		List<GlueScheduleResult> resultList = list.stream().map(r -> {
			GlueScheduleResult newR = new GlueScheduleResult();
			BeanUtils.copyProperties(r, newR);
			newR.setMachineCode("01026");
			return newR;
		}).collect(Collectors.toList());
//		glueScheduleEngineService.changeMachine(resultList);
	}

//    @Test
	public void testPublish() throws RemoteException, ServiceException {
		List<Long> ids = new ArrayList<>();
//		ids.add(411L);
		ids.add(549L);
//		glueScheduleEngineService.publishToMes(ids);
	}

//	@Test
//	public void testWebService() throws RemoteException, ServiceException {
//		OutputStream os = null;
//		InputStream is = null;
//		HttpURLConnection conn = null;
//		try {
//			URL wsUrl = new URL("http://192.168.1.137:8018/APSServiceTest/Service.asmx");
//			conn = (HttpURLConnection) wsUrl.openConnection();
//			conn.setDoInput(true);
//			conn.setDoOutput(true);
//			conn.setRequestMethod("POST");
//			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
//			conn.setConnectTimeout(2000);
//			conn.setReadTimeout(2000);
//			os = conn.getOutputStream();
//
//			String soap = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://tempuri.org/\">\r\n"
//					+ "   <soapenv:Header/>\r\n" + "   <soapenv:Body>\r\n" + "      <tem:APSPlanInfo>\r\n"
//					+ "         <!--Optional:-->\r\n" + "         <tem:PlanDate>2022-07-01</tem:PlanDate>\r\n"
//					+ "         <tem:shiftID>1</tem:shiftID>\r\n" + "         <!--Optional:-->\r\n"
//					+ "         <tem:EquipCode>01026</tem:EquipCode>\r\n" + "         <tem:PlanNum>20</tem:PlanNum>\r\n"
//					+ "         <!--Optional:-->\r\n" + "         <tem:MaterCode>4010000040437</tem:MaterCode>\r\n"
//					+ "         <tem:RecipeType>1</tem:RecipeType>\r\n" + "         <tem:EdtCode>1</tem:EdtCode>\r\n"
//					+ "         <tem:Orderid>10</tem:Orderid>\r\n" + "         <!--Optional:-->\r\n"
//					+ "         <tem:APSCode>GLUEM4202206240110002</tem:APSCode>\r\n" + "      </tem:APSPlanInfo>\r\n"
//					+ "   </soapenv:Body>\r\n" + "</soapenv:Envelope>";
//			os.write(soap.getBytes());
//			is = conn.getInputStream();
//
//			byte[] b = new byte[1024];
//			int len = 0;
//			String s = "";
//			while ((len = is.read(b)) != -1) {
//				String ss = new String(b, 0, len, "UTF-8");
//				s += ss;
//			}
//			String result = s.split("<response xsi:type=\"xsd:string\">")[1].split("</response>")[0];
//
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			try {
//				if (is != null) {
//					is.close();
//				}
//				if (os != null) {
//					os.close();
//				}
//				if (conn != null) {
//					conn.disconnect();
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//	}
}
