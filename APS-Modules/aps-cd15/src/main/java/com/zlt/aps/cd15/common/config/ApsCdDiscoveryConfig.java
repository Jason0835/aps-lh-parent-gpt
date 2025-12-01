package com.zlt.aps.cd15.common.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.ruoyi.common.core.utils.ip.IpUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

@Configuration
public class ApsCdDiscoveryConfig implements SmartInitializingSingleton, ApplicationListener<ContextClosedEvent> {
	@Value("${server.port:9005}")
	Integer prot;

	@Autowired
	ApplicationContext context;

	@Autowired
	NacosServiceRegistry registry;

//	NacosRegistration cd15Registration;
	NacosRegistration cd90Registration;
	NacosRegistration tqRegistration;
	NacosRegistration gsqRegistration;
	NacosRegistration ncRegistration;

	@Override
	public void afterSingletonsInstantiated() {
		// 注册15度裁断服务
//		cd15Registration = this.registService("aps-cd15");
//		registry.register(cd15Registration);
		// 注册90度裁断服务
		cd90Registration = this.registService("aps-cd90");
		registry.register(cd90Registration);
		// 注册胎圈服务
		tqRegistration = this.registService("aps-tq");
		registry.register(tqRegistration);
		// 注册钢丝圈服务
		gsqRegistration = this.registService("aps-gsq");
		registry.register(gsqRegistration);
		// 注册内衬服务
		ncRegistration = this.registService("aps-nc");
		registry.register(ncRegistration);
	}

	/**
	 * 将当前服务注册为指定服务名
	 * 
	 * @param serviceName
	 * @return
	 */
	private NacosRegistration registService(String serviceName) {
		NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
		properties.setNamespace(serviceName);
		properties.setPort(prot);
		properties.setIp(IpUtils.getHostIp());
		properties.setServerAddr(serviceName);
		properties.setService(serviceName);
		NacosRegistration registration = new NacosRegistration(null, properties, context);
		registration.setPort(prot);
		return registration;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
//		registry.deregister(cd15Registration);
		registry.deregister(cd90Registration);
		registry.deregister(tqRegistration);
		registry.deregister(gsqRegistration);
		registry.deregister(ncRegistration);
	}
}