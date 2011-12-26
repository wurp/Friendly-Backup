package com.geekcommune.friendlybackup.integ;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

public class TestNode {

    private URLClassLoader cl;
    private Object backupConfig;
    private Object backup;
    private Object restore;
    
    public TestNode(String configFilePath) throws Exception {
        cl = new URLClassLoader(
                ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs(),
                ClassLoader.getSystemClassLoader().getParent()
            );

        invokeStaticMethod(
                "com.geekcommune.friendlybackup.main.App",
                "wire",
                new String[] { "java.lang.String" },
                new Object[] { configFilePath });
        
        backupConfig = invokeStaticMethod(
                "com.geekcommune.friendlybackup.main.App",
                "getBackupConfig",
                new String[0],
                new Object[0]);

        backup = invokeConstructor(
                "com.geekcommune.friendlybackup.main.Backup",
                new String[0],
                new Object[0]);

        restore = invokeConstructor(
                "com.geekcommune.friendlybackup.main.Restore",
                new String[0],
                new Object[0]);
    }

    Object invokeMethod(Object targetObject, String methodName, String[] argumentClassNames, Object[] arguments) throws Exception {
        return invokeMethod(targetObject.getClass().getName(), targetObject, methodName, argumentClassNames, arguments);
    }

    Object invokeStaticMethod(String targetClassName, String methodName, String[] argumentClassNames, Object[] arguments) throws Exception {
        return invokeMethod(targetClassName, null, methodName, argumentClassNames, arguments);
    }
    
    Object invokeMethod(String targetClassName, Object targetObject, String methodName, String[] argumentClassNames, Object[] arguments) throws Exception {
        Class<?> targetClass = cl.loadClass(targetClassName);
        
        Class<?>[] argClasses = classesForClassnames(argumentClassNames);
        
        Method m = targetClass.getMethod(methodName, argClasses);
        return m.invoke(targetObject, arguments);
    }
    
    Object invokeConstructor(String targetClassName, String[] argumentClassNames, Object[] arguments) throws Exception {
        Class<?> targetClass = cl.loadClass(targetClassName);
        
        Class<?>[] argClasses = classesForClassnames(argumentClassNames);
        
        Constructor<?> m = targetClass.getConstructor(argClasses);
        return m.newInstance(arguments);
    }

    private Class<?>[] classesForClassnames(String[] argumentClassNames)
            throws ClassNotFoundException {
        Class<?> argClasses[] = new Class<?>[argumentClassNames.length];
        for(int i = 0; i < argumentClassNames.length; ++i) {
            argClasses[i] = cl.loadClass(argumentClassNames[i]);
        }
        return argClasses;
    }
    
    public File getRestoreDirectory() throws Exception {
        return (File) invokeMethod(backupConfig, "getRestoreRootDirectory", new String[0], new Object[0]);
    }

    public void backup() throws Exception {
        invokeMethod(
                backup,
                "doBackup",
                new String[] { "java.lang.String" },
                new Object[] { "password" });
    }

    public void restore() throws Exception {
        invokeMethod(
                restore,
                "doRestore",
                new String[] { "java.lang.String" },
                new Object[] { "password" });
    }

    public File[] getBackupRootDirectories() throws Exception {
        return (File[]) invokeMethod(backupConfig, "getBackupRootDirectories", new String[0], new Object[0]);
    }

    public File getRestoreRootDirectory() throws Exception {
        return (File) invokeMethod(backupConfig, "getRestoreRootDirectory", new String[0], new Object[0]);
    }

}
