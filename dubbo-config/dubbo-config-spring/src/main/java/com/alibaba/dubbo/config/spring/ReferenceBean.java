/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.config.support.Parameter;

/**
 * ReferenceFactoryBean
 * 
 * @author william.liangf
 * @export
 */
public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {

	private static final long serialVersionUID = 213195494150089726L;
	
	private transient ApplicationContext applicationContext;

	public ReferenceBean() {
        super();
    }

    public ReferenceBean(Reference reference) {
        super(reference);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		SpringExtensionFactory.addApplicationContext(applicationContext);
	}
    
    public Object getObject() throws Exception {
        return get();
    }

    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Parameter(excluded = true)
    public boolean isSingleton() {
        return true;
    }

    @SuppressWarnings({ "unchecked"})
    public void afterPropertiesSet() throws Exception {
        //如果Consumer还未注册
        if (getConsumer() == null) {
            //获取applicationContext这个IOC容器实例中的所有ConsumerConfig
            Map<String, ConsumerConfig> consumerConfigMap = applicationContext == null ? null  : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class, false, false);
            if (consumerConfigMap != null && consumerConfigMap.size() > 0) {
                ConsumerConfig consumerConfig = null;
                //遍历consumerConfigMap
                for (ConsumerConfig config : consumerConfigMap.values()) {
                    //如果是默认配置
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        //已经存在consumerConfig 配置
                        if (consumerConfig != null) {
                            throw new IllegalStateException("Duplicate consumer configs: " + consumerConfig + " and " + config);
                        }
                        //设置当前配置为consumerConfig
                        consumerConfig = config;
                    }
                }
                if (consumerConfig != null) {
                    setConsumer(consumerConfig);
                }
            }
        }
        //应用名称没有初始化，<dubbo:application name="dubbo-test-consumer" /> 或者<dubbo:consumer default="true" application="dd"/> 没有配置
        if (getApplication() == null
                && (getConsumer() == null || getConsumer().getApplication() == null)) {
            //获取applicationContext所有的ApplicationConfig实例
            Map<String, ApplicationConfig> applicationConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class, false, false);
            //
            if (applicationConfigMap != null && applicationConfigMap.size() > 0) {
                ApplicationConfig applicationConfig = null;
                //遍历ApplicationConfig
                for (ApplicationConfig config : applicationConfigMap.values()) {
                    //是默认的，则设置为applicationConfig
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (applicationConfig != null) {
                            throw new IllegalStateException("Duplicate application configs: " + applicationConfig + " and " + config);
                        }
                        applicationConfig = config;
                    }
                }
                //applicationConfig为非空的，该校验使用ReferenceConfig.init();在做的
                if (applicationConfig != null) {
                    setApplication(applicationConfig);
                }
            }
        }
        //初始化module，如同上边application
        if (getModule() == null
                && (getConsumer() == null || getConsumer().getModule() == null)) {
            Map<String, ModuleConfig> moduleConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class, false, false);
            if (moduleConfigMap != null && moduleConfigMap.size() > 0) {
                ModuleConfig moduleConfig = null;
                for (ModuleConfig config : moduleConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (moduleConfig != null) {
                            throw new IllegalStateException("Duplicate module configs: " + moduleConfig + " and " + config);
                        }
                        moduleConfig = config;
                    }
                }
                if (moduleConfig != null) {
                    setModule(moduleConfig);
                }
            }
        }

        //初始化registries，如同上边application
        if ((getRegistries() == null || getRegistries().size() == 0)
                && (getConsumer() == null || getConsumer().getRegistries() == null || getConsumer().getRegistries().size() == 0)
                && (getApplication() == null || getApplication().getRegistries() == null || getApplication().getRegistries().size() == 0)) {
            Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (registryConfigMap != null && registryConfigMap.size() > 0) {
                List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                for (RegistryConfig config : registryConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        registryConfigs.add(config);
                    }
                }
                if (registryConfigs != null && registryConfigs.size() > 0) {
                    super.setRegistries(registryConfigs);
                }
            }
        }

        //初始化monitor，如同上边application
        if (getMonitor() == null
                && (getConsumer() == null || getConsumer().getMonitor() == null)
                && (getApplication() == null || getApplication().getMonitor() == null)) {
            Map<String, MonitorConfig> monitorConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                MonitorConfig monitorConfig = null;
                for (MonitorConfig config : monitorConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (monitorConfig != null) {
                            throw new IllegalStateException("Duplicate monitor configs: " + monitorConfig + " and " + config);
                        }
                        monitorConfig = config;
                    }
                }
                if (monitorConfig != null) {
                    setMonitor(monitorConfig);
                }
            }
        }
        Boolean b = isInit();
        if (b == null && getConsumer() != null) {
            b = getConsumer().isInit();
        }
        //需要初始化
        if (b != null && b.booleanValue()) {
            //手动调用，获取bean，进行bean初始化
            getObject();
        }
    }

}