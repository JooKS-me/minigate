## Mini Gate - an api gateway based on netty

Mini Gate 是一个基于Netty实现的轻量级API网关。

### 已实现功能
- HTTP转发：支持GET、POST方法，POST方法的请求体支持`json`和`x-www-form-urlencoded`格式。
- 负载均衡：支持随机、轮询算法
- 权限验证：支持JWT权限验证

### 设计思路
Mini Gate分为`server`、`client`、`router`、`filter`四个主要部分。
其中，`server`负责请求的接收和返回；
`client`负责向真实服务发送请求和接收响应；
`router`负责路由；
`filter`负责对两个请求和两个响应进行过滤，可以实现特定功能（比如日志、链路追踪、度量、设置Header等等）

### 架构图
![架构图](./images/minigate.jpg)

### 压测
使用wrk工具，编写好lua脚本
```lua
wrk.method = "POST"
wrk.body = '{"id": "10001","name": "jooks"}'
wrk.headers["Content-Type"] = "application/json"
function request()
return wrk.format('POST', nil, nil, body)
end
```

首先不使用网关进行压测，` wrk -t1 -c20 -d30s --latency -s Desktop/test.lua http://localhost:8080/test `，得到如下结果：
```
Running 30s test @ http://localhost:8080/test
  1 threads and 20 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.27ms   14.25ms 270.24ms   99.21%
    Req/Sec    20.25k     2.30k   25.93k    75.17%
  Latency Distribution
     50%  833.00us
     75%    0.99ms
     90%    1.26ms
     99%   11.28ms
  603123 requests in 30.10s, 85.24MB read
Requests/sec:  20038.45
Transfer/sec:      2.83MB
```

然后使用网关，开启轮询负载均衡策略，不配置jwt过滤，进行压测，` wrk -t1 -c20 -d30s --latency -s Desktop/test.lua http://localhost:13307/test1 `，得到如下结果：
```
Running 30s test @ http://localhost:13307/test1
  1 threads and 20 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   393.62ms  318.01ms   1.87s    80.70%
    Req/Sec    75.23     64.55   600.00     90.86%
  Latency Distribution
     50%  309.67ms
     75%  502.86ms
     90%  747.97ms
     99%    1.62s
  1716 requests in 30.07s, 258.07KB read
Requests/sec:     57.07
Transfer/sec:      8.58KB
```
发现性能非常辣鸡。。。

然后在调用netty发起http请求那里使用线程池，再进行压测，` wrk -t1 -c20 -d30s --latency -s Desktop/test.lua http://localhost:13307/test1 `，得到结果如下：
```
Running 30s test @ http://localhost:13307/test1
  1 threads and 20 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    40.07ms   84.30ms 733.96ms   93.03%
    Req/Sec   831.94    498.15     1.94k    63.92%
  Latency Distribution
     50%   15.61ms
     75%   23.99ms
     90%   71.64ms
     99%  468.45ms
  8202 requests in 30.10s, 1.20MB read
Requests/sec:    272.48
Transfer/sec:     40.98KB
```
依旧辣鸡。。。

然后在netty发起http请求那里的NioEventLoopGroup加入ThreadFactory，然后把eventLoopGroup设置为静态，使其可以复用；再把前面的线程池核心数调整为8，再次进行压测：
```
Running 30s test @ http://localhost:13307/test1
  1 threads and 20 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     8.20ms   39.34ms 606.79ms   98.80%
    Req/Sec     3.38k     1.50k    5.15k    62.50%
  Latency Distribution
     50%    3.92ms
     75%    5.31ms
     90%    7.65ms
     99%   93.15ms
  8243 requests in 30.04s, 1.21MB read
Requests/sec:    274.39
Transfer/sec:     41.27KB
```

这就是个垃圾玩意儿。。。
