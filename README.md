# txle | [中文](README_ZH.md)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Gitter](https://img.shields.io/static/v1?label=chat&message=on&nbsp;gitter&color=brightgreen)](https://gitter.im/actiontech-txle/Lobby)

## What is txle?
txle is a distributed transaction framework and can guarantee the final consistency of the business data.

## Feature
* Multiple ways to guarantee the final consistency of the business data.
* High performance. QPS is 5000 or so and TPS is 50000 or so.
* Low invasion. It can work by setting 2 annotations.
* Support quick start by Docker.
* Support service downgrading. No effect to main business in case of irresistible factors.
* Support for exception snapshot processing.
* Support both timeout and retry.

## History
txle is based on [ServiceComb Pack](https://github.com/apache/servicecomb-pack). First of all, thanks to the contributors from ServiceComb Pack.

For txle, more attention on financial field and it can work in a lot of complex business cases. Moreover, there were significant improvements on Stability and Performance, especially the QPS, it was increased for several times.

## Architecture

![txle architecture](docs/txle-architecture.png)

## Quick start
### Step 1: Download txle Release
[Download](https://github.com/actiontech/txle/releases) the release and un-tar it.

```bash
# tar -xzf actiontech-txle-$version.tar.gz
# mv actiontech-txle-$version txle
# cd txle
```
### Step 2: Preparation
* MySQL Instance

    Start a MySQL Instance in your machine where the txle service is deployed. And create a database called 'txle', a user called 'test' and the password is '123456'.

* JVM

    Install Java 1.8 or later in your machine and make sure the JAVA_HOME configuration is correct.

### Step 3: Start server
```bash
# ./txle start
Starting the txle server....
Start successfully!
```
### Step 4: Stop server

```bash
# ./txle stop
Stopping the txle server....
Stop successfully!
```

## Official website

For more information, please visit the [Official website](https://opensource.actionsky.com/).

## Contribution

Contributions are welcomed and greatly appreciated. See [CONTRIBUTION.md](https://github.com/actiontech/txle/docs/CONTRIBUTION.md) for details on submitting patches and the contribution workflow.

## Community

* [![Gitter](https://badges.gitter.im/actiontech-txle/community.svg)](https://gitter.im/actiontech-txle/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
* QQ group: 696990638
* wechat subscription QR code
  
  ![dble](./docs/QR_code.png)

## Contact us

txle has enterprise support plans, you may contact our sales team:

- Global Sales: 400-820-6580
- North China: 86-13718877200, Mr.Wang
- South China: 86-18503063188, Mr.Cao
- East China: 86-18930110869, Mr.Liang
- South-West China: 86-13540040119, Mr.Hong