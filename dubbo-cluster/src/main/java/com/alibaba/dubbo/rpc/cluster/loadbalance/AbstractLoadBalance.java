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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

/**
 * AbstractLoadBalance
 * 
 * @author william.liangf
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (invokers == null || invokers.size() == 0)
            return null;
        if (invokers.size() == 1)
            return invokers.get(0);
        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    /**
     * 获取调用者权重
     * @param invoker
     * @param invocation
     * @return
     */
    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
        if (weight > 0) {
            //服务启动时间
	        long timestamp = invoker.getUrl().getParameter(Constants.TIMESTAMP_KEY, 0L);
	    	if (timestamp > 0L) {
                //服务已运行时长
	    		int uptime = (int) (System.currentTimeMillis() - timestamp);
                //服务热身时长，热身时长内，服务的权重低，分的流量少，以此打到热身目的
	    		int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                //还在热身范围内
	    		if (uptime > 0 && uptime < warmup) {
                    //根据热身时长计算权重
	    			weight = calculateWarmupWeight(uptime, warmup, weight);
	    		}
	    	}
        }
    	return weight;
    }

    /**
     * 根据热身时长计算权重
     * （启动时间×权重）/热身时间
     * @param uptime
     * @param warmup
     * @param weight
     * @return
     */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        //（启动时间×权重）/热身时间
    	int ww = (int) ( (float) uptime / ( (float) warmup / (float) weight ) );
    	return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

}