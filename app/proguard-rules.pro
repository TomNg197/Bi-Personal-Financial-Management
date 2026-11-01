# kotlinx.serialization: keep generated serializers of the data model
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class vn.mrsbi.handout.data.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class vn.mrsbi.handout.data.**$$serializer { *; }
