package org.adempiere.ad.trx.spi;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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
 * Invoke {@link ITrxManager#registerTrxCustomizer(ITrxCustomizer)} to have the respective customer be invoked every time a new transaction is created.
 * Note that this is different from {@link ITrxListener} in that it affects <b>every</b> single transaction that is newly created.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public interface ITrxCustomizer
{
	void onTrxCreated(ITrx trx);
}
