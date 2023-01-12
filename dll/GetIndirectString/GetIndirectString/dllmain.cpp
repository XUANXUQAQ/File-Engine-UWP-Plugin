// dllmain.cpp : 定义 DLL 应用程序的入口点。
#include "pch.h"
#include "GetIndirectString.h"
#include <Shlwapi.h>
#include <string>
#pragma comment(lib, "Shlwapi.lib")

std::wstring Java_To_WStr(JNIEnv* env, jstring string);
jstring WStr_To_Java(JNIEnv* env, const wchar_t* src);

JNIEXPORT jstring JNICALL Java_file_engine_uwp_dllInterface_GetIndirectString_SHLoadIndirectString
(JNIEnv* env, jclass, jstring source_jstr)
{
    const auto source = Java_To_WStr(env, source_jstr);
    wchar_t output[1000];
    SHLoadIndirectString(source.c_str(), output, 1000, nullptr);
    return WStr_To_Java(env, output);
}

std::wstring Java_To_WStr(JNIEnv* env, jstring string)
{
    std::wstring value;
    const jchar* raw = env->GetStringChars(string, nullptr);
    const jsize len = env->GetStringLength(string);
    value.assign(raw, raw + len);
    env->ReleaseStringChars(string, raw);
    return value;
}

jstring WStr_To_Java(JNIEnv* env, const wchar_t* src)
{
    const size_t src_len = wcslen(src);
    auto* dest = new jchar[src_len + 1];
    memset(dest, 0, sizeof(jchar) * (src_len + 1));
    for (size_t i = 0; i < src_len; i++)
    {
        memcpy(&dest[i], &src[i], 2);
    }
    const jstring dst = env->NewString(dest, src_len);
    delete [] dest;
    return dst;
}
