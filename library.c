#include "library.h"

#include <jni.h>
#include <dlfcn.h>
#include <malloc.h>
#include <unistring/stdint.h>
#include <unistd.h>
#include <pthread.h>

int inject(JNIEnv *env) {
    printf("[DEBUG] Defining Injector class of [size=%lu]...\n", sizeof(JarInjectorBytes));
    jclass JarInjector = (*env)->DefineClass(env, NULL, NULL, (jbyte *) JarInjectorBytes, sizeof(JarInjectorBytes));
    jmethodID hook = (*env)->GetStaticMethodID(env, JarInjector, "hook", "()I");
    printf("[DEBUG] Hooking into Injector class\n");
    return (uint8_t) (*env)->CallStaticBooleanMethod(env, JarInjector, hook);
}

void *hook(void *args) {
    printf("[DEBUG] Fetching available JVMs...\n");

    jsize nVMs;
    JNI_GetCreatedJavaVMs(NULL, 0, &nVMs);
    printf("[DEBUG] Found %d\n", nVMs);
    JavaVM *buffer = malloc(nVMs * sizeof(JavaVM));
    JNI_GetCreatedJavaVMs(&buffer, nVMs, &nVMs);

    int ret = -1;
    for (jsize index = 0; index < nVMs; index++) {
        JavaVM *jvm = &buffer[index];
        JNIEnv *env = NULL;

        printf("[DEBUG] AttachCurrentThread #%d\n", (*jvm)->AttachCurrentThread(jvm, (void **) &env, 0));
        printf("[DEBUG] GetEnv #%d\n", (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_8));

        ret = inject(env);

        printf("[DEBUG] DetachCurrentThread #%d\n", (*jvm)->DetachCurrentThread(jvm));

        if (ret != -1) {
            printf("[DEBUG] Hooked successfully #%d\n", ret);
            break;
        }
    }

    pthread_exit(NULL);
}

__attribute__((constructor))
void attach(void) {
    printf("[DEBUG] Attached to process of [pid=%d]\n", getpid());

    pthread_t thread;
    pthread_create(&thread, NULL, &hook, NULL);
    printf("[DEBUG] Created a new thread for the hook\n");
}
