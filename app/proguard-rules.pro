# youtubedl-android: se invoca por reflexión/JNI-like y usa Jackson para el JSON de yt-dlp.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Referencias opcionales que Jackson/commons-io mencionan pero Android no tiene.
-dontwarn java.beans.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.apache.commons.**
-dontwarn org.slf4j.**
-dontwarn com.fasterxml.jackson.databind.ext.**
