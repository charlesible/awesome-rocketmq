/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.client.consumer.store;

import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * Offset store interface\s
 * 注释5.6.3：消息消费进度接口
 */
public interface OffsetStore {
    /**
     * Load
     * 注释5.6.3：从消息进度存储文件加载消息进度到内存
     */
    void load() throws MQClientException;

    /**
     * Update the offset,store it in memory
     * 注释5.6.3：更新内存中的消息消费进度
     */
    void updateOffset(final MessageQueue mq, final long offset, final boolean increaseOnly);

    /**
     * Get offset from local storage
     * 注释5.6.3：读取消息消费进度
     * @return The fetched offset
     */
    long readOffset(final MessageQueue mq, final ReadOffsetType type);

    /**
     * Persist all offsets,may be in local storage or remote name server
     * 注释5.6.3：持久化指定消息队列进度到磁盘
     */
    void persistAll(final Set<MessageQueue> mqs);

    /**
     * Persist the offset,may be in local storage or remote name server
     */
    void persist(final MessageQueue mq);

    /**
     * Remove offset
     * 注释5.6.3：将消息队列的消息消费进度从内存中移除
     */
    void removeOffset(MessageQueue mq);

    /**
     * @return The cloned offset table of given topic
     * 注释5.6.3：克隆该主题下所有消息队列的消息消费进度
     */
    Map<MessageQueue, Long> cloneOffsetTable(String topic);

    /**
     * 注释5.6.3：更新存储在 Broker端的消息消费进度，使用集群模式
     * @param mq
     * @param offset
     * @param isOneway
     */
    void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException,
        MQBrokerException, InterruptedException, MQClientException;
}
