package com.bbmovie.ai_assistant_service.utils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

public class AudioConverterUtils {

    /**
     * Converts audio InputStream to a Whisper-compatible float array.
     * Supports MP3 via SPI (requires to be input stream to be marketable).
     */
    public static float[] convertToWhisperFormat(InputStream rawInputStream) throws IOException, UnsupportedAudioFileException {
        // Wrap in BufferedInputStream to ensure mark/reset support required by AudioSystem
        // and prevent "mark/reset not supported" errors with some MP3 decoders.
        InputStream inputStream = rawInputStream.markSupported() ?
                rawInputStream : new BufferedInputStream(rawInputStream);

        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(inputStream)) {
            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioInputStream pcmStream = sourceStream;

            // 1. Decode MP3/Compressed -> PCM
            if (sourceFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getSampleRate(),
                        false
                );
                pcmStream = AudioSystem.getAudioInputStream(decodedFormat, sourceStream);
            }

            // 2. Resample to 16kHz Mono
            AudioFormat whisperFormat = new AudioFormat(16000, 16, 1, true, false);
            try (AudioInputStream finalStream = AudioSystem.getAudioInputStream(whisperFormat, pcmStream)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[4096];
                while ((nRead = finalStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                byte[] rawBytes = buffer.toByteArray();
                float[] samples = new float[rawBytes.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    int sample = (rawBytes[2 * i] & 0xFF) | (rawBytes[2 * i + 1] << 8);
                    samples[i] = sample / 32768.0f;
                }
                return samples;
            } finally {
                // Close intermediate stream if created
                if (pcmStream != sourceStream) pcmStream.close();
            }
        }
    }
}
