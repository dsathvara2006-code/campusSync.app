# CampusSync ProGuard Rules

# Apache POI - Keep all POI classes for Excel export
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.commons.**
