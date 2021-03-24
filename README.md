# QuartzInitializerListener for Tomcat 10

There is a significant breaking change between Tomcat 9.0.x and Tomcat 10.0.x. The Java package used by the specification APIs has changed from javax.* to jakarta.* . The most popular java scheduler Quartz project is not change. So we must change javax.* to jakarta.* .

## Usage

1. Download QuartzInitializerListener.java to your project
2. Rewrite QuartzInitializerListener.java package match your project
3. Rewrite listener-class match your new QuartzInitializerListener in web.xml
