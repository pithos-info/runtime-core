/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.runtime.core.context;

import java.util.Properties;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.runtime.model.config.Config.Configs;

/**
 * @author svarma
 *
 *         Jun 6, 2021
 *
 */
public class ServiceConfigs {

	private final ConfigMap configMap;
	private final String serviceName;

	/**
	 * @param configMap
	 */
	public ServiceConfigs(ConfigMap configMap) {
		if (configMap == null) {
			throw new IllegalArgumentException("null configMap");
		}

		this.configMap = configMap;
		this.serviceName = this.configMap.getBootstrapConfigs().getServiceName();
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public String getConfig(String key, String defaultVal) {
		if (this.configMap.getConfigsMap().get(this.serviceName).getConfigsMap().containsKey(key)) {
			return this.configMap.getConfigsMap().get(this.serviceName).getConfigsMap().get(key);
		}

		return defaultVal;
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public int getIntConfig(String key, int defaultVal) {
		if (this.configMap.getConfigsMap().get(this.serviceName).getIntConfigsMap().containsKey(key)) {
			return this.configMap.getConfigsMap().get(this.serviceName).getIntConfigsMap().get(key);
		}

		return defaultVal;
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public boolean getBoolConfig(String key, boolean defaultVal) {
		if (this.configMap.getConfigsMap().get(this.serviceName).getBoolConfigsMap().containsKey(key)) {
			return this.configMap.getConfigsMap().get(this.serviceName).getBoolConfigsMap().get(key);
		}

		return defaultVal;
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public long getLongConfig(String key, long defaultVal) {
		if (this.configMap.getConfigsMap().get(this.serviceName).getLongConfigsMap().containsKey(key)) {
			return this.configMap.getConfigsMap().get(this.serviceName).getLongConfigsMap().get(key);
		}

		return defaultVal;
	}

	/**
	 * @param props
	 * @return
	 */
	private Configs parse(Properties props) {
		Configs.Builder bldr = Configs.newBuilder();

		for (String key : props.stringPropertyNames()) {
			if (key.equals("multiplier")) {
				bldr.putIntConfigs(key, new Integer(props.getProperty(key)));
			} else if (key.equals("port")) {
				bldr.putIntConfigs(key, new Integer(props.getProperty(key)));
			} else {
				bldr.putConfigs(key, props.getProperty(key));
			}
		}

		return bldr.build();
	}

}
