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

import com.terracottatech.frs.GettableAction;
import com.terracottatech.frs.GettableActionFactory;
import com.terracottatech.frs.PutAction;
import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionFactory;
import com.terracottatech.frs.compaction.Compactor;
import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.util.ByteBufferUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 * An interceptor that wraps incoming PutAction instances and encrypts their
 * values. This class implements the GettableAction interface and delegates most
 * operations to the wrapped PutAction, while handling encryption/decryption of
 * values.
 */
public class CipherPutAction implements GettableAction {
  /*
   * CipheredPutAction Header
   * 4 bytes - idByteCount
   * 4 bytes - keyByteCount
   * 4 bytes - ivByteCount
   * 4 bytes - valueByteCount
   * 8 bytes - invalidatedLsn
   */
  public static final long CIPHERED_PUT_ACTION_OVERHEAD = 24L;

  public static class CipherPutActionFactory implements GettableActionFactory,
      ActionFactory<ByteBuffer, ByteBuffer, ByteBuffer> {
    private final CipherManager cipherManager;

    public CipherPutActionFactory(CipherManager cipherManager) {
      this.cipherManager = cipherManager;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Action create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
        ActionCodec codec, ByteBuffer[] buffers) {
      int idLength = ByteBufferUtils.getInt(buffers);
      int keyLength = ByteBufferUtils.getInt(buffers);
      int ivLength = ByteBufferUtils.getInt(buffers);
      int valueLength = ByteBufferUtils.getInt(buffers);
      long invalidatedLsn = ByteBufferUtils.getLong(buffers);

      ByteBuffer id = ByteBufferUtils.getBytes(idLength, buffers);
      ByteBuffer key = ByteBufferUtils.getBytes(keyLength, buffers);
      ByteBuffer iv = ByteBufferUtils.getBytes(ivLength, buffers);
      ByteBuffer encryptedValue = ByteBufferUtils.getBytes(valueLength, buffers);

      // Decrypt the value
      ByteBuffer value = cipherManager.decrypt(encryptedValue, iv);

      // Create a PutAction with the decrypted value
      PutAction putAction = new PutAction(objectManager, null, id, key, value, invalidatedLsn);

      // Create an interceptor with the original encrypted value
      CipherPutAction cipherPutAction = new CipherPutAction(putAction, cipherManager);
      return cipherPutAction;
    }

    @Override
    public GettableAction create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
        Compactor compactor, ByteBuffer id, ByteBuffer key, ByteBuffer value, boolean isRecovering) {
      PutAction putAction = new PutAction(objectManager, compactor, id, key, value, isRecovering);
      CipherPutAction cipherPutAction = new CipherPutAction(putAction, cipherManager);
      return cipherPutAction;
    }
  }

  private static final int HEADER_SIZE = ByteBufferUtils.INT_SIZE * 4 + ByteBufferUtils.LONG_SIZE;

  private final PutAction delegate;
  private final CipherManager cipherManager;
  // buffer contains initialization vector which introduces randomness and
  // ensure same plaintext encrypts to different ciphertexts each time
  private final ByteBuffer iv;

  public CipherPutAction(PutAction delegate, CipherManager cipherManager) {
    this.delegate = delegate;
    this.cipherManager = cipherManager;
    this.iv = cipherManager.generateInitializationVector();
  }

  @Override
  public ByteBuffer getIdentifier() {
    return delegate.getIdentifier();
  }

  @Override
  public ByteBuffer getKey() {
    return delegate.getKey();
  }

  @Override
  public ByteBuffer getValue() {
    return delegate.getValue();
  }

  /**
   * Gets the initialization vector used for encryption.
   *
   * @return The initialization vector
   */
  public ByteBuffer getInitVector() {
    return iv;
  }

  /**
   * Gets the encrypted value.
   *
   * @return The encrypted value
   */
  public ByteBuffer getEncryptedValue() {
    return cipherManager.encrypt(getValue(), iv);
  }

  @Override
  public long getLsn() {
    return delegate.getLsn();
  }

  @Override
  public Set<Long> getInvalidatedLsns() {
    return delegate.getInvalidatedLsns();
  }

  @Override
  public void setDisposable(Closeable c) {
    delegate.setDisposable(c);
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void record(long lsn) {
    delegate.record(lsn);
  }

  @Override
  public void replay(long lsn) {
    delegate.replay(lsn);
  }

  @Override
  public int replayConcurrency() {
    return delegate.replayConcurrency();
  }

  @Override
  public ByteBuffer[] getPayload(ActionCodec codec) {
    // This is where we modify the payload to include the encrypted value
    // and initialization vector instead of the original value
    ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
    header.putInt(getIdentifier().remaining());
    header.putInt(getKey().remaining());
    header.putInt(iv.remaining());

    ByteBuffer encryptedValue = cipherManager.encrypt(getValue(), iv);
    header.putInt(encryptedValue.remaining());
    long lsn = getInvalidatedLsns().stream().findFirst().orElse(-1L);
    header.putLong(lsn).flip();

    return new ByteBuffer[] { header, getIdentifier().slice(), getKey().slice(), iv.slice(),
        encryptedValue.slice() };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CipherPutAction that = (CipherPutAction) o;
    return delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
