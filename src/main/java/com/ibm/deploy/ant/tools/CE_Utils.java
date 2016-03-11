package com.ibm.deploy.ant.tools;

import com.filenet.api.admin.LocalizedString;
import com.filenet.api.collection.LocalizedStringList;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;

public class CE_Utils {

	/**
	 * Gets a localized string.
	 *
	 * @param value the name
	 * @return the localized string
	 */
	public static LocalizedString getLocalizedString(ObjectStore objectStore, String value) {
		LocalizedString localizedString = Factory.LocalizedString.createInstance();
		localizedString.set_LocalizedText(value);
		objectStore.refresh( new String[] { PropertyNames.LOCALE_NAME } );
		localizedString.set_LocaleName(objectStore.get_LocaleName());
		return localizedString;
	}
	
	/**
	 * Gets a localized string and adds it to a list.
	 *
	 * @param value the name
	 * @return the localized string list
	 */
	@SuppressWarnings("unchecked")
	public static LocalizedStringList getLocalizedStringList(ObjectStore objectStore, String value) {
		LocalizedStringList localizedStringList = Factory.LocalizedString.createList();
		localizedStringList.add(getLocalizedString(objectStore, value));
		return localizedStringList;
	}	
}
