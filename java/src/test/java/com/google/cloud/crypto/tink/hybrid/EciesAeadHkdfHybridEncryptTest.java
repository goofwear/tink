// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.cloud.crypto.tink.hybrid;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import com.google.cloud.crypto.tink.AesCtrHmacAeadProto.AesCtrHmacAeadKey;
import com.google.cloud.crypto.tink.AesCtrHmacAeadProto.AesCtrHmacAeadKeyFormat;
import com.google.cloud.crypto.tink.AesGcmProto.AesGcmKeyFormat;
import com.google.cloud.crypto.tink.CommonProto.EllipticCurveType;
import com.google.cloud.crypto.tink.CommonProto.EcPointFormat;
import com.google.cloud.crypto.tink.HmacProto.HmacKey;
import com.google.cloud.crypto.tink.HmacProto.HmacKeyFormat;
import com.google.cloud.crypto.tink.HybridDecrypt;
import com.google.cloud.crypto.tink.HybridEncrypt;
import com.google.cloud.crypto.tink.TinkProto.KeyFormat;
import com.google.cloud.crypto.tink.Util;
import com.google.cloud.crypto.tink.aead.AeadFactory;
import com.google.protobuf.Any;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for EciesAeadHkdfHybridEncrypt.
 * TODO(przydatek): Add more tests.
 */
@RunWith(JUnit4.class)
public class EciesAeadHkdfHybridEncryptTest {
  private static final String PLAINTEXT = "Hello";
  private static final String CONTEXT = "context info";
  private static final int AES_GCM_KEY_SIZE = 16;

  @Before
  public void setUp() throws GeneralSecurityException {
    AeadFactory.registerStandardKeyTypes();
  }

  @Test
  public void testBasicAesGcm() throws Exception {
    ECParameterSpec spec = Util.getCurveSpec(EllipticCurveType.NIST_P256);
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(spec);
    KeyPair recipientKey = keyGen.generateKeyPair();
    ECPublicKey recipientPublicKey = (ECPublicKey) recipientKey.getPublic();
    ECPrivateKey recipientPrivateKey = (ECPrivateKey) recipientKey.getPrivate();
    byte[] salt = "some salt".getBytes();

    KeyFormat keyFormat = KeyFormat.newBuilder()
        .setKeyType("type.googleapis.com/google.cloud.crypto.tink.AesGcmKey")
        .setFormat(Any.pack(AesGcmKeyFormat.newBuilder().setKeySize(AES_GCM_KEY_SIZE).build()))
        .build();
    HybridEncrypt hybridEncrypt =
        new EciesAeadHkdfHybridEncrypt(recipientPublicKey, salt, keyFormat, EcPointFormat.UNCOMPRESSED);
    HybridDecrypt hybridDecrypt =
        new EciesAeadHkdfHybridDecrypt(recipientPrivateKey, salt, keyFormat, EcPointFormat.UNCOMPRESSED);

    byte[] ciphertext = hybridEncrypt.encrypt(PLAINTEXT.getBytes(), CONTEXT.getBytes());
    byte[] decrypted = hybridDecrypt.decrypt(ciphertext, CONTEXT.getBytes());

    assertFalse(PLAINTEXT.equals(new String(ciphertext)));
    assertEquals(PLAINTEXT, new String(decrypted));
  }
}
