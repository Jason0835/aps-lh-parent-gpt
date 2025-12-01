package com.zlt.aps.xwyy.common.config;

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
public class ApsYyDiscoveryConfig implements SmartInitializingSingleton, ApplicationListener<ContextClosedEvent> {
	@Value("${server.port:9009}")
	Integer prot;

	@Autowired
	ApplicationContext context;

	@Autowired
	NacosServiceRegistry registry;

//	NacosRegistration xwyyRegistration;
	NacosRegistration gdyyRegistration;

	@Override
	public void afterSingletonsInstantiated() {
		// 注册纤维压延服务
//		xwyyRegistration = this.registService("aps-xwyy");
//		registry.register(xwyyRegistration);
		// 注册钢带压延服务
		gdyyRegistration = this.registService("aps-gdyy");
		registry.register(gdyyRegistration);
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
//		registry.deregister(xwyyRegistration);
		registry.deregister(gdyyRegistration);
	}
}