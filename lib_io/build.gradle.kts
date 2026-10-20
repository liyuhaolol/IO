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

dependencies {
    // FileProvider（getShareableUri）
    //千万别升级，会导致最低版本不能是19
    implementation("androidx.core:core:1.13.0")
}


