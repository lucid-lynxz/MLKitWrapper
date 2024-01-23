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
    // app模块中自行添加 kotlin 依赖 
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.6.10"
    implementation 'com.google.android.material:material:1.2.1'
    
    // 必须, 导入基础库
    implementation "com.github.lucid-lynxz.MLKitWrapper:base:latest"
    
    // 可选, 文本识别时导入, 默认支持中文简体繁体和英文识别
    implementation "com.github.lucid-lynxz.MLKitWrapper:textdetector:latest"
    
    // 可选, 物体识别时导入
    implementation "com.github.lucid-lynxz.MLKitWrapper:objectdetector:latest"
}
```

### 各工具类方法说明

...