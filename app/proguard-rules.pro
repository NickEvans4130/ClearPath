# osmdroid
-keep class org.osmdroid.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }

# GraphHopper (if used)
-keep class com.graphhopper.** { *; }
-dontwarn com.graphhopper.**
