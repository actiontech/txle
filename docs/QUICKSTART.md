## Quick Start
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