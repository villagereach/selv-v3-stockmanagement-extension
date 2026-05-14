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

import java.util.Collections;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

public final class SecurityContexts {

  private SecurityContexts() {
    throw new UnsupportedOperationException();
  }

  /**
   * Builds a fresh {@link SecurityContext} carrying a client-only
   * {@link OAuth2Authentication}.
   *
   * <p>"Client-only" means {@code userAuthentication} is {@code null}, so
   * {@link OAuth2Authentication#isClientOnly()} returns {@code true}. Upstream stockmanagement
   * uses that flag to gate per-program permission checks (e.g. {@code STOCK_CARDS_VIEW}):
   * calls made with a client-only authentication skip the check, exactly the same path used
   * for service-to-service communication. Use this when an in-process call must operate
   * independently of which user is logged in.
   *
   * @param clientId identifier embedded in the inner {@link OAuth2Request}; surfaces in
   *                 audit / log lines so the elevated call can be traced back to the caller.
   * @return a SecurityContext with a client-only authentication; callers are expected to
   *         restore the original context after use (typically via {@code try/finally}).
   */
  public static SecurityContext clientOnly(String clientId) {
    OAuth2Request request = new OAuth2Request(
        Collections.emptyMap(),
        clientId,
        Collections.emptyList(),
        true,
        Collections.emptySet(),
        Collections.emptySet(),
        null,
        null,
        Collections.emptyMap());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new OAuth2Authentication(request, null));
    return context;
  }
}
