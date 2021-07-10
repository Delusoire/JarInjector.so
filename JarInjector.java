import java.lang.Class;
import java.lang.Thread;
import java.lang.Exception;
import java.lang.ClassLoader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Map;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.lang.System;
import java.util.zip.ZipEntry;

public class JarInjector extends URLClassLoader {
    public static JarInjector INSTANCE;

    public static int hook() {
        System.out.println("[DEBUG] Hellow from JVM!");
        ClassLoader cl = queryUserForCL(getClassLoadersMap());
        if (cl != null) {
            INSTANCE = new JarInjector(cl);
            return Arrays.stream(queryUserForJars())
                    .map(JarInjector::loadJar)
                    .anyMatch(b -> !b) ? 1 : 0;
        }
        return -1;
    }

    private static <V, K> Collector<V, ?, Map<K, V>> Mapify(Function<V, K> keyify) {
        return Collectors.toMap(keyify, v -> v);
    }

    private static Map<String, ClassLoader> getClassLoadersMap() {
        return Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getContextClassLoader)
                .filter(Objects::nonNull).distinct().sorted()
                .collect(Mapify(cl -> cl.getClass().getName()));
    }

    private static Map<String, Method> getCallableMethodsMap(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .collect(Mapify(Method::toGenericString));
    }

    private static File[] queryUserForJars () {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Select jars to inject!");
        jfc.setApproveButtonText("Inject");
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("JARs only", "jar"));
        jfc.setMultiSelectionEnabled(true);
        return (jfc.showDialog(null, null) == JFileChooser.APPROVE_OPTION) ? jfc.getSelectedFiles() : new File[0];
    }

    private static String queryUserCommon(Object message, String[] options) {
        return (String) JOptionPane.showInputDialog(null, message, "JarInjector - by Delusional", JOptionPane.INFORMATION_MESSAGE, null, options, null);
    }

    private static ClassLoader queryUserForCL(Map<String, ClassLoader> cls) {
        return cls.get(queryUserCommon("Choose The ClassLoader (cancel if none matches)", cls.keySet().toArray(new String[0])));
    }

    private static String queryUserForMainClass(File jar) throws Exception {
        return queryUserCommon("Choose The Main class", new JarFile(jar).stream()
                .map(ZipEntry::getName)
                .filter(name -> !name.startsWith("META-INF/") && name.endsWith(".class"))
                .map(name -> name.substring(0, name.length() - 6).replace("/", "."))
                .sorted().toArray(String[]::new));
    }

    private static Method queryUserForCallee(Map<String, Method> methods) {
        return methods.get(queryUserCommon("Choose the method to call", methods.keySet().toArray(new String[0])));
    }

    // Living example of why java error handling suxsce
    private static Object safeInstantiate(Class<?> clazz) {
        Object ret = null;
        Constructor<?> cstr;
        if (clazz.isArray()) {
          ret = Array.newInstance(clazz.getComponentType(), 0);
        } else {
            try {
                cstr = clazz.getDeclaredConstructor();
            } catch (Exception e0) {
                try {
                    System.err.println("[DEBUG] Fatal error @ Main.getDeclaredConstructor()");
                    cstr = clazz.getConstructor();
                } catch (Exception e1) {
                    System.err.println("[DEBUG] Fatal error @ Main.getConstructor()");
                    return null;
                }
            }
            try {
                ret = cstr.newInstance();
            } catch (Exception e) { e.printStackTrace(); }
        }
        return ret;
    }

    private static boolean loadJar(File jar) {
        boolean ret = false;
        try {
            INSTANCE.addURL(jar.toURI().toURL());
            Class<?> Main = INSTANCE.loadClass(queryUserForMainClass(jar), true);
            ret = true;

            safeInstantiate(Main);
            Map<String, Method> methods = getCallableMethodsMap(Main); Method method;
            while ((method = queryUserForCallee(methods)) != null)
                try {
                    method.invoke(Main, (Object[]) Arrays.stream(method.getParameterTypes())
                            .map(JarInjector::safeInstantiate)
                            .toArray(Object[]::new));
                } catch (Exception e) { e.printStackTrace(); }
        } catch (Exception ignored) { }
        return ret;
    }

    private JarInjector(ClassLoader parent) {
        super(new URL[0], parent);
    }
}