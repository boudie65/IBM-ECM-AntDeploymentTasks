package com.ibm.deploy.ant.task.ce;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.filenet.api.admin.PropertyTemplate;
import com.filenet.api.admin.PropertyTemplateDateTime;
import com.filenet.api.admin.PropertyTemplateInteger32;
import com.filenet.api.admin.PropertyTemplateString;
import com.filenet.api.collection.PropertyTemplateSet;
import com.filenet.api.constants.Cardinality;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.ibm.deploy.ant.tools.CE_Utils;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class ImportClassTask.
 *
 * @author pvdhorst
 */
public class ImportPropertyTemplateTask extends ObjectStoreNestedTask{

	/** The display name. */
	@Getter @Setter private String displayName;
	
	/** The symbolic name. */
	@Getter @Setter private String symbolicName;

	/** The property type. */
	@Getter @Setter private String propertyType;
	
	/** The description. */
	@Getter @Setter private String description = "";

	/** The cardinality. */
	@Getter private Cardinality cardinality;
	
	/** The requires unique elements. */
	@Getter @Setter private Boolean requiresUniqueElements;

	/** 
	 * String specific fields
	 * TODO maybe refactor into different Actions?
	 */

	@Getter @Setter private int maximumStringLength;
	@Getter @Setter private String defaultString;

	/** 
	 * DateTime specific fields
	 * TODO maybe refactor into different Actions?
	 */
	@Getter private Date defaultDateTime;
	@Getter private Date maximumDateTime;
	@Getter private Date minimumDateTime;


	/** 
	 * Integer specific fields
	 * TODO maybe refactor into different Actions?
	 */
	@Getter @Setter private int maximumInteger;
	@Getter @Setter private int minimumInteger;

	/**
     * Execute.
     *
     * @throws BuildException the build exception
     */
	public void execute() throws BuildException {
        validateInput();

        PropertyTemplate template = getExistingPropertyTemplate(getSymbolicName());
        
        if(template == null)
        {
            this.log("Creating PropertyTemplate '" +getSymbolicName() + "'", Project.MSG_INFO);
            //Binary
            //Boolean
            //DateTime
            if(getPropertyType().equalsIgnoreCase("datetime"))
            {
                this.log("DateTime PropertyTemplate", Project.MSG_VERBOSE);
            	PropertyTemplateDateTime templateDateTime = Factory.PropertyTemplateDateTime.createInstance(getObjectStore());
            	templateDateTime.set_PropertyMinimumDateTime(getMinimumDateTime());
            	templateDateTime.set_PropertyMaximumDateTime(getMaximumDateTime());
            	templateDateTime.set_PropertyDefaultDateTime(getDefaultDateTime());
            	template = templateDateTime;
            }
            //Integer32
            else if(getPropertyType().equalsIgnoreCase("integer"))
            {
                this.log("Integer PropertyTemplate", Project.MSG_VERBOSE);
            	PropertyTemplateInteger32 templateInteger = Factory.PropertyTemplateInteger32.createInstance(getObjectStore());
            	templateInteger.set_PropertyMinimumInteger32(getMinimumInteger());
            	templateInteger.set_PropertyMaximumInteger32(getMaximumInteger());
            	template = templateInteger;
            }
            //Float64
            //String

            else if(getPropertyType().equalsIgnoreCase("string"))
            {
                this.log("String PropertyTemplate", Project.MSG_VERBOSE);
            	PropertyTemplateString templateString = Factory.PropertyTemplateString.createInstance(getObjectStore());
            	templateString.set_PropertyDefaultString(getDefaultString());
            	templateString.set_MaximumLengthString(getMaximumStringLength());
            	template = templateString;
            }
        	
            template.set_SymbolicName(getSymbolicName());
            template.set_Cardinality(getCardinality());
            if(getCardinality().equals(Cardinality.LIST) || getCardinality().equals(Cardinality.ENUM))
            {
                this.log("Creating MutliValue PropertyTemplate", Project.MSG_VERBOSE);
            	template.set_RequiresUniqueElements(getRequiresUniqueElements());
            }
        }
        
        
        //Id
        //Object
        
        this.log("Setting localized DisplayNames and Descriptions", Project.MSG_VERBOSE);
        template.set_DisplayNames(CE_Utils.getLocalizedStringList(getObjectStore(), getDisplayName()));
        template.set_DescriptiveTexts(CE_Utils.getLocalizedStringList(getObjectStore(), getDescription()));

        this.log("Saving template", Project.MSG_VERBOSE);
        template.save(RefreshMode.REFRESH);
        getObjectStore().refresh();
    }
	
    /**
     * Gets the existing property template.
     *
     * @param symbolicName the symbolic name
     * @return the existing property template
     */
    private PropertyTemplate getExistingPropertyTemplate(String symbolicName) {
        PropertyTemplate returnValue = null;
        
        this.log("Searching for existing PropertyTemplate", Project.MSG_VERBOSE);
        
        //Since we are not sure which tasks have been defined, retrieve all properties!
        ObjectStore os = getObjectStore();
        os.getProperties();
        
    	PropertyTemplateSet templates = os.get_PropertyTemplates();
        this.log("Retrieved PropertyTemplates", Project.MSG_VERBOSE);
        
    	@SuppressWarnings("unchecked")
		Iterator<PropertyTemplate> iter = templates.iterator();
        
        while(iter.hasNext())
        {
            this.log("Iterating PropertyTemplates", Project.MSG_VERBOSE);
        	PropertyTemplate pt = iter.next();
        	if(pt.get_SymbolicName().equalsIgnoreCase(symbolicName))
        	{
        		returnValue = pt;
        		this.log("Existing property Template '" +symbolicName+"' found", Project.MSG_INFO);
        		break;
        	}
        }

		return returnValue;
	}

	/**
	 * Sets the cardinality.
	 *
	 * @param cardinality the new cardinality
	 */
	public void setCardinality(int cardinality) {
    	this.cardinality = Cardinality.getInstanceFromInt(cardinality);
	}

	public void setDefaultDateTime(String defaultDateTime) {
        try {
			this.defaultDateTime = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(defaultDateTime);
		} catch (ParseException e) {
	        throw new BuildException(MessageFormat.format("The attribute ''{0}'' was not in the correct format, use 'MM/dd/yyyy HH:mm' ", "defaultDateTime"), getLocation() );
		}
	}

	public void setMaximumDateTime(String maximumDateTime) {
        try {
			this.maximumDateTime = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(maximumDateTime);
		} catch (ParseException e) {
	        throw new BuildException(MessageFormat.format("The attribute ''{0}'' was not in the correct format, use 'MM/dd/yyyy HH:mm' ", "maximumDateTime"), getLocation() );
		}
	}

	public void setMinimumDateTime(String minimumDateTime) {
        try {
			this.minimumDateTime = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(minimumDateTime);
		} catch (ParseException e) {
	        throw new BuildException(MessageFormat.format("The attribute ''{0}'' was not in the correct format, use 'MM/dd/yyyy HH:mm' ", "minimumDateTime"), getLocation() );
		}
	}

	/**
     * Validate input.
     */
    public void validateInput() {
    	this.log("Validating input", Project.MSG_VERBOSE);
        checkForNull("displayName", getDisplayName() );
        checkForNull("symbolicName", getSymbolicName() );
        checkForNull("cardinality", getCardinality() );
        checkForNull("objectStore", getObjectStore() );
    }
}
