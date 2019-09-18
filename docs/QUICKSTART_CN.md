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