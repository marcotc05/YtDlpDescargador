# ========================================================
# Reglas de Preservacion para Motor Nativo (youtubedl-android)
# ========================================================
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.apache.commons.io.** { *; }
-keep class java.util.zip.** { *; }
-keep class java.util.jar.** { *; }
-keep class androidx.documentfile.** { *; }

# ========================================================
# Jackson / DOM / Java SE Desktop (Ausentes en Android SDK)
# ========================================================
-dontwarn org.w3c.dom.bootstrap.**
-dontwarn org.w3c.dom.ls.**
-dontwarn com.fasterxml.jackson.databind.ext.**
-dontwarn com.fasterxml.jackson.databind.ext.DOMSerializer
-dontwarn java.beans.**
-dontwarn javax.xml.**
-dontwarn javax.xml.stream.**
-dontwarn javax.xml.bind.**
-dontwarn java.nio.file.**
-dontwarn org.apache.commons.**
-dontwarn com.yausername.**

# ========================================================
# Configuracion de Optimizacion R8
# ========================================================
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontobfuscate