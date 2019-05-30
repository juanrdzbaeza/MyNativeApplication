#include <jni.h>
#include <string>

#include <jni.h>
#include <string>
#include <android/log.h>
#include <chrono>
#include <vector>
#include <thread>
#include <omp.h>
using namespace std;

/**
 *
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_juanrdzbaeza_mynativeapplication_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

/**
 *
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_juanrdzbaeza_mynativeapplication_MainActivity_histogramaC(
        JNIEnv *env, jobject instance,
        jint tam, jshortArray imagen_,
        jintArray h_
        ){
    // cuerpo
    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    jboolean isCopyA, isCopyB;
    long aux;
    aux = 0;
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
            for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                for (int k = 0; k < 3; k++) h[k + i] = 0;
            }
            for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                }
            }
            for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                }
            }
            for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                for (int j = 0; j < tam; j++) {
                    for (int k = 0; k < 3; k++) {
                        for (int x = 0; x < 256; x++) {
                            aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] - h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                            h[k + x] = (int) (aux % 256);
                        }
                        imagen[k + i + j] = (short) (imagen[k + i + j] *h[k + imagen[k + i + j]]);
                    }
                }
            }

        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);

} //fin de histogramaC

/**
 *
 * @param me
 * @param nth
 * @param tam
 * @param imagen
 * @param h
 */
void histoChunk(int me, int nth, int tam, short* imagen, int* h) {
    long aux = 0;
    for (int i = 0; i < 256; i++) {// Inicializa el histograma.
        for (int k = 0; k < 3; k++) h[k + i] = 0;
    }
    for (int i = 0; i < tam; i++) {// Inicializa la imagen.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
        }
    }
    for (int i = 0; i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
        }
    }
    for (int i = (me * tam) / nth; i < ((me + 1) * tam) / nth; i++) { // Modificar imagen utilizando el histograma
        for (int j = 0; j < tam; j++) {
            for (int k = 0; k < 3; k++) {
                for (int x = 0; x < 256; x++) {
                    aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] -
                                  h[k + x] * h[k + x] * h[k + x] + h[k + x] * h[k + x]);
                    h[k + x] = (int) (aux % 256);
                }
                imagen[k + i + j] = (short) (imagen[k + i + j] * h[k + imagen[k + i + j]]);
            }
        }
    }
}

/**
 *
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_juanrdzbaeza_mynativeapplication_MainActivity_histogramaCpth(
        JNIEnv *env,
        jobject instance, jint tam,
        jint nth,
        jshortArray imagen_,
        jintArray h_
        ){
    // cuerpo

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);
    int** hpriv;
    jboolean isCopyA, isCopyB;
    vector<thread>t;
    hpriv=new int*[nth];
    for(int i=0; i<nth; i++){
        hpriv[i]=new int[tam];
        for(int j=0; j<tam;j++)hpriv[i][j]=0;
    }
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {

            for (int i = 0; i < nth; i++)
                t.push_back(thread(histoChunk,i,nth,tam,imagen,hpriv[i]));
            for (int i = 0; i < nth; i++) {
                t[i].join();
                for(int j=0; j<256; j++) h[j]+=hpriv[i][j];
            }
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);

} // fin de histogramaCpth

/**
 *
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_juanrdzbaeza_mynativeapplication_MainActivity_histogramaOpenMP(
        JNIEnv *env,
        jobject instance,
        jint tam,
        jshortArray imagen_,
        jintArray h_
        ){
    // cuerpo

    jshort *imagen = env->GetShortArrayElements(imagen_, NULL);
    jint *h = env->GetIntArrayElements(h_, NULL);

    jboolean isCopyA, isCopyB;
    if (imagen==NULL) __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get data array imagen reference");
    else {
        if (h == NULL)
            __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Can't get result array h reference");
        else {
#pragma omp parallel
            {
                long aux = 0;
                for (int i = 0; i < 256; i++) {// Inicializa el histograma.
                    for (int k = 0; k < 3; k++) h[k + i] = 0;
                }
                for (int i = 0; i < tam; i++) {// Inicializa la imagen.
                    for (int j = 0; j < tam; j++) {
                        for (int k = 0; k < 3; k++) imagen[k + i + j] = (short) ((i * j) % 256);
                    }
                }
#pragma omp critical
                {
                    for (int i = 0;
                         i < tam; i++) {// Contabiliza el nº veces que aparece cada valor.
                        for (int j = 0; j < tam; j++) {
                            for (int k = 0; k < 3; k++) h[k + imagen[k + i + j]]++;
                        }
                    }
                }
#pragma omp for nowait
                {
                    for (int i = 0; i < tam; i++) { // Modificar imagen utilizando el histograma
                        for (int j = 0; j < tam; j++) {
                            for (int k = 0; k < 3; k++) {
                                for (int x = 0; x < 256; x++) {
                                    aux = (long) (h[k + x] * h[k + x] * h[k + x] * h[k + x] -
                                                  h[k + x] * h[k + x] * h[k + x] +
                                                  h[k + x] * h[k + x]);
                                    h[k + x] = (int) (aux % 256);
                                }
                                imagen[k + i + j] = (short) (imagen[k + i + j] *
                                                             h[k + imagen[k + i + j]]);
                            }
                        }
                    }
                }
            }
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HOOKnative", "Values h[0]=%d; h[5]=%d; h[10]=%d",h[0],h[5],h[10]);
    if (imagen!=NULL) env->ReleaseShortArrayElements(imagen_, imagen, 0);
    if (h!=NULL) env->ReleaseIntArrayElements(h_, h, 0);

} // fin de histogramaOpenMP