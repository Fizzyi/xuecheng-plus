# 学成在线项目

## 资源

（1）百度网盘
链接：https://pan.baidu.com/s/16eYJLuhDA5YYespd3OmFSQ?pwd=uu91 提取码：uu91

（2）文献
链接：https://aigz8jy30yo.feishu.cn/docx/VGoudR1Y3oMA1Rxx6ABcjq8nnnb

## 知识点记录

1、Springboot读取配置文件的顺序

（1）项目启动加载bootstrap.yml文件,获取nacos地址，配置文件id，读取nacos中的配置文件

（2）读取本地配置文件application.yml，与nacos中的配置文件合并

2、xxl-job 任务调度

3、锁

单机：避免多线程去争抢同一个任务可以使用synchronized同步锁去解决。
分布式锁：

（1）基于数据库实现分布式锁：利用数据库主键唯一性、唯一索引、行级锁的特点实现。
比如：多个线程同时向数据库插入主键相同的同一条记录，谁插入成功谁就成功获取锁。
多个线程同时去更新相同的记录，谁更新成功谁就抢到锁。

（2）基于redis实现锁
redis提供了分布式锁的实现方案，比如：SETNX， set nx,redisson等。
SETNX 命令是去set一个不存在的key，多个线程去设置同一个key只会有一个线程设置成功，谁设置成功谁就拿到锁。

（3）使用zookeeper实现
zookeeper提供了分布式锁的实现方案，比如：/lock节点下创建临时节点，多个线程去创建临时节点，只有一个线程创建成功，谁创建成功谁就拿到锁。

什么是乐观锁、悲观锁？

synchronized是一种悲观锁，在执行被synchronized包裹的代码时需要首先获取锁，没有拿到锁则无法执行，是总悲观的认为别的线程会去抢，所以要悲观锁。（可以理解为先select 在 update）

乐观锁的思想是它不认为会有线程去争抢，尽管去执行，如果没有执行成功就再去重试。（可以理解为直接执行update）

4、分布式事务技术方案
一次操作需要向多个数据源写入数据，需要保证数据的一致性，不能出现部分成功的情况。

cap理论是指在分布式系统中，一致性（Consistency）、可用性（Availability）、分区容错性（Partition tolerance）三个指标，最多只能同时满足两个。

（1）一致性：在分布式系统中，所有节点的数据状态必须保持一致。
（2）可用性：在分布式系统中，所有节点都必须是可用的。
（3）分区容错性：在分布式系统中，节点之间可能会发生故障，需要保证系统的可用性。

在分布式系统中进行分布式事务控制，要么保证cp（强一致性），要么保证ap（最终数据一致性）。


5、es

安装analysis-ik中文分词插件
""""
bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.17.0/elasticsearch-analysis-ik-8.17.0.zip