# mlkitWrapper

> 拆分官方demo为不同模块,简化使用成本

## 使用说明

### 导包

[![](https://jitpack.io/v/lucid-lynxz/MLKitWrapper.svg)](https://jitpack.io/#lucid-lynxz/MLKitWrapper)

```gradle
// 1. 修改项目根目录 build.gradle 文件,添加仓库
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// 2. 在模块下的 build.gradle 中添加依赖
dependencies {
    implementation "com.github.lucid-lynxz.MLKitWrapper:base:latest"
    implementation "com.github.lucid-lynxz.MLKitWrapper:textdetector:latest"
    implementation "com.github.lucid-lynxz.MLKitWrapper:objectdetector:latest"
}
```

### 各工具类方法说明

...