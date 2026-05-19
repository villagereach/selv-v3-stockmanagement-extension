/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.stockmanagement.extension.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class SecurityContextsTest {

  @Test
  public void shouldReturnContextWhoseAuthenticationIsClientOnly() {
    SecurityContext context = SecurityContexts.clientOnly("cce-capacity");

    assertNotNull(context.getAuthentication());
    assertTrue(context.getAuthentication() instanceof OAuth2Authentication);
    assertTrue(((OAuth2Authentication) context.getAuthentication()).isClientOnly());
  }

  @Test
  public void shouldPreserveProvidedClientIdOnTheRequest() {
    SecurityContext context = SecurityContexts.clientOnly("my-client");

    OAuth2Authentication auth = (OAuth2Authentication) context.getAuthentication();
    assertEquals("my-client", auth.getOAuth2Request().getClientId());
  }

  @Test
  public void shouldReturnFreshContextInstanceOnEachCall() {
    SecurityContext first = SecurityContexts.clientOnly("a");
    SecurityContext second = SecurityContexts.clientOnly("a");

    assertNotNull(first);
    assertNotNull(second);
    assertEquals(false, first == second);
  }
}
