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
    Latency     2.00ms   11.36ms 228.27ms   98.16%
    Req/Sec    20.71k     2.64k   28.74k    82.21%
  Latency Distribution
     50%  830.00us
     75%    0.98ms
     90%    1.18ms
     99%   21.59ms
  616316 requests in 30.01s, 87.10MB read
Requests/sec:  20535.33
Transfer/sec:      2.90MB
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
TODO：性能优化