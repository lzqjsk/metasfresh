package de.metas.ui.web.dataentry.interceptor;

import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.compiere.model.ModelValidator;
import org.springframework.stereotype.Component;

import de.metas.dataentry.model.I_DataEntry_Field;
import de.metas.dataentry.model.I_DataEntry_Group;
import de.metas.dataentry.model.I_DataEntry_SubGroup;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.ui.web.window.descriptor.factory.DocumentDescriptorFactory;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
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

@Component("de.metas.ui.web.dataentry.interceptor.DataEntry_Field")
@Interceptor(I_DataEntry_Field.class)
public class DataEntry_Field
{
	private DocumentDescriptorFactory documentDescriptorFactory;

	public DataEntry_Field(@NonNull final DocumentDescriptorFactory documentDescriptorFactory)
	{
		this.documentDescriptorFactory = documentDescriptorFactory;
	}

	@ModelChange(timings = { ModelValidator.TYPE_AFTER_NEW, ModelValidator.TYPE_AFTER_CHANGE, ModelValidator.TYPE_BEFORE_DELETE })
	public void invalidateDocumentDescriptorCache(@NonNull final I_DataEntry_Field dataEntryFieldRecord)
	{
		if (dataEntryFieldRecord.getDataEntry_SubGroup_ID() <= 0)
		{
			return;
		}
		final I_DataEntry_SubGroup dataEntrySubGroupRecord = dataEntryFieldRecord.getDataEntry_SubGroup();

		if (dataEntrySubGroupRecord.getDataEntry_Group_ID() <= 0)
		{
			return;
		}
		final I_DataEntry_Group dataEntryGroupRecord = dataEntrySubGroupRecord.getDataEntry_Group();

		final int windowId = dataEntryGroupRecord.getDataEntry_TargetWindow_ID();
		if (windowId <= 0)
		{
			return;
		}
		documentDescriptorFactory.invalidateForWindow(WindowId.of(windowId));
	}
}
