package com.vns.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioHelper.class);

    public static AudioInputStream getAudioInputStream(File file)
            throws UnsupportedAudioFileException, IOException {
        if (!file.exists()) {
            String error = "File not found: " + file.getAbsolutePath();
            logger.error(error);
            throw new FileNotFoundException(error);
        }
        if (file.getName().endsWith(".mp3")) {
            return convertToWave(file);
        } else {
            return AudioSystem.getAudioInputStream(file);
        }
    }

    static boolean playCompleted = true;

    public static synchronized void play(String fileName) {
        playCompleted = false;
        File audioFile = new File(fileName);
        try (AudioInputStream in = getAudioInputStream(audioFile)) {
            SourceDataLine line = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    in.getFormat());
            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(in.getFormat());
                line.start();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(in, out);
                byte[] data = out.toByteArray();
                line.write(data, 0, data.length);
                line.drain();
                line.close();
            } catch (LineUnavailableException | IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static AudioInputStream convertToWave(File mp3File) throws UnsupportedAudioFileException,
            IOException {
        AudioInputStream mp3In = AudioSystem.getAudioInputStream(mp3File);
        AudioFormat audioFormat = createAudioFormat(mp3In.getFormat().getSampleRate());
        MpegFormatConversionProvider cnv = new MpegFormatConversionProvider();
        return cnv.isConversionSupported(audioFormat, mp3In.getFormat()) ? cnv
                .getAudioInputStream(audioFormat, mp3In) : null;
    }

    private static AudioFormat createAudioFormat(float sampleRate) {
        boolean bigEndian = false;
        boolean signed = true;
        int bits = 16;
        int channels = 1;
        return new AudioFormat(sampleRate, bits, channels,
                signed, bigEndian);
    }
}