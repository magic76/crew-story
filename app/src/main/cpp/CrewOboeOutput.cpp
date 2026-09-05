#include <jni.h>
#include <oboe/Oboe.h>
#include <atomic>
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <memory>

namespace {
constexpr uint32_t kCapacity = 1u << 20; // 43.7 s @ 24 kHz; power of two.
constexpr uint32_t kMask = kCapacity - 1;
constexpr uint32_t kPrimeSamples = 1920; // 80 ms of Gemini's 24 kHz PCM.
constexpr uint32_t kMaxPrimeSamples = 4800; // 200 ms max pre-roll.
class OutputEngine : public oboe::AudioStreamDataCallback {
public:
    std::atomic<uint32_t> read{0}, write{0};
    std::atomic<uint32_t> fifoUnderruns{0};
    std::atomic<uint32_t> fifoDroppedSamples{0};
    std::atomic<bool> primed{false};
    std::atomic<bool> forceOutput{false};
    std::atomic<uint32_t> primeSamples{kPrimeSamples};
    int16_t fifo[kCapacity]{};
    std::shared_ptr<oboe::AudioStream> stream;
    bool pop(int16_t &sample) {
        uint32_t r = read.load(std::memory_order_relaxed);
        uint32_t w = write.load(std::memory_order_acquire);
        if (r == w) return false;
        sample = fifo[r & kMask];
        read.store(r + 1, std::memory_order_release);
        return true;
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void *data, int32_t frames) override {
        auto *out = static_cast<int16_t*>(data);
        if (!primed.load(std::memory_order_acquire)) {
            uint32_t buffered = write.load(std::memory_order_acquire) - read.load(std::memory_order_relaxed);
            if (!forceOutput.load(std::memory_order_acquire) && buffered < primeSamples.load(std::memory_order_acquire)) {
                std::memset(out, 0, static_cast<size_t>(frames) * sizeof(int16_t));
                return oboe::DataCallbackResult::Continue;
            }
            primed.store(true, std::memory_order_release);
        }
        for (int32_t i = 0; i < frames; ++i) {
            if (!pop(out[i])) {
                fifoUnderruns.fetch_add(1, std::memory_order_relaxed);
                uint32_t target = primeSamples.load(std::memory_order_relaxed);
                while (target < kMaxPrimeSamples && !primeSamples.compare_exchange_weak(target, std::min(kMaxPrimeSamples, target + 1200u), std::memory_order_relaxed)) {}
                primed.store(false, std::memory_order_release);
                forceOutput.store(false, std::memory_order_release);
                std::memset(out + i, 0, static_cast<size_t>(frames - i) * sizeof(int16_t));
                break;
            }
        }
        return oboe::DataCallbackResult::Continue;
    }

    bool start(bool mediaOutput) {
        stop();
        read.store(0);
        write.store(0);
        fifoUnderruns.store(0);
        fifoDroppedSamples.store(0);
        primed.store(false);
        forceOutput.store(false);
        primeSamples.store(kPrimeSamples);

        oboe::AudioStreamBuilder b;
        b.setDirection(oboe::Direction::Output);
        b.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        b.setSharingMode(oboe::SharingMode::Shared);
        b.setFormat(oboe::AudioFormat::I16);
        b.setChannelCount(oboe::ChannelCount::Mono);
        b.setSampleRate(24000);
        b.setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium);
        b.setDataCallback(this);
        if (mediaOutput) {
            b.setUsage(oboe::Usage::Media);
            b.setContentType(oboe::ContentType::Music);
        } else {
            b.setUsage(oboe::Usage::VoiceCommunication);
            b.setContentType(oboe::ContentType::Speech);
        }
        oboe::Result r = b.openStream(stream);
        if (r != oboe::Result::OK || !stream) return false;
        r = stream->requestStart();
        if (r != oboe::Result::OK) {
            stream->close();
            stream.reset();
            return false;
        }
        return true;
    }

    void stop() {
        if (!stream) return;
        stream->stop();
        stream->close();
        stream.reset();
    }

    void finishTurn() {
        forceOutput.store(true, std::memory_order_release);
    }

    int xRuns() const {
        if (!stream) return -1;
        auto result = stream->getXRunCount();
        return result ? result.value() : -1;
    }
    bool exclusive() const { return stream && stream->getSharingMode() == oboe::SharingMode::Exclusive; }
    int underruns() const { return static_cast<int>(fifoUnderruns.load(std::memory_order_relaxed)); }
    int droppedSamples() const { return static_cast<int>(fifoDroppedSamples.load(std::memory_order_relaxed)); }
    int bufferedMilliseconds() const {
        return static_cast<int>((write.load(std::memory_order_acquire) - read.load(std::memory_order_acquire)) * 1000u / 24000u);
    }
    void push(const int16_t *in, uint32_t count) {
        uint32_t r = read.load(std::memory_order_acquire), w = write.load(std::memory_order_relaxed);
        if (r == w) {
            primeSamples.store(kPrimeSamples, std::memory_order_relaxed);
        }
        for (uint32_t i = 0; i < count; ++i) {
            if (w - r >= kCapacity - 1) {
                fifoDroppedSamples.fetch_add(count - i, std::memory_order_relaxed);
                break;
            }
            fifo[(w++) & kMask] = in[i];
        }
        write.store(w, std::memory_order_release);
    }
} engine;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeStart(JNIEnv*, jclass, jboolean mediaOutput) { return engine.start(mediaOutput == JNI_TRUE) ? JNI_TRUE : JNI_FALSE; }
extern "C" JNIEXPORT void JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeStop(JNIEnv*, jclass) { engine.stop(); }
extern "C" JNIEXPORT void JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeFlush(JNIEnv*, jclass) { uint32_t w = engine.write.load(); engine.read.store(w); engine.primed.store(false); engine.forceOutput.store(false); engine.primeSamples.store(kPrimeSamples); }
extern "C" JNIEXPORT void JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeFinishTurn(JNIEnv*, jclass) { engine.finishTurn(); }
extern "C" JNIEXPORT void JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeWrite(JNIEnv *env, jclass, jbyteArray pcm, jint length) {
    if (!pcm || length < 2) return; jsize n = env->GetArrayLength(pcm); length = length > n ? n : length;
    jbyte *p = env->GetByteArrayElements(pcm, nullptr); if (!p) return;
    engine.push(reinterpret_cast<int16_t*>(p), static_cast<uint32_t>(length / 2)); env->ReleaseByteArrayElements(pcm, p, JNI_ABORT);
}
extern "C" JNIEXPORT jint JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeGetBufferedMs(JNIEnv*, jclass) {
    return engine.bufferedMilliseconds();
}
extern "C" JNIEXPORT jstring JNICALL Java_com_crewpocket_story_NativeOboeOutput_nativeGetInfo(JNIEnv *env, jclass) {
    char info[176]; std::snprintf(info, sizeof(info), "Oboe SRC 24k · %s · xRun %d · buf %dms · pre %dms · drop %d", engine.exclusive() ? "Exclusive" : "Shared", engine.xRuns(), engine.bufferedMilliseconds(), static_cast<int>(engine.primeSamples.load() * 1000u / 24000u), engine.droppedSamples());
    return env->NewStringUTF(info);
}
