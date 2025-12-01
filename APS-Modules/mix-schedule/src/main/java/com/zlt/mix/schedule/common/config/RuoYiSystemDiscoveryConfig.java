package com.zlt.mix.schedule.common.config;

// import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
// import com.alibaba.cloud.nacos.registry.NacosRegistration;
// import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
// import com.ruoyi.common.core.utils.ip.IpUtils;
// import org.springframework.beans.factory.SmartInitializingSingleton;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.ApplicationContext;
// import org.springframework.context.ApplicationListener;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.event.ContextClosedEvent;


// @Configuration
// public class RuoYiSystemDiscoveryConfig implements SmartInitializingSingleton, ApplicationListener<ContextClosedEvent> {
// 	@Value("${server.port:9104}")
// 	Integer port;
//
// 	@Autowired
// 	ApplicationContext context;
//
// 	@Autowired
// 	NacosServiceRegistry registry;
//
// 	NacosRegistration settingRegistration;
// 	NacosRegistration scheduleRegistration;
//
// 	@Override
// 	public void afterSingletonsInstantiated() {
// 		// 注册setting服务
// 		settingRegistration = this.registService("mix-setting");
// 		registry.register(settingRegistration);
// 		// 注册schedule服务
// 		scheduleRegistration = this.registService("mix-schedule");
// 		registry.register(scheduleRegistration);
// 	}
//
// 	/**
// 	 * 将当前服务注册为指定服务名
// 	 * 
// 	 * @param serviceName
// 	 * @return
// 	 */
// 	private NacosRegistration registService(String serviceName) {
// 		NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
// 		properties.setNamespace(serviceName);
// 		properties.setPort(port);
// 		properties.setIp(IpUtils.getHostIp());
// 		properties.setServerAddr(serviceName);
// 		properties.setService(serviceName);
// 		NacosRegistration registration = new NacosRegistration(null, properties, context);
// 		registration.setPort(port);
// 		return registration;
// 	}
//
// 	@Override
// 	public void onApplicationEvent(ContextClosedEvent event) {
// 		registry.deregister(settingRegistration);
// 		registry.deregister(scheduleRegistration);
// 	}
// }