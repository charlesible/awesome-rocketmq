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



[0]: https://leran2deeplearnjavawebtech.oss-cn-beijing.aliyuncs.com/learn/RocketMQ%E6%8A%80%E6%9C%AF%E5%86%85%E5%B9%95/4_1.png

