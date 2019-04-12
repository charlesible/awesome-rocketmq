注释格式：`注释2.3.3`

1. broker：broker 模块
2. client：消息客户端，包含消息生产者、消息消费者相关类
3. common：公共包
4. dev：开发者信息（非源代码 ）
5. distribution：部署实例文件夹
6. example：示例代码
7. filter：消息过滤相关基础类
8. filtersrv：消息过滤服务器实现相关类
11. openmessaging：消息开放标准
12. remoting：远程通信模块，基于 Netty
13. srvutil：服务器工具类
14. store：消息存储实现相关类
15. style：checkstyle 相关实现
16. test：测试相关类
17. tools：工具类，监控命令相关实现类

### 1.3.1 设计理念

基于主题的发布与订阅模式：消息发送、消息存储、消息消费  
NameServer 实现元数据的管理（Topic路由信息等），但集群之间互不通信  

# 2 路由中心 NameServer

## 2.1 架构设计

NameServer 互相之间不通信，Broker 消息服务器在启动时向所有的 NameServer 注册，Producer
在发送消息之前从 NameServer 获取 Broker 服务器地址列表，然后根据负载算法选择一台进行发送，
NameServer 与每台 Broker 保持长连接，30s 检测 Broker 是否存活，如果检测到 Broker 宕机，
从路由注册表中移除，但不会马上通知 Producer，为了降低 NameServer 的复杂度，让 Producer 
的容错机制保证消息发送的高可用性

## 2.3 路由注册、故障剔除

### 2.3.1 路由元信息

QueueData：在 2M-2S 中，每个 M-S 的每个 Topic 有 4个读队列和4个写队列  
BrokerData  
BrokerLiveInfo

### 2.3.2 路由注册

Broker 基于定时线程池组装请求信息，遍历 NameServer 发送，多个 Broker 心跳包
NameServer 基于读写锁串行更新 Broker 信息，但是读取 Broker 表信息是并发读，标准
的 ReadWriteLock

### 2.3.3 路由删除

被动：NameServer 的定时任务线程会 10s 扫描一次 Broker 元信息表，查看 BrokerLive 的
lastUpdateTimestamp 并与当前时间戳对比，超过 120s 进行移除操作，并更新对应的其它
元信息表  
主动：Broker 正常关闭，发送 unregisterBroker 指令删除

### 2.3.4 路由发现

非实时，即 NameServer 不主动推送最新的路由，而是由客户端主动定时通过特殊的某个主题去拉
取最新的路由

# 3 消息发送

可靠消息发送、可靠异步发送、单向（oneway）发送

## 3.1 简略消息发送

同步(sync)：发送消息的API是阻塞的，直到消息服务器返回  
异步(async)：发送消息的API是异步主线程不阻塞，通过回调来获取发送结果   
单向(oneway)：发送消息的API直接返回，也没回调函数，只管发

## 3.2 Message 类

扩展都存放到 map，包括 tag keys 等

## 3.3 生产者启动流程

### 3.3.1 DefaultMQProducer

### 3.3.2 生产者启动流程

## 3.4 消息发送基本流程

验证消息、查找路由、消息发送 

### 3.4.1 消息长度验证

### 3.4.2 查找主题路由信息

### 3.4.3 选择消息队列

根据 ThreadLocal + random + volatile：对消息队列轮询查找，并对超过阈值 broker 没有
交互的直接剔除，避免轮询已经宕机的 broker 消息队列

### 3.4.4 消息发送

## 3.5 批量消息发送

使用 MessageBatch 类，在发送时使用 instanceof 判断是否是批量，从而进行操作  
压缩不支持批量

# 4 消息存储

## 4.1 存储概要

Commitlog、ConsumeQueue、IndexFile   
Commitlog：将所有主题的消息存储在同一个文件中，确保消息发送时顺序写文件，但是对消息主题检索消息
不友好  
因此额外增加了 ConsumeQueue 消息队列文件，每个消息主题包含多个消息消费队列，
每个消息队列有一个消息文件  
IndexFile 索引文件，加速消息检索，根据消息的属性快速从 Commitlog 检索消息  
![][0]

## 4.2 初识消息存储

## 4.3 消息发送存储流程

## 4.4 存储文件组织与内存映射

CommitLog、ConsumeQueue、IndexFile：单个文件固定长度，文件名为该文件第一条消息对应的全局物理偏移量

### 4.4.1 MappedFileQueue 映射文件队列

MappedFileQueue 是对存储目录的封装，即作为一个文件夹，下面包含多个文件

### 4.4.2 MappedFile 内存映射文件

勘误：第96页，不是 MappedFile 的 shutdown 而应该是 ReferenceResource 的 shutdown，
其中使用了引用计数关闭

### 4.4.3 TransientStorePool

短暂的存储池，用来临时存储数据，数据先写入该内存映射中，然后由 commit 线程定时将数据从该
内存复制到与目的物理文件对应的内存映射中  
引入该机制主要原因是提供一种内存锁定，将当前堆外内存一直锁定在内存中，避免被进程将内存交换到磁盘。

## 4.5 RocketMQ存储文件

commitlog：消息存储目录  
config：运行时配置  
consumerFilter.json：主题消息过滤信息  
consumerOffset.json：集群消费模式消息消费进度  
delayOffset.json：延时消息队列拉取进度  
subscriptionGroup.json：消息消费组配置信息  
topics.json:topic配置属性  
consumequeue：消息消费队列存储目录  
index：消息索引文件存储目录  
abort：如果存在，则说明 Broker 非正常关闭，启动时创建，正常退出前删除  
checkpoint：存储 commitlog 文件最后一次刷盘时间戳 、consumequeue 最后一次刷盘时间、index 索引文件最后一次刷盘时间戳

### 4.5.1 Commitlog文件

![][1]

### 4.5.2 ConsumeQueue文件

同一主题的消息不连续地存储在 commitlog 文件中，如果需要查找某个主题下的消息，只能通过遍历，
效率极低，因此设计了消息消费队列文件（ConsumeQueue），即作为 Commitlog 文件消息消费的"索引"
文件，consumequeue 的第一级目录为消息主题，第二级目录为主题的消息队列   

- Topic1   
-------- 0  
-------- 1  
-------- 2  
-------- 3  
- Topic2   
-------- 0  
-------- 1  
-------- 2  
-------- 3  

### 4.5.3 Index索引文件

![][2]

## 4.6 实时更新消息消费队列与索引文件

消息消费队列文件、消息属性属性文件都是基于 CommitLog 文件构建，当消息生产者提交的消息存储在 CommitLog 文件中，
ConsumeQueue、IndexFile 需要及时更新，否则消息无法及时被消费，而且查找消息也出现延迟   
RocketMQ 通过开启一个线程 ReputMessageService 来准实时转发 CommitLog 文件更新事件（事件通知）

### 4.6.1 根据消息更新 ConsumeQueue

写入到 mappedFile，默认异步落盘

### 4.6.2 根据消息更新 Index 索引文件

## 4.7 消息队列与索引文件恢复

问题：消息成功存储到 Commitlog，但是转发任务未执行，Broker 宕机，此时三个文件不一致   
启动时通过判断 abort 文件的存在从而判断是否是异常宕机，初始化文件的偏移量，进行文件恢复

### 4.7.1 Broker 正常停止文件恢复

### 4.7.2 Broker 异常停止文件恢复

## 4.8 文件刷盘机制

基于 JDK NIO 的 MappedByteBuffer，内存映射机制，先将消息追加到内存，然后根据刷盘策略
在不同的时间写入磁盘，如果是同步刷盘，消息追加到内存后，将同步调用 MappedByteBuffer.force(),
如果是异步刷盘，则追加到内存后立刻返回给消息发送端  
基于 Commitlog 文件刷盘机制分析，其它两个文件类似

### 4.8.1 Broker 同步刷盘

消息生产者在消息服务端将消息内容追加到内存映射文件后（内存），需要同步将内存的内容立刻
刷写到磁盘，其中是堆外内存保证零拷贝效率，MappedByteBuffer.force 保证写入

### 4.8.2 Broker 异步刷盘

1. 将消息直接追加到 ByteBuffer(Direct)，wrotePostion 随着消息追加而移动  
2. CommitRealTimeService 线程每200ms将Buffer新追加的内容的数据提交到MappedByteBuffer
3. FlushRealTimeService 线程每500ms将MappedByteBuffer中新追加的内存通过调用
MappedByteBuffer.force 写入到磁盘  

## 4.9 过期文件删除机制

超过72小时的两个文件，无论消息是否被消费，都被删除

## 4.10 总结

消息到达 commitlog 通过定时线程转发到 consumequeue/indexFile  
使用 abort 文件做异常宕机的记录

# 5 消息消费 

## 5.1 消息消费概述

集群模式、广播模式  
推模式、拉模式

## 5.2 消息消费者初探





[0]: https://leran2deeplearnjavawebtech.oss-cn-beijing.aliyuncs.com/learn/RocketMQ%E6%8A%80%E6%9C%AF%E5%86%85%E5%B9%95/4_1.png
[1]: https://leran2deeplearnjavawebtech.oss-cn-beijing.aliyuncs.com/learn/RocketMQ%E6%8A%80%E6%9C%AF%E5%86%85%E5%B9%95/4_2.png
[2]: https://leran2deeplearnjavawebtech.oss-cn-beijing.aliyuncs.com/learn/RocketMQ%E6%8A%80%E6%9C%AF%E5%86%85%E5%B9%95/4_3.png
[3]: https://leran2deeplearnjavawebtech.oss-cn-beijing.aliyuncs.com/learn/RocketMQ%E6%8A%80%E6%9C%AF%E5%86%85%E5%B9%95/4_4.png
