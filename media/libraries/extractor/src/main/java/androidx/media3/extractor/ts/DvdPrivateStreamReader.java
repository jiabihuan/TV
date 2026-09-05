/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import androidx.media3.common.C;
import androidx.media3.common.ParserException;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * Parses DVD private stream 1 (audio) data.
 */
public class DvdPrivateStreamReader implements ElementaryStreamReader {

  private final TrackOutput output;
  private @MonotonicNonNull TimestampAdjuster timestampAdjuster;

  public DvdPrivateStreamReader(TrackOutput output) {
    this.output = output;
  }

  @Override
  public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
    // No-op
  }

  @Override
  public void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput,
      TrackIdGenerator idGenerator) {
    this.timestampAdjuster = timestampAdjuster;
  }

  @Override
  public void consume(ParsableByteArray data, int flags) throws ParserException {
    // No-op by default
  }

  @Override
  public void packetStarted(long timeUs, int flags) {
    // No-op
  }

  @Override
  public void packetFinished() {
    // No-op
  }

  @Override
  public void endOfInputReached() {
    // No-op
  }

  @Override
  public void seek() {
    // No-op
  }
}
