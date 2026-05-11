#include <fcntl.h>
#include <sys/ioctl.h>
#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <termios.h>

#define PT32_MAGIC	0x7f
#define PT32_WRITE	_IOW(PT32_MAGIC, 0x00, unsigned int)
#define PT32_READ	_IOR(PT32_MAGIC, 0x01, unsigned int)

#define DRIVER_NAME "/dev/pt32"





static const char* kTAG = "JNI_I2Ctransfer";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))




static int g_fd = -1;

static int open_fd_if_needed(void) {
    if (g_fd >= 0) return g_fd;

    g_fd = open(DRIVER_NAME, O_RDWR);

    return g_fd;
}
static int g_indoor_fd = -1;

static int open_fd_indoor_needed(void) {
    if (g_indoor_fd >= 0) return g_indoor_fd;

    g_indoor_fd = open("/sys/class/gpio_sw/INT_RING_DETECT/data", O_RDONLY);
    if (g_indoor_fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "I2CTransfer",
                            "Failed to open indoor GPIO: %s", strerror(errno));
    }
    return g_indoor_fd;
}


static void reset_fd(void) {
    if (g_fd >= 0) {
        close(g_fd);
        g_fd = -1;
    }

    if (g_indoor_fd >= 0) {
        close(g_indoor_fd);
        g_indoor_fd = -1;
    }
}


JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void)vm; (void)reserved;
    reset_fd();
}




#define PT32_DATA_LEN 15
#define PT32_WRITE_LEN 24

JNIEXPORT jint JNICALL
Java_com_alfanar_i2c_I2CTransfer_i2cWrite(JNIEnv *env, jclass clazz, jbyteArray byteArray) {
    if (byteArray == NULL) return -1;

    int fd = open_fd_if_needed();
    if (fd < 0) return -1;

    jbyte arg[PT32_WRITE_LEN] = {0};
    (*env)->GetByteArrayRegion(env, byteArray, 0, PT32_WRITE_LEN, arg);
    if ((*env)->ExceptionCheck(env)) return -1;

    return ioctl(fd, PT32_WRITE, arg);
}



JNIEXPORT jint JNICALL
Java_com_alfanar_i2c_I2CTransfer_i2cRead(JNIEnv *env, jclass clazz, jbyteArray byteArray) {
    if (byteArray == NULL) return -1;

    jbyte arg[PT32_DATA_LEN] = {0};

    int fd = open_fd_if_needed();
    if (fd < 0) return fd;

    int res = ioctl(fd, PT32_READ, arg);
    if (res == PT32_DATA_LEN) {
        (*env)->SetByteArrayRegion(env, byteArray, 0, PT32_DATA_LEN, arg);
        if ((*env)->ExceptionCheck(env)) return -1;
    }

    return res;
}










JNIEXPORT void JNICALL
Java_com_alfanar_i2c_I2CTransfer_setLedState(JNIEnv *env, jclass clazz, jint val) {
    //  LOGI("setled1");

    if (val != 0 && val != 1) return;  // ✅ validasyon
    //  LOGI("setled2");

    int fd = open("/sys/class/gpio_sw/LED_A/data", O_WRONLY);
//   LOGI("setled3 fd = %d" , fd);
    if (fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "I2CTransfer",
                            "Failed to open led GPIO: %s", strerror(errno));
        return;
    }
    // LOGI("setled4");

    char buf[1] = { '0' + val };
    if (write(fd, buf, 1) < 0) {
        LOGI("LED write failed: %s", strerror(errno));
    }
    close(fd);
}




JNIEXPORT jint JNICALL
Java_com_alfanar_i2c_I2CTransfer_readIndoor(JNIEnv *env, jclass clazz) {
    int fd = open_fd_indoor_needed();
    if (fd < 0) return -1;

    char buf[2] = {0};
    ssize_t n = read(fd, buf, 1);
    if (n < 1) {
        __android_log_print(ANDROID_LOG_ERROR, "I2CTransfer",
                            "Failed to read indoor GPIO: %s", strerror(errno));
        g_indoor_fd = -1;  // sonraki çağrıda yeniden açmayı dene
        return -1;
    }

    // sysfs'ten sonraki okuma için başa sar
    lseek(fd, 0, SEEK_SET);

    char c = buf[0];
    if (c != '0' && c != '1') return -1;  // beklenmedik değer
    return c - '0';
}