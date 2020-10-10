package de.metas.currency.exceptions;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
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

import de.metas.currency.CurrencyCode;
import de.metas.currency.CurrencyConversionContext;
import de.metas.i18n.AdMessageKey;
import de.metas.i18n.ITranslatableString;
import de.metas.i18n.TranslatableStrings;
import org.adempiere.exceptions.AdempiereException;

/**
 * Exception thrown when there was no currency rate found.
 *
 * @author metas-dev <dev@metasfresh.com>
 */
public class NoCurrencyRateFoundException extends AdempiereException
{
	private static final AdMessageKey MSG = AdMessageKey.of("NoCurrencyConversion");

	public NoCurrencyRateFoundException(
			final CurrencyConversionContext conversionCtx,
			final CurrencyCode currencyFrom,
			final CurrencyCode currencyTo)
	{
		super(buildMsg(conversionCtx, currencyFrom, currencyTo));
	}

	private static ITranslatableString buildMsg(
			final CurrencyConversionContext conversionCtx,
			final CurrencyCode currencyFrom,
			final CurrencyCode currencyTo)
	{
		return TranslatableStrings.builder()
				.appendADMessage(MSG).append(" ")
				.appendObj(currencyFrom).appendObj("->").appendObj(currencyTo)
				.append(" (").appendObj(conversionCtx).appendObj(")")
				.build();
	}
}
