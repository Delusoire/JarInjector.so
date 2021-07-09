import java.lang.Class;
import java.lang.Thread;
import java.lang.Exception;
import java.lang.ClassLoader;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Map;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class JarInjector extends URLClassLoader {
    public static JarInjector INSTANCE;

    public static int hook() {
        ClassLoader cl = queryUserForCL();

        if (cl != null) {
            INSTANCE = new JarInjector(cl);
            return Arrays.stream(queryUserForJars()).map(JarInjector::loadJar).anyMatch(b -> !b) ? 1 : 0;
        }

        return -1;
    }

    private static Map<String, ClassLoader> getCLMap() {
        return Thread.getAllStackTraces().keySet().stream()
            .map(thread -> thread.getContextClassLoader())
            .filter(cl -> cl != null).distinct().sorted()
            .collect(Collectors.toMap(cl -> cl.getClass().getName(), cl -> cl));
    }

    private static String queryUserCommon(String text, String[] options) {
        return (String) JOptionPane.showInputDialog(null, text, "JarInjector - by Delusional", JOptionPane.INFORMATION_MESSAGE, null, options, null);
    }

    private static ClassLoader queryUserForCL() {
        Map<String, ClassLoader> cls = getCLMap();
        String targetCLName = queryUserCommon("Choose The ClassLoader (cancel if none matches)", cls.keySet().toArray(new String[0]));
        return cls.get(targetCLName);
    }

    private static File[] queryUserForJars () {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Select jars to inject!");
        jfc.setApproveButtonText("Inject");
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("JARs only", new String[] { "jar" }));
        jfc.setMultiSelectionEnabled(true);
        return (jfc.showDialog(null, null) == JFileChooser.APPROVE_OPTION) ? jfc.getSelectedFiles() : new File[0];
    }

    private static String queryUserForMainClass(File jar) throws Exception {
        String[] classes = new JarFile(jar).stream()
            .map(entry -> entry.getName())
            .filter(name -> !name.startsWith("META-INF/") && name.endsWith(".class"))
            .map(name -> name.substring(0, name.length() - 6).replace("/", "."))
            .sorted().toArray(String[]::new);

        return queryUserCommon("Choose The Main class", classes);
    }

    private static boolean loadJar(File jar) {
        boolean ret = false;
        try {
            INSTANCE.addURL(jar.toURI().toURL());
            Class Main = INSTANCE.loadClass(queryUserForMainClass(jar), true);
            ret = true;
            Main.newInstance();
        } catch (Exception e) { }
        return ret;
    }

    private JarInjector(ClassLoader parent) {
        super(new URL[0], parent);
    }
}