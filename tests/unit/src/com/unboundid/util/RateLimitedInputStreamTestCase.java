/*
 * Copyright 2018-2021 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright 2018-2021 Ping Identity Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2018-2021 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.util;



import java.io.ByteArrayInputStream;

import org.testng.annotations.Test;

import com.unboundid.ldap.sdk.LDAPSDKTestCase;



/**
 * This class provides a set of test cases for the rate-limited input stream.
 */
public final class RateLimitedInputStreamTestCase
       extends LDAPSDKTestCase
{
  /**
   * Test the output stream with a tiny limit.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testWithTinyLimit()
         throws Exception
  {
    final ByteArrayInputStream wrappedStream =
         new ByteArrayInputStream(new byte[] { 0x00, 0x01, 0x02 });
    final RateLimitedInputStream inputStream =
         new RateLimitedInputStream(wrappedStream, 1);

    inputStream.available();

    assertTrue(inputStream.markSupported());
    inputStream.mark(3);
    inputStream.reset();

    final long startTime = System.currentTimeMillis();
    assertEquals(inputStream.read(), 0x00);

    final byte[] buffer = new byte[8192];
    assertEquals(inputStream.read(buffer), 2);
    assertEquals(buffer[0], (byte) 0x01);
    assertEquals(buffer[1], (byte) 0x02);

    assertEquals(inputStream.read(), -1);

    inputStream.close();

    final long elapsedTimeMillis = System.currentTimeMillis() - startTime;
    assertTrue(elapsedTimeMillis >= 2000L);
  }



  /**
   * Test the output stream with a big limit.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testWithBigLimit()
         throws Exception
  {
    final byte[] array = new byte[1_048_579];
    array[1] = (byte) 0x01;
    array[2] = (byte) 0x02;

    final ByteArrayInputStream wrappedStream = new ByteArrayInputStream(array);
    final RateLimitedInputStream inputStream =
         new RateLimitedInputStream(wrappedStream, 10_485_760);

    inputStream.available();

    assertTrue(inputStream.markSupported());
    inputStream.mark(3);
    inputStream.reset();

    final long startTime = System.currentTimeMillis();
    assertEquals(inputStream.read(), 0x00);

    assertEquals(inputStream.read(StaticUtils.NO_BYTES), 0);

    final byte[] buffer = new byte[8192];
    assertEquals(inputStream.read(buffer, 0, 2), 2);
    assertEquals(buffer[0], (byte) 0x01);
    assertEquals(buffer[1], (byte) 0x02);

    int totalBytesRead = 3;
    while (true)
    {
      final int bytesRead = inputStream.read(buffer);
      if (bytesRead < 0)
      {
        break;
      }

      totalBytesRead += bytesRead;
    }

    inputStream.close();

    final long elapsedTimeMillis = System.currentTimeMillis() - startTime;
    assertTrue(elapsedTimeMillis <= 10_000L);

    assertEquals(totalBytesRead, array.length);
  }
}
