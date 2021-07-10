JarInjector.so
==============

 #### This simple Linux GUI Tool provides its users the ability to ***inject*** .jar files into already running jvm hosts.
 In order to use it, you first need to know which java process this shared library should attach to.
 Running `jps -mlv` will give you a list of each jvm host alongside with its pid.
 You can then go ahead and use any tool of your choice to inject the lib, [injector](https://github.com/kubo/injector) worked well for me.
 
 The prebuilt binary is available on the [Releases](https://github.com/Delusoire/JarInjector.so/releases) page.
