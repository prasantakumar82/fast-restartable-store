/*
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracottatech.frs.cipher;

import com.terracottatech.frs.PutAction;
import com.terracottatech.frs.compaction.CompactionAction;
import com.terracottatech.frs.compaction.CompactionActionFactory;
import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.object.ObjectManagerEntry;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Encrypted implementation of CompactionActionFactory that creates
 * CompactionAction objects with CipherPutAction delegates.
 */
public class CipherCompactionActionFactory implements CompactionActionFactory {

  private final CipherManager cipherManager;

  public CipherCompactionActionFactory(CipherManager cipherManager) {
    this.cipherManager = Objects.requireNonNull(cipherManager, "CipherManager cannot be null");
  }

  @Override
  public CompactionAction create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
      ObjectManagerEntry<ByteBuffer, ByteBuffer, ByteBuffer> entry) {
    Objects.requireNonNull(entry, "Entry cannot be null");

    PutAction putAction = new PutAction(objectManager, null, entry.getId(), entry.getKey(),
        entry.getValue(), entry.getLsn());
    CipherPutAction delegate = new CipherPutAction(putAction, cipherManager);
    return new CipherCompactionAction(objectManager, entry, delegate);
  }
}
