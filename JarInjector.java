import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class JarInjector extends URLClassLoader {
    public static JarInjector INSTANCE;

    private JarInjector(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public static int hook() {
        System.out.println("[DEBUG] Hellow from JVM!");
        ClassLoader cl = queryUserForClassLoader(getClassLoadersMap());
        if (cl != null) {
            INSTANCE = new JarInjector(cl);
            return Arrays.stream(queryUserForJars())
                    .map(JarInjector::loadJar)
                    .anyMatch(b -> !b) ? 1 : 0;
        }
        return -1;
    }

    private static Map<String, ClassLoader> getClassLoadersMap() {
        return Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getContextClassLoader)
                .filter(Objects::nonNull).distinct().sorted()
                .collect(CollectionMappify(cl -> cl.getClass().getName()));
    }

    private static ClassLoader queryUserForClassLoader(Map<String, ClassLoader> cls) {
        return cls.get(queryUserCommon("Select a ClassLoader to use for defining", cls.keySet().toArray(new String[0])));
    }

    private static File[] queryUserForJars() {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Select jars to inject!");
        jfc.setApproveButtonText("Inject");
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("JARs only", "jar"));
        jfc.setMultiSelectionEnabled(true);
        return jfc.showDialog(null, null) == JFileChooser.APPROVE_OPTION
                ? jfc.getSelectedFiles() : new File[0];
    }

    private static boolean loadJar(File jar) {
        Class<?> Main;
        try {
            INSTANCE.addURL(jar.toURI().toURL());
            Main = INSTANCE.loadClass(queryUserForMainClass(jar), true);
        } catch (Exception ignored) {
            return false;
        }

        Map<String, Constructor<?>> cstrs = getCstrMapOfClass(Main);
        Map<String, Method> methods = getMethodMapOfClass(Main);
        String[] execs = Stream.concat(cstrs.keySet().stream(), methods.keySet().stream()).toArray(String[]::new);

        while (true) {
            String exec = queryUserCommon("Select an Executable to be evaluated", execs);
            if (Objects.isNull(exec)) break;

            try {
                Executable ex;
                if (Objects.nonNull(ex = cstrs.get(exec))) ((Constructor<?>) ex).newInstance(parseArgsForExec(ex));
                if (Objects.nonNull(ex = methods.get(exec))) ((Method) ex).invoke(parseArgsForExec(ex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private static String queryUserForMainClass(File jar) throws Exception {
        return queryUserCommon("Select a Class to be treated as Main", new JarFile(jar).stream()
                .map(ZipEntry::getName)
                .filter(name -> !name.startsWith("META-INF/") && name.endsWith(".class"))
                .map(name -> name.substring(0, name.length() - 6).replace("/", "."))
                .sorted().toArray(String[]::new));
    }

    private static String queryUserCommon(Object message, String[] options) {
        return (String) JOptionPane.showInputDialog(null, message, "JarInjector - by Delusional",
                JOptionPane.INFORMATION_MESSAGE, null, options, null);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }


    private static <V, K> Collector<V, ?, Map<K, V>> CollectionMappify(Function<V, K> keyify) {
        return Collectors.toMap(keyify, v -> v);
    }

    private static <T> Stream<T> arraysToStream(T[] a, T[] b) {
        return Stream.concat(Arrays.stream(a), Arrays.stream(b));
    }

    private static <K, V> Map<K, V> arraysToMap(V[] a, V[] b, Function<V, K> keyify) {
        return arraysToStream(a, b).distinct().collect(CollectionMappify(keyify));
    }

    private static Map<String, Constructor<?>> getCstrMapOfClass(Class<?> clazz) {
        return arraysToMap(clazz.getDeclaredConstructors(), clazz.getConstructors(), Constructor::toGenericString);
    }

    private static Map<String, Method> getMethodMapOfClass(Class<?> clazz) {
        return arraysToMap(clazz.getDeclaredMethods(), clazz.getMethods(), Method::toGenericString);
    }

    private static Object[] parseArgsForExec(Executable ex) {
        return Arrays.stream(ex.getParameterTypes())
                .map(JarInjector::safeInstantiate)
                .toArray(Object[]::new);
    }
}