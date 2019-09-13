# txle | [English](README.md)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Gitter](https://img.shields.io/static/v1?label=chat&message=on&nbsp;gitter&color=brightgreen)](https://gitter.im/actiontech-txle/Lobby)

## 什么是txle？
txle是一款能够保证业务数据最终一致性的分布式事务框架。

## 特征
* 多种手段保证数据最终一致性。
* 高性能。QPS为5000/s左右，TPS为50000/s左右。
* 低侵入。最少2个注解即可。
* 支持Docker快速部署。
* 支持服务降级。发生不可抗拒因素时，也能保证主业务正常运行。
* 支持异常快照处理。
* 支持超时和重试机制。

## 历史
txle是基于[ServiceComb Pack](https://github.com/apache/servicecomb-pack)进行研发的。首先，我们要感谢ServiceComb Pack项目的所有贡献者。

对于txle而言，我们更加专注于金融领域，可适应诸多的复杂业务场景。另外，在稳定性和高性能方面有显著改善，尤其在性能上，我们提升了几倍的QPS。

## 架构

![txle业务集成架构](docs/txle-architecture-cn.png)

## 快速启动

### Step 1: 下载txle正式版本
[下载](https://github.com/actiontech/txle/releases)最新版本并解压。

```bash
# tar -xzf actiontech-txle-$version.tar.gz
# mv actiontech-txle-$version txle
# cd txle
```

### Step 2: 准备环境
* MySQL实例

    在部署txle服务的机器中启动一个MySQL实例，创建名为txle的数据库，用户为test，密码为123456。

* JVM

    txle是使用java开发的，所以需要在部署txle服务的机器上安装java 1.8或以上版本，并确保JAVA_HOME参数被正确的设置。

### Step 3: 启动txle服务

```bash
# ./txle start
Starting the txle server....
Start successfully!
```
### Step 4: 停止txle服务

```bash
# ./txle stop
Stopping the txle server....
Stop successfully!
```

## 官网

了解更多信息，欢迎访问[官网](https://opensource.actionsky.com/)。

## 贡献

我们欢迎并十分感谢您的贡献。有关提交补丁和贡献流程请参阅[CONTRIBUTION.md](https://github.com/actiontech/txle/docs/CONTRIBUTION.md)。

## 社区

* [![Gitter](https://badges.gitter.im/actiontech-txle/community.svg)](https://gitter.im/actiontech-txle/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
* QQ group: 696990638
* 开源社区微信公众号
  
  ![dble](./docs/QR_code.png)

## 联系我们

如果想获得txle的商业支持, 您可以联系我们:

- 全国支持: 400-820-6580
- 华北地区: 86-13718877200, 王先生
- 华南地区: 86-18503063188, 曹先生
- 华东地区: 86-18930110869, 梁先生
- 西南地区: 86-13540040119, 洪先生