#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// NOTE: This is a stub implementation.
// To enable real inference, integrate llama.cpp:
//   1. git submodule add https://github.com/ggerganov/llama.cpp.git engine/src/main/cpp/llama.cpp
//   2. Add llama.cpp sources to CMakeLists.txt
//   3. Replace stubs below with actual llama.cpp API calls

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_pocketmind_engine_LlamaCppEngine_nativeLoadModel(
    JNIEnv* env, jobject thiz, jstring path, jint n_ctx, jint n_gpu_layers) {
    
    const char* model_path = env->GetStringUTFChars(path, nullptr);
    LOGI("Loading model from: %s (ctx=%d, gpu_layers=%d)", model_path, n_ctx, n_gpu_layers);
    env->ReleaseStringUTFChars(path, model_path);
    
    // Return non-zero to indicate "loaded" (stub)
    // Replace with: return (jlong)llama_load_model_from_file(model_path, params);
    return (jlong)1;
}

JNIEXPORT jstring JNICALL
Java_com_pocketmind_engine_LlamaCppEngine_nativeGenerate(
    JNIEnv* env, jobject thiz, jlong model_ptr, jstring prompt,
    jint max_tokens, jfloat temperature, jobject callback) {
    
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating (ptr=%ld, tokens=%d, temp=%.2f)", model_ptr, max_tokens, temperature);
    env->ReleaseStringUTFChars(prompt, prompt_str);
    
    // Stub: call callback with mock tokens
    // Replace with actual llama.cpp token generation loop
    std::string response = "[Native inference not yet linked. Add llama.cpp sources.]";
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jfloatArray JNICALL
Java_com_pocketmind_engine_LlamaCppEngine_nativeEmbed(
    JNIEnv* env, jobject thiz, jlong model_ptr, jstring text) {
    
    // Return mock 384-dim embedding
    jfloatArray result = env->NewFloatArray(384);
    jfloat fill[384] = {};
    env->SetFloatArrayRegion(result, 0, 384, fill);
    return result;
}

JNIEXPORT void JNICALL
Java_com_pocketmind_engine_LlamaCppEngine_nativeFree(
    JNIEnv* env, jobject thiz, jlong model_ptr) {
    
    LOGI("Freeing model (ptr=%ld)", model_ptr);
    // Replace with: llama_free_model((llama_model*)model_ptr);
}

} // extern "C"
