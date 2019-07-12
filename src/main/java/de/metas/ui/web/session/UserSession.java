package de.metas.ui.web.session;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import org.adempiere.service.ClientId;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.service.OrgId;
import org.compiere.Adempiere;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.compiere.util.Evaluatees;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import de.metas.i18n.Language;
import de.metas.logging.LogManager;
import de.metas.security.IUserRolePermissions;
import de.metas.security.RoleId;
import de.metas.security.UserRolePermissionsKey;
import de.metas.ui.web.base.session.UserPreference;
import de.metas.ui.web.exceptions.DeprecatedRestAPINotAllowedException;
import de.metas.ui.web.login.exceptions.AlreadyLoggedInException;
import de.metas.ui.web.login.exceptions.NotLoggedInAsSysAdminException;
import de.metas.ui.web.login.exceptions.NotLoggedInException;
import de.metas.ui.web.websocket.WebSocketConfig;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
import de.metas.user.UserId;
import de.metas.util.Check;
import de.metas.util.Services;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * User Session service
 *
 * @author metas-dev <dev@metasfresh.com>
 */
@Service
public class UserSession
{
	/**
	 * Gets current {@link UserSession} if any
	 *
	 * NOTE: please use this method only if there is no other way to get the {@link UserSession}
	 *
	 * @return {@link UserSession} or null
	 */
	public static UserSession getCurrentOrNull()
	{
		//
		// Quickly check if the session scoped UserSession bean will be really available
		// NOTE: it's not about that the object will be null but if it's method calls will be really working
		if (!isWebuiThread())
		{
			return null;
		}

		//
		UserSession userSession = _staticUserSession;
		if (userSession == null)
		{
			synchronized (UserSession.class)
			{
				if (_staticUserSession == null)
				{
					userSession = _staticUserSession = Adempiere.getSpringApplicationContext().getBean(UserSession.class);
				}
			}
		}
		return userSession;
	}

	public static UserSession getCurrentIfMatchingOrNull(final UserId adUserId)
	{
		final UserSession userSession = getCurrentOrNull();
		if (userSession == null)
		{
			return null;
		}
		if (!UserId.equals(userSession.getLoggedUserId(), adUserId))
		{
			return null;
		}

		return userSession;
	}

	/**
	 * Gets current {@link UserSession}.
	 *
	 * NOTE: please use this method only if there is no other way to get the {@link UserSession}
	 *
	 * @return user session; never returns null
	 * @throws NotLoggedInException
	 */
	public static UserSession getCurrent() throws NotLoggedInException
	{
		final UserSession userSession = getCurrentOrNull();
		if (userSession == null)
		{
			throw new NotLoggedInException("no session found");
		}
		return userSession;
	}

	/**
	 * Gets current permissions.
	 *
	 * @return permissions; never returns null
	 * @throws NotLoggedInException
	 */
	public static IUserRolePermissions getCurrentPermissions()
	{
		return getCurrent().getUserRolePermissions();
	}

	/** @return true if we are running in a webui thread (i.e. NOT a background daemon thread) */
	public static boolean isWebuiThread()
	{
		return RequestContextHolder.getRequestAttributes() != null;
	}

	// services
	static final transient Logger logger = LogManager.getLogger(UserSession.class);
	private final transient ApplicationEventPublisher eventPublisher;

	private static UserSession _staticUserSession = null;

	@Autowired
	private InternalUserSessionData _data; // session scoped

	@Autowired
	public UserSession(final ApplicationEventPublisher eventPublisher)
	{
		this.eventPublisher = eventPublisher;
	}

	private InternalUserSessionData getData()
	{
		_data.initializeIfNeeded();
		return _data;
	}

	public String getSessionId()
	{
		return getData().getSessionId();
	}

	/**
	 * Never call it directly. Consider calling {@link Env#getCtx()}.
	 * 
	 * @return effective session context
	 */
	Properties getCtx()
	{
		return getData().getCtx();
	}

	public Evaluatee toEvaluatee()
	{
		return Evaluatees.ofCtx(getData().getCtx());
	}

	public UserPreference getUserPreference()
	{
		return getData().getUserPreference();
	}

	public boolean isLoggedIn()
	{
		return getData().isLoggedIn();
	}

	public void setLoggedIn(final boolean loggedIn)
	{
		final InternalUserSessionData data = getData();
		final boolean currentlyLoggedIn = data.isLoggedIn();
		if (currentlyLoggedIn == loggedIn)
		{
			return;
		}

		if (loggedIn)
		{
			data.setLoggedIn(true);
			data.getUserPreference().loadPreference(data.getCtx());
			logger.trace("User session logged in: {}", data);
		}
		else
		{
			data.setLoggedIn(false);
			data.setUserPreference(new UserPreference());
			logger.trace("User session logged out: {}", data);
		}
	}

	public void assertLoggedIn()
	{
		if (!getData().isLoggedIn())
		{
			throw new NotLoggedInException();
		}
	}

	public void assertLoggedInAsSysAdmin()
	{
		assertLoggedIn();

		final RoleId adRoleId = getData().getLoggedRoleId();
		if (!adRoleId.isSystem())
		{
			throw new NotLoggedInAsSysAdminException();
		}
	}

	public void assertNotLoggedIn()
	{
		if (getData().isLoggedIn())
		{
			throw new AlreadyLoggedInException();
		}
	}

	/**
	 * Sets user preferred language.
	 *
	 * Fires {@link LanguagedChangedEvent}.
	 *
	 * @param adLanguage
	 * @return old AD_Language
	 */
	public String setAD_Language(final String adLanguage)
	{
		Check.assumeNotEmpty(adLanguage, "adLanguage is not empty");
		final Language lang = Language.getLanguage(adLanguage);
		final InternalUserSessionData data = getData();
		final String adLanguageOld = data.verifyLanguageAndSet(lang);
		final String adLanguageNew = data.getAdLanguage();
		logger.info("Changed AD_Language: {} -> {}, {}", adLanguageOld, adLanguageNew, lang);

		// Fire event
		if (!Objects.equals(adLanguageOld, adLanguageNew))
		{
			eventPublisher.publishEvent(new LanguagedChangedEvent(adLanguageNew, getLoggedUserId()));
		}

		return adLanguageOld;
	}

	public ClientId getClientId()
	{
		return getData().getClientId();
	}

	public OrgId getOrgId()
	{
		return getData().getOrgId();
	}

	public String getAD_Language()
	{
		return getData().getAdLanguage();
	}

	public Language getLanguage()
	{
		return getData().getLanguage();
	}

	public JSONLookupValue getLanguageAsJson()
	{
		final Language language = getLanguage();
		return JSONLookupValue.of(language.getAD_Language(), language.getName());
	}

	public Locale getLocale()
	{
		return getData().getLocale();
	}

	public UserSessionLocale getUserSessionLocale()
	{
		return UserSessionLocale.get(getAD_Language());
	}

	public boolean isUseHttpAcceptLanguage()
	{
		return getData().isUseHttpAcceptLanguage();
	}

	public void setUseHttpAcceptLanguage(final boolean useHttpAcceptLanguage)
	{
		final InternalUserSessionData data = getData();
		final boolean useHttpAcceptLanguageOld = data.isUseHttpAcceptLanguage();
		data.setUseHttpAcceptLanguage(useHttpAcceptLanguage);
		logSettingChanged("UseHttpAcceptLanguage", useHttpAcceptLanguage, useHttpAcceptLanguageOld);
	}

	public UserId getLoggedUserId()
	{
		return getData().getLoggedUserId();
	}

	public boolean isLoggedInAs(@NonNull final UserId userId)
	{
		return isLoggedIn()
				&& UserId.equals(getLoggedUserId(), userId);
	}

	public String getUserName()
	{
		return getData().getUserName();
	}

	public String getRoleName()
	{
		return getData().getRoleName();
	}

	public UserRolePermissionsKey getUserRolePermissionsKey()
	{
		// TODO: cache the permissions key
		return UserRolePermissionsKey.fromContext(getData().getCtx());
	}

	public IUserRolePermissions getUserRolePermissions()
	{
		return Env.getUserRolePermissions(getData().getCtx());
	}

	public String getAvatarId()
	{
		return getData().getAvatarId();
	}

	public String getUserEmail()
	{
		return getData().getUserEmail();
	}

	public String getUserFullname()
	{
		return getData().getUserFullname();
	}

	String setAvatarId(final String avatarId)
	{
		final InternalUserSessionData data = getData();
		final String avatarIdOld = data.getAvatarId();
		data.setAvatarId(avatarId);
		return avatarIdOld;
	}

	String setUserEmail(final String userEmail)
	{
		final InternalUserSessionData data = getData();
		final String userEmailOld = data.getUserEmail();
		data.setUserEmail(userEmail);
		return userEmailOld;
	}

	String setUserFullname(final String userFullname)
	{
		final InternalUserSessionData data = getData();
		final String userFullnameOld = data.getUserFullname();
		data.setUserFullname(userFullname);
		return userFullnameOld;
	}

	/** @return websocket notifications endpoint on which the frontend shall listen */
	public String getWebsocketEndpoint()
	{
		return WebSocketConfig.buildUserSessionTopicName(getLoggedUserId());
	}

	public void assertDeprecatedRestAPIAllowed()
	{
		if (!getData().isAllowDeprecatedRestAPI())
		{
			throw new DeprecatedRestAPINotAllowedException();
		}
	}

	private static final void logSettingChanged(final String name, final Object value, final Object valueOld)
	{
		UserSession.logger.warn("/*********************************************************************************************\\");
		UserSession.logger.warn("Setting changed: {} = {} (Old: {})", name, value, valueOld);
		UserSession.logger.warn("\\*********************************************************************************************/");
	}

	public boolean isAllowDeprecatedRestAPI()
	{
		return getData().isAllowDeprecatedRestAPI();
	}

	public void setAllowDeprecatedRestAPI(final boolean allowDeprecatedRestAPI)
	{
		final InternalUserSessionData data = getData();
		final boolean allowDeprecatedRestAPIOld = data.isAllowDeprecatedRestAPI();
		data.setAllowDeprecatedRestAPI(allowDeprecatedRestAPI);
		logSettingChanged("AllowDeprecatedRestAPI", allowDeprecatedRestAPI, allowDeprecatedRestAPIOld);
	}

	public boolean isShowColumnNamesForCaption()
	{
		return getData().isShowColumnNamesForCaption();
	}

	public void setShowColumnNamesForCaption(final boolean showColumnNamesForCaption)
	{
		final InternalUserSessionData data = getData();
		final boolean showColumnNamesForCaptionOld = data.isShowColumnNamesForCaption();
		data.setShowColumnNamesForCaption(showColumnNamesForCaption);
		logSettingChanged("ShowColumnNamesForCaption", showColumnNamesForCaption, showColumnNamesForCaptionOld);
	}

	public void setHttpCacheMaxAge(final int httpCacheMaxAge)
	{
		final InternalUserSessionData data = getData();
		final int httpCacheMaxAgeOld = data.getHttpCacheMaxAge();
		data.setHttpCacheMaxAge(httpCacheMaxAge);
		logSettingChanged("HttpCacheMaxAge", httpCacheMaxAge, httpCacheMaxAgeOld);
	}

	public int getHttpCacheMaxAge()
	{
		return getData().getHttpCacheMaxAge();
	}

	private static final String SYSCONFIG_DefaultLookupSearchStartDelayMillis = "de.metas.ui.web.window.descriptor.LookupDescriptor.DefaultLookupSearchStartDelayMillis";

	public Supplier<Duration> getDefaultLookupSearchStartDelay()
	{
		return () -> {
			final int defaultLookupSearchStartDelayMillis = Services.get(ISysConfigBL.class).getIntValue(SYSCONFIG_DefaultLookupSearchStartDelayMillis, 0);
			return defaultLookupSearchStartDelayMillis > 0 ? Duration.ofMillis(defaultLookupSearchStartDelayMillis) : Duration.ZERO;
		};
	}

	@NonNull
	public ZoneId getTimeZone()
	{
		// ZoneId.getAvailableZoneIds()
		return ZoneId.of("Etc/GMT+9");
		// return ZoneId.systemDefault(); // TODO: get it from organization
	}

	/**
	 * 
	 * @deprecated avoid using this method; usually it's workaround-ish / quick and dirty fix
	 */
	@Deprecated
	public static ZoneId getTimeZoneOrSystemDefault()
	{
		final UserSession userSession = getCurrentOrNull();
		return userSession != null ? userSession.getTimeZone() : ZoneId.systemDefault();
	}

	/**
	 * Event fired when the user language was changed.
	 * Usually it is user triggered.
	 *
	 * @author metas-dev <dev@metasfresh.com>
	 *
	 */
	@lombok.Value
	public static class LanguagedChangedEvent
	{
		@NonNull
		private final String adLanguage;
		private final UserId adUserId;
	}
}
