#include "library.h"

#include <jni.h>
#include <dlfcn.h>
#include <malloc.h>
#include <unistring/stdint.h>

int hook(JNIEnv *env) {
    jclass JarInjector = (*env)->DefineClass(env, NULL, NULL, (jbyte *)JarInjectorBytes, JarInjectorSize);
    jmethodID hook = (*env)->GetMethodID(env, JarInjector, "hook", "()Z");
    return (uint8_t)(*env)->CallStaticBooleanMethod(env, JarInjector, hook);
}

__attribute__((constructor))
void attach(void) {
    void *jvmDLL = dlopen("libjvm.so", RTLD_LAZY);

    typedef jint (JNICALL *GetCreatedJavaVMs_t)(JavaVM **, jsize, jsize *);
    GetCreatedJavaVMs_t GetCreatedJavaVMs = dlsym(jvmDLL, "JNI_GetCreatedJavaVMs");

    jsize nVMs;
    GetCreatedJavaVMs(NULL, 0, &nVMs);
    JavaVM *buffer = malloc(nVMs * sizeof(JavaVM));
    GetCreatedJavaVMs(&buffer, nVMs, &nVMs);

    int ret = -1;
    for (jsize index = 0; index < nVMs; index++) {
        JavaVM *jvm = (buffer + index);
        JNIEnv *env = NULL;

        (*jvm)->AttachCurrentThread(jvm, (void **) &env, 0);
        (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_8);

        ret = hook(env);

        (*jvm)->DetachCurrentThread(jvm);

        if (ret != -1) break;
    }

    dlclose(jvmDLL);
    end: return;
}