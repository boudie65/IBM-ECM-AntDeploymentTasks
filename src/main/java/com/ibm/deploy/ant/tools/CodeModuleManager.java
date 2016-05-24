package com.ibm.deploy.ant.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;

import org.apache.tools.ant.Task;

import com.filenet.api.admin.CodeModule;
import com.filenet.api.collection.ActionSet;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.ClassNames;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.events.Action;
import com.filenet.api.exception.ExceptionCode;
import com.ibm.deploy.ant.task.ce.ImportCodeModuleTask;

// TODO: Auto-generated Javadoc
/**
 * The Class CodeModuleManager.
 */
public class CodeModuleManager {

	/** The object store. */
	private final ObjectStore objectStore;

	/** The task. */
	private final Task task;

	/**
	 * Instantiates a new code module manager.
	 *
	 * @param objectStore
	 *            the object store
	 * @param importCodeModuleTask
	 *            the import code module task
	 */
	public CodeModuleManager(ObjectStore objectStore, ImportCodeModuleTask importCodeModuleTask) {
		this.objectStore = objectStore;
		this.task = importCodeModuleTask;
	}

	/**
	 * Creates the or update.
	 *
	 * @param name
	 *            the name
	 * @param path
	 *            the path
	 * @param files
	 *            the files
	 * @param updateReferencingActions
	 *            the update referencing actions
	 * @return the code module
	 */
	public CodeModule createOrUpdate(String name, String path, Collection<File> files,
			boolean updateReferencingActions) {
		CodeModule codeModule = getCodeModule(name, path);
		if (codeModule != null) {
			updateCodeModule(files, codeModule);

			codeModule.refresh();

			if (updateReferencingActions) {
				updateReferencingActions(codeModule);
			}
			return codeModule;
		} else {
			return createCodeModule(name, path, files);
		}
	}

	/**
	 * Update referencing actions.
	 *
	 * @param codeModule the code module
	 */
	private void updateReferencingActions(CodeModule codeModule) {
		if (!codeModule.get_IsCurrentVersion()) {
			// implicit refresh
			codeModule = (CodeModule) codeModule.get_CurrentVersion();
		}

		// Get all referenced actions
		ActionSet actionSet = codeModule.get_ReferencingActions();
		@SuppressWarnings("rawtypes")
		Iterator iterator = actionSet.iterator();

		// Loop all actions and update codemodule reference
		while (iterator.hasNext()) {
			Action act = (Action) iterator.next();
			act.set_CodeModule(codeModule);
			act.save(RefreshMode.NO_REFRESH);
		}
	}

	/**
	 * Creates the code module.
	 *
	 * @param name
	 *            the name
	 * @param path
	 *            the path
	 * @param files
	 *            the files
	 * @return the code module
	 */
	private CodeModule createCodeModule(String name, String path, Collection<File> files) {

		// create CodeModule
		CodeModule codeModule = Factory.CodeModule.createInstance(objectStore, ClassNames.CODE_MODULE);
		codeModule.getProperties().putValue("DocumentTitle", name);
		codeModule.set_ContentElements(createContentElements(files));
		codeModule.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
		codeModule.save(RefreshMode.REFRESH);

		// File Codemodule in /CodeModules path
		if (path != null && !path.isEmpty()) {
			Folder folder = Factory.Folder.getInstance(objectStore, ClassNames.FOLDER, path);
			folder.file(codeModule, AutoUniqueName.AUTO_UNIQUE, null,
					DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE).save(RefreshMode.NO_REFRESH);
		}
		return codeModule;

	}

	/**
	 * Gets the code module.
	 *
	 * @param name
	 *            the name
	 * @param path
	 *            the path
	 * @return the code module
	 */
	public CodeModule getCodeModule(String name, String path) {
		try {
			CodeModule codeModule = Factory.CodeModule.getInstance(objectStore, "/" + path + "/" + name);
			codeModule.refresh();
			return codeModule;
		} catch (com.filenet.api.exception.EngineRuntimeException e) {
			if (e.getExceptionCode() == ExceptionCode.E_OBJECT_NOT_FOUND) {
				return null;
			}
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Update code module.
	 *
	 * @param files
	 *            the files
	 * @param codeModule
	 *            the code module
	 */
	private void updateCodeModule(Collection<File> files, CodeModule codeModule) {
		task.log("Updating existing code module");

		// Find codemodules current version
		VersionSeries versionSeries = codeModule.get_VersionSeries();
		versionSeries.fetchProperties(new String[] { PropertyNames.CURRENT_VERSION });
		Document document = (Document) versionSeries.get_CurrentVersion();

		// Do a checkout
		document.checkout(ReservationType.EXCLUSIVE, null, document.getClassName(), null);
		document.save(RefreshMode.REFRESH);
		Document reservation = (Document) document.get_Reservation();

		// Add new JAR files
		ContentElementList contentElementList = createContentElements(files);
		reservation.set_ContentElements(contentElementList);

		// Check in new version
		reservation.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
		reservation.save(RefreshMode.REFRESH);
	}

	/**
	 * Creates the content elements.
	 *
	 * @param files the files
	 * @return the content element list
	 */
	@SuppressWarnings("unchecked")
	private ContentElementList createContentElements(Collection<File> files) {
		ContentElementList cel = Factory.ContentElement.createList();
		for (File file : files) {
			if (file.exists()) {
				ContentTransfer ct = createContentTransferFromFile(file);
				cel.add(ct);
			} else {
				task.log("File '" + file.getName() + "' not found");
			}
		}
		return cel;
	}

	/**
	 * Creates the content transfer from file.
	 *
	 * @param file the file
	 * @return the content transfer
	 */
	private ContentTransfer createContentTransferFromFile(File file) {
		ContentTransfer content = Factory.ContentTransfer.createInstance();
		content.set_RetrievalName(file.getName());
		try {
			content.setCaptureSource(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			// Only existing files passed to this method...
		}

		try {
			content.set_ContentType(Files.probeContentType(file.toPath()));
		} catch (IOException e) {
			// Should never occur
			e.printStackTrace();
		}
		return content;
	}

}
