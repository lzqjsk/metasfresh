package de.metas.banking.payment.paymentallocation;

import java.time.ZonedDateTime;

import javax.annotation.Nullable;

import org.adempiere.service.ClientId;

import com.google.common.collect.ImmutableSet;

import de.metas.bpartner.BPartnerId;
import de.metas.invoice.InvoiceId;
import de.metas.money.CurrencyId;
import de.metas.order.OrderId;
import de.metas.organization.OrgId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/*
 * #%L
 * de.metas.banking.base
 * %%
 * Copyright (C) 2019 metas GmbH
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

@Value
@Builder(toBuilder = true)
public class InvoiceToAllocateQuery
{
	@NonNull
	BPartnerId bpartnerId;

	@Nullable
	CurrencyId currencyId;

	@NonNull
	ClientId adClientId;

	@NonNull
	OrgId orgId;

	@NonNull
	ZonedDateTime date;

	@Nullable
	String poReference;

	@NonNull
	@Singular
	ImmutableSet<InvoiceId> considerOnlyInvoiceIds;

	@NonNull
	@Singular("additionalInvoiceIdToInclude")
	ImmutableSet<InvoiceId> additionalInvoiceIdsToInclude;

	@NonNull
	@Singular
	ImmutableSet<InvoiceId> excludeInvoiceIds;

	@NonNull
	@Singular("additionalPrepayOrderIdToInclude")
	ImmutableSet<OrderId> additionalPrepayOrderIdsToInclude;
}
