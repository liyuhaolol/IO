plugins {
    id("com.android.library")
}

val versionName = "1.1.6"

android {
    namespace = "spa.lyh.cn.utils_io"
    compileSdk = 37

    defaultConfig {
        minSdk = 19
    }

    buildTypes {
        release {
            isMinifyEnabled  = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility =  JavaVersion.VERSION_25
    }

}


