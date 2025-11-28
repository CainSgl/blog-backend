的# gRPC代码生成器

**第一行输入格式**: `模块名 ServiceName`  
例如: `test TestService`

---

## 生成内容

根据已存在的Service接口(`com.cainsgl.common.service.{ServiceName}`)，生成3个文件：

### 1. Proto文件
**路径**: `common/src/main/proto/{serviceName}.proto`

```proto
syntax = "proto3";
package com.cainsgl.grpc;
option java_package = "com.cainsgl.grpc.api";
option java_outer_classname = "{ServiceName}Proto";
option java_multiple_files = true;

// 根据Service接口方法生成对应的message和rpc
// 每个方法生成: {MethodName}Request, {MethodName}Response, rpc方法
```

**规则**:
- 参数类型映射: String→string, Integer→int32, Long→int64, Boolean→bool
- 每个Service方法对应一个rpc方法
- Request/Response的字段序号从1开始

### 2. GrpcImpl实现类
**路径**: `{模块名}/src/main/java/com/cainsgl/{模块名}/api/{ServiceName}GrpcImpl.java`

```java
package com.cainsgl.{模块名}.api;

import com.cainsgl.common.service.{ServiceName};
import com.cainsgl.grpc.api.*; // proto生成的类
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import net.devh.boot.grpc.server.service.GrpcService;

// 这是grpc的具体实现类，跟proto文件一一对应
// 如果本地有实现，则会优先调用本地实现
@GrpcService
public class {ServiceName}GrpcImpl extends {ServiceName}Grpc.{ServiceName}ImplBase {
    
    @Resource
    private {ServiceName} {serviceName}; // 注入本地实现
    
    // 为每个Service方法生成对应的grpc方法
    @Override
    public void {methodName}({MethodName}Request request, StreamObserver<{MethodName}Response> responseObserver) {
        // 1. 从request提取参数
        // 2. 调用本地service方法
        // 3. 封装response并返回
        responseObserver.onNext({MethodName}Response.newBuilder()...build());
        responseObserver.onCompleted();
    }
}
```

### 3. 代理实现类
**路径**: `common/src/main/java/com/cainsgl/common/service/{ServiceName}GrpcService.java`

```java
package com.cainsgl.common.service;

import com.cainsgl.grpc.api.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

// 备选实现，只在本地实现不存在时生效
@Service
@ConditionalOnMissingBean(type = "com.cainsgl.{模块名}.service.{ServiceName}Impl")
public class {ServiceName}GrpcService implements {ServiceName} {
    
    @GrpcClient("{模块名}")
    {ServiceName}Grpc.{ServiceName}BlockingStub {serviceName}Stub;
    
    // 为每个Service方法生成对应的远程调用
    @Override
    public {ReturnType} {methodName}({ParamType} {paramName}) {
        {MethodName}Response response = {serviceName}Stub.{methodName}(
            {MethodName}Request.newBuilder().set{ParamName}({paramName}).build()
        );
        return response.get{ResponseField}();
    }
}
```

**关键规则**:
- `@ConditionalOnMissingBean`的type值为: `"com.cainsgl.{模块名}.service.{ServiceName}Impl"`
- `@GrpcClient`的值为模块名(小写)
- 方法实现: 构建Request → 调用stub → 提取Response字段

---

## 完整示例

**输入**: `test TestService`

**假设Service接口**:
```java
package com.cainsgl.common.service;

public interface TestService {
    String sayHello(String who);
}
```

**生成**:

#### 1. test.proto
```proto
syntax = "proto3";
package com.cainsgl.grpc;
option java_package = "com.cainsgl.grpc.api";
option java_outer_classname = "TestServiceProto";
option java_multiple_files = true;

message SayHelloRequest {
  string who = 1;
}
message SayHelloResponse {
  string message = 1;
}
service TestService {
  rpc sayHello (SayHelloRequest) returns (SayHelloResponse);
}
```

#### 2. TestServiceGrpcImpl.java
```java
package com.cainsgl.test.api;

import com.cainsgl.common.service.TestService;
import com.cainsgl.grpc.api.*;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class TestServiceGrpcImpl extends TestServiceGrpc.TestServiceImplBase {
    
    @Resource
    private TestService testService;
    
    @Override
    public void sayHello(SayHelloRequest request, StreamObserver<SayHelloResponse> responseObserver) {
        String who = request.getWho();
        String result = testService.sayHello(who);
        responseObserver.onNext(SayHelloResponse.newBuilder().setMessage(result).build());
        responseObserver.onCompleted();
    }
}
```

#### 3. TestGrpcService.java
```java
package com.cainsgl.common.service;

import com.cainsgl.grpc.api.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(type = "com.cainsgl.test.service.TestServiceImpl")
public class TestGrpcService implements TestService {
    
    @GrpcClient("test")
    TestServiceGrpc.TestServiceBlockingStub testServiceStub;
    
    @Override
    public String sayHello(String who) {
        SayHelloResponse response = testServiceStub.sayHello(
            SayHelloRequest.newBuilder().setWho(who).build()
        );
        return response.getMessage();
    }
}
```

---

## 注意事项

1. **方法名转换**: Service接口的`sayHello` → proto的`sayHello` (保持一致)
2. **参数名转换**: `who` → `SayHelloRequest.who` → `setWho()`/`getWho()`
3. **返回值字段**: 与Service接口返回的类型一致，需要在proto中定义，并在GrpcImpl中封装成Response并返回
4. **包名规则**: proto生成的代码在`com.cainsgl.grpc.api`包
5. **完成调用**: GrpcImpl中必须调用`responseObserver.onCompleted()`
