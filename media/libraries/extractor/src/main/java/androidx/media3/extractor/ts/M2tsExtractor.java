/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with this License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.extractor.ts;

import androidx.media3.common.util.TimestampAdjuster;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.text.SubtitleParser;
import java.io.IOException;

public final class M2tsExtractor implements Extractor {

  private static final int M2TS_PACKET_SIZE = 192;
  private static final int TS_PACKET_SIZE = 188;
  private static final int M2TS_HEADER_SIZE = M2TS_PACKET_SIZE - TS_PACKET_SIZE;
  private static final int SNIFF_PACKET_COUNT = 5;

  private final TsExtractor tsExtractor;
  private final byte[] sniffBuffer = new byte[TS_PACKET_SIZE * SNIFF_PACKET_COUNT];
  private boolean headerSkipped;

  public M2tsExtractor(SubtitleParser.Factory subtitleParserFactory) {
    tsExtractor =
        new TsExtractor(
            TsExtractor.MODE_SINGLE_PMT,
            TsExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA,
            subtitleParserFactory,
            new TimestampAdjuster(0),
            new DefaultTsPayloadReaderFactory(
                DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS
                    | DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM),
            TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES);
  }

  @Override
  public boolean sniff(ExtractorInput input) throws IOException {
    input.peekFully(sniffBuffer, 0, sniffBuffer.length);
    for (int i = 0; i < sniffBuffer.length; i += TS_PACKET_SIZE) {
        if (sniffBuffer[i] == TsExtractor.TS_SYNC_BYTE) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void init(ExtractorOutput output) {
    tsExtractor.init(output);
  }

  @Override
  public void seek(long position, long timeUs) {
    tsExtractor.seek(position, timeUs);
    headerSkipped = false;
  }

  @Override
  public void release() {
    tsExtractor.release();
  }

  @Override
  public @ReadResult int read(ExtractorInput input, PositionHolder seekPosition)
      throws IOException {
    if (!headerSkipped) {
      input.skipFully(M2TS_HEADER_SIZE);
      headerSkipped = true;
    }
    return tsExtractor.read(input, seekPosition);
  }
}
