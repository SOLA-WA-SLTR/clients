package org.fao.sola.admin.web.beans.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.ArrayUtils;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.FormConstraintOptionSorter;
import org.fao.sola.admin.web.beans.helpers.FormFieldSorter;
import org.fao.sola.admin.web.beans.helpers.FormSectionSorter;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.helpers.MessagesKeys;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.fao.sola.admin.web.beans.localization.LocalizedValuesListBean;
import org.sola.common.StringUtility;
import org.sola.common.mapping.MappingManager;
import org.sola.admin.opentenure.services.ejbs.claim.businesslogic.ClaimAdminEJBLocal;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FieldConstraint;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FieldConstraintOption;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FieldTemplate;
import org.sola.admin.opentenure.services.ejbs.claim.entities.FormTemplate;
import org.sola.admin.opentenure.services.ejbs.claim.entities.SectionTemplate;
import org.sola.services.common.EntityAction;
import org.sola.services.common.logging.LogUtility;
import org.sola.admin.services.ejb.refdata.businesslogic.RefDataAdminEJBLocal;
import org.sola.admin.services.ejb.refdata.entities.FieldConstraintType;
import org.sola.admin.services.ejb.refdata.entities.FieldType;

/**
 * Contains methods to manage dynamic forms
 */
@Named
@ViewScoped
public class FormsPageBean extends AbstractBackingBean {

    @Inject
    MessageProvider msgProvider;

    @EJB
    RefDataAdminEJBLocal refEjb;
    
    @EJB
    ClaimAdminEJBLocal claimEjb;

    @Inject
    private LanguageBean languageBean;
    
    private FormTemplate formTemplate;
    private SectionTemplate tmpSection;
    private FieldTemplate tmpField;
    private FieldConstraint tmpConstraint;
    private FieldConstraintOption tmpOption;
    private FieldType[] fieldTypes;
    private FieldConstraintType[] constraintTypes;
    private String message;
    private LocalizedValuesListBean localizedFormDisplayNames;
    private LocalizedValuesListBean localizedSectionError;
    private LocalizedValuesListBean localizedSectionDisplayNames;
    private LocalizedValuesListBean localizedSectionElementNames;
    private LocalizedValuesListBean localizedFieldNames;
    private LocalizedValuesListBean localizedConstraintNames;
    private LocalizedValuesListBean localizedConstraintsError;
    private LocalizedValuesListBean localizedOptionNames;
    
    public FormTemplate getFormTemplate() {
        return formTemplate;
    }

    public SectionTemplate getTmpSection() {
        return tmpSection;
    }

    public FieldTemplate getTmpField() {
        return tmpField;
    }

    public FieldConstraint getTmpConstraint() {
        return tmpConstraint;
    }

    public FieldConstraintOption getTmpOption() {
        return tmpOption;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FieldType[] getFieldTypes() {
        return fieldTypes;
    }

    public FieldConstraintType[] getConstraintTypes() {
        return constraintTypes;
    }

    public LocalizedValuesListBean getLocalizedFormDisplayNames() {
        return localizedFormDisplayNames;
    }
    
    public LocalizedValuesListBean getLocalizedSectionError(){
        return localizedSectionError;
    }
    
    public LocalizedValuesListBean getLocalizedSectionDisplayNames(){
        return localizedSectionDisplayNames;
    }
    
    public LocalizedValuesListBean getLocalizedSectionElementNames(){
        return localizedSectionElementNames;
    }

    public LocalizedValuesListBean getLocalizedFieldNames() {
        return localizedFieldNames;
    }

    public LocalizedValuesListBean getLocalizedConstraintNames() {
        return localizedConstraintNames;
    }

    public LocalizedValuesListBean getLocalizedConstraintsError() {
        return localizedConstraintsError;
    }

    public LocalizedValuesListBean getLocalizedOptionNames() {
        return localizedOptionNames;
    }
    
    public FormsPageBean() {
    }

    @PostConstruct
    private void init() {
        // Load lists
        List<FieldType> fieldTypesList = MappingManager.getMapper().map(refEjb.getCodeEntityList(FieldType.class, languageBean.getLocale()), List.class);
        List<FieldConstraintType> constraintTypesList = MappingManager.getMapper().map(refEjb.getCodeEntityList(FieldConstraintType.class, languageBean.getLocale()), List.class);
        
        if(fieldTypesList != null){
            FieldType dummy = new FieldType();
            dummy.setCode("");
            dummy.setDisplayValue(" ");
            fieldTypesList.add(0, dummy);
            fieldTypes = fieldTypesList.toArray(new FieldType[fieldTypesList.size()]);
        }
        
        if(constraintTypesList != null){
            FieldConstraintType dummy = new FieldConstraintType();
            dummy.setCode("");
            dummy.setDisplayValue(" ");
            constraintTypesList.add(0, dummy);
            constraintTypes = constraintTypesList.toArray(new FieldConstraintType[constraintTypesList.size()]);
        }
        
        // Check action code
        String action = getRequestParam("action");
        if (!StringUtility.isEmpty(action) && action.equalsIgnoreCase("saved")) {
            message = msgProvider.getMessage(MessagesKeys.FORMS_PAGE_FORM_SAVED);
        }

        // Init form template
        String id = getRequestParam("id");
        if (!StringUtility.isEmpty(id)) {
            formTemplate = claimEjb.getFormTemplate(id, null);
        }
        if (formTemplate == null) {
            formTemplate = new FormTemplate();
        }
      
        // Init tmp variables
        tmpSection = new SectionTemplate();
        tmpField = new FieldTemplate();
        tmpConstraint = new FieldConstraint();
        tmpOption = new FieldConstraintOption();
    }

    public void loadForm() {
        // Load localized values
        localizedFormDisplayNames = new LocalizedValuesListBean(languageBean);
        localizedFormDisplayNames.loadLocalizedValues(formTemplate.getDisplayName());
    }
    
    public SectionTemplate[] getSections() {
        if (formTemplate == null) {
            return null;
        }
        if (formTemplate.getSectionTemplateList() == null) {
            formTemplate.setSectionTemplateList(new ArrayList<SectionTemplate>());
        }

        List<SectionTemplate> sections = new ArrayList<>();
        for (SectionTemplate sec : formTemplate.getSectionTemplateList()) {
            if (sec.getEntityAction() == null || sec.getEntityAction() != EntityAction.DELETE) {
                sections.add(sec);
            }
        }
        return sections.toArray(new SectionTemplate[sections.size()]);
    }

    public void saveFormHeader() throws Exception {
        // Check for errors
        String errors = "";
        if (!localizedFormDisplayNames.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_DISPLAY_NAME);
        }
        
        formTemplate.setDisplayName(localizedFormDisplayNames.buildMultilingualString());
        
        if (!errors.equals("")) {
            throw new Exception(errors);
        }
    }

    public void editSection(SectionTemplate sec) {
        if (sec == null) {
            tmpSection = new SectionTemplate();
            tmpSection.setId(UUID.randomUUID().toString());
            tmpSection.setFormTemplateName(formTemplate.getName());
            tmpSection.setFieldTemplateList(new ArrayList<FieldTemplate>());
            tmpSection.setItemOrder(1);
        } else {
            tmpSection = MappingManager.getMapper().map(sec, SectionTemplate.class);
        }
        
        localizedSectionError = new LocalizedValuesListBean(languageBean);
        localizedSectionError.loadLocalizedValues(tmpSection.getErrorMsg());
        
        localizedSectionDisplayNames = new LocalizedValuesListBean(languageBean);
        localizedSectionDisplayNames.loadLocalizedValues(tmpSection.getDisplayName());
        
        localizedSectionElementNames = new LocalizedValuesListBean(languageBean);
        localizedSectionElementNames.loadLocalizedValues(tmpSection.getElementDisplayName());
    }

    public void deleteSection(SectionTemplate sec) {
        if (sec != null) {
            if (sec.isNew()) {
                // Just delete from the list
                int i = 0;
                for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
                    if (sec.getId().equals(secTmpl.getId())) {
                        formTemplate.getSectionTemplateList().remove(i);
                        return;
                    }
                    i += 1;
                }
            } else {
                // Set entity action
                sec.setEntityAction(EntityAction.DELETE);
            }
        }
    }

    public void saveSection() throws Exception {
        if (tmpSection == null) {
            return;
        }

        // Check for errors
        String errors = "";
        if (StringUtility.isEmpty(tmpSection.getName())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_NAME) + "\r\n";
        }
        if (!localizedSectionDisplayNames.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_DISPLAY_NAME) + "\r\n";
        }
        if (StringUtility.isEmpty(tmpSection.getElementName())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_ELEMENT_NAME) + "\r\n";
        }
        if (!localizedSectionElementNames.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_ELEMENT_DISPLAY_NAME) + "\r\n";
        }
        if (!localizedSectionError.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_ERROR_MESSAGE) + "\r\n";
        }
        if (tmpSection.getMinOccurrences() > tmpSection.getMaxOccurrences()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_MIN_OCCUR_GRATER_MAX_OCCUR) + "\r\n";
        }
        // check for name duplication
        if (formTemplate.getSectionTemplateList() != null) {
            // Check if section already exists
            for (SectionTemplate templ : formTemplate.getSectionTemplateList()) {
                if (templ.getEntityAction() != EntityAction.DELETE && 
                        !templ.getId().equalsIgnoreCase(tmpSection.getId()) &&
                        StringUtility.empty(templ.getName()).equalsIgnoreCase(StringUtility.empty(tmpSection.getName()))) {
                    errors += String.format(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_SECTION_NAME_DUPLICATION), tmpSection.getName()) + "\r\n";
                    break;
                }
            }
        }
        if (!errors.equals("")) {
            throw new Exception(errors);
        }

        tmpSection.setErrorMsg(localizedSectionError.buildMultilingualString());
        tmpSection.setDisplayName(localizedSectionDisplayNames.buildMultilingualString());
        tmpSection.setElementDisplayName(localizedSectionElementNames.buildMultilingualString());
        
        if (formTemplate.getSectionTemplateList() != null) {
            // Check if section already exists
            for (SectionTemplate templ : formTemplate.getSectionTemplateList()) {
                if (templ.getId().equals(tmpSection.getId())) {
                    MappingManager.getMapper().map(tmpSection, templ);
                    Collections.sort(formTemplate.getSectionTemplateList(), new FormSectionSorter());
                    return;
                }
            }
            // Otherwise add it
            formTemplate.getSectionTemplateList().add(tmpSection);
            Collections.sort(formTemplate.getSectionTemplateList(), new FormSectionSorter());
        }
    }

    public FieldTemplate[] getFields(SectionTemplate sec) {
        if (sec == null) {
            return null;
        }
        if (sec.getFieldTemplateList() == null) {
            sec.setFieldTemplateList(new ArrayList<FieldTemplate>());
        }

        List<FieldTemplate> fields = new ArrayList<>();
        for (FieldTemplate fTempl : sec.getFieldTemplateList()) {
            if (fTempl.getEntityAction() == null || fTempl.getEntityAction() != EntityAction.DELETE) {
                fields.add(fTempl);
            }
        }
        return fields.toArray(new FieldTemplate[fields.size()]);
    }

    public void editField(FieldTemplate fTmpl, String sectionId) {
        if (fTmpl == null) {
            tmpField = new FieldTemplate();
            tmpField.setId(UUID.randomUUID().toString());
            tmpField.setSectionTemplateId(sectionId);
            tmpField.setItemOrder(1);
            tmpField.setFieldConstraintList(new ArrayList<FieldConstraint>());
        } else {
            tmpField = MappingManager.getMapper().map(fTmpl, FieldTemplate.class);
        }
        localizedFieldNames = new LocalizedValuesListBean(languageBean);
        localizedFieldNames.loadLocalizedValues(tmpField.getDisplayName());
    }

    public void deleteField(FieldTemplate fTmpl, SectionTemplate sec) {
        if (fTmpl != null && sec != null) {
            if (fTmpl.isNew()) {
                // Just delete from the list
                int i = 0;
                for (FieldTemplate fTmpl2 : sec.getFieldTemplateList()) {
                    if (fTmpl2.getId().equals(fTmpl.getId())) {
                        sec.getFieldTemplateList().remove(i);
                        return;
                    }
                    i += 1;
                }
            } else {
                // Set entity action
                fTmpl.setEntityAction(EntityAction.DELETE);
            }
        }
    }

    public void saveField() throws Exception {
        if (tmpField == null) {
            return;
        }

        // Check for errors
        String errors = "";
        if (StringUtility.isEmpty(tmpField.getName())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_NAME) + "\r\n";
        }
        if (!localizedFieldNames.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_DISPLAY_NAME) + "\r\n";
        }
        if (StringUtility.isEmpty(tmpField.getHint())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_HINT) + "\r\n";
        }
        if (StringUtility.isEmpty(tmpField.getFieldType())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_SELECT_FIELD_TYPE) + "\r\n";
        }
        // check for name duplication
        if (formTemplate.getSectionTemplateList() != null) {
            // Look for section 
            for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
                if (secTmpl.getId().equals(tmpField.getSectionTemplateId())) {
                    // Look for field
                    for (FieldTemplate fTmpl : secTmpl.getFieldTemplateList()) {
                        if (fTmpl.getEntityAction() != EntityAction.DELETE && 
                                !fTmpl.getId().equalsIgnoreCase(tmpField.getId()) &&
                                StringUtility.empty(fTmpl.getName()).equalsIgnoreCase(StringUtility.empty(tmpField.getName()))) {
                            errors += String.format(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FIELD_NAME_DUPLICATION), tmpField.getName()) + "\r\n";
                            break;
                        }
                    }
                    break;
                }
            }
        }
        
        if (!errors.equals("")) {
            throw new Exception(errors);
        }

        tmpField.setDisplayName(localizedFieldNames.buildMultilingualString());
        
        if (formTemplate.getSectionTemplateList() != null) {
            // Look for section 
            for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
                if (secTmpl.getId().equals(tmpField.getSectionTemplateId())) {
                    // Check if field already exists
                    for (FieldTemplate fTmpl : secTmpl.getFieldTemplateList()) {
                        if (fTmpl.getId().equals(tmpField.getId())) {
                            MappingManager.getMapper().map(tmpField, fTmpl);
                            Collections.sort(secTmpl.getFieldTemplateList(), new FormFieldSorter());
                            return;
                        }
                    }
                    // Otherwise add it
                    secTmpl.getFieldTemplateList().add(tmpField);
                    Collections.sort(secTmpl.getFieldTemplateList(), new FormFieldSorter());
                    return;
                }
            }
        }
    }

    public FieldConstraint[] getConstraints(FieldTemplate fTmpl) {
        if (fTmpl == null) {
            return null;
        }
        if (fTmpl.getFieldConstraintList() == null) {
            fTmpl.setFieldConstraintList(new ArrayList<FieldConstraint>());
        }

        List<FieldConstraint> constraints = new ArrayList<>();
        for (FieldConstraint fContsr : fTmpl.getFieldConstraintList()) {
            if (fContsr.getEntityAction() == null || fContsr.getEntityAction() != EntityAction.DELETE) {
                constraints.add(fContsr);
            }
        }
        return constraints.toArray(new FieldConstraint[constraints.size()]);
    }

    public void editConstraint(FieldConstraint fConstr, String fieldId) {
        if (fConstr == null) {
            tmpConstraint = new FieldConstraint();
            tmpConstraint.setId(UUID.randomUUID().toString());
            tmpConstraint.setFieldTemplateId(fieldId);
            tmpConstraint.setFieldConstraintOptionList(new ArrayList<FieldConstraintOption>());
        } else {
            tmpConstraint = MappingManager.getMapper().map(fConstr, FieldConstraint.class);
        }
        localizedConstraintNames = new LocalizedValuesListBean(languageBean);
        localizedConstraintNames.loadLocalizedValues(tmpConstraint.getDisplayName());
        localizedConstraintsError = new LocalizedValuesListBean(languageBean);
        localizedConstraintsError.loadLocalizedValues(tmpConstraint.getErrorMsg());
    }

    public void deleteConstraint(FieldConstraint fConstr, FieldTemplate fTmpl) {
        if (fConstr != null && fTmpl != null) {
            if (fConstr.isNew()) {
                // Just delete from the list
                int i = 0;
                for (FieldConstraint fConstr2 : fTmpl.getFieldConstraintList()) {
                    if (fConstr2.getId().equals(fConstr.getId())) {
                        fTmpl.getFieldConstraintList().remove(i);
                        return;
                    }
                    i += 1;
                }
            } else {
                // Set entity action
                fConstr.setEntityAction(EntityAction.DELETE);
            }
        }
    }

    public void saveConstraint() throws Exception {
        if (tmpConstraint == null) {
            return;
        }

        // Check for errors
        String errors = "";
        if (StringUtility.isEmpty(tmpConstraint.getName())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_NAME) + "\r\n";
        }
        if (!localizedConstraintNames.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_DISPLAY_NAME) + "\r\n";
        }
        if (!localizedConstraintsError.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_ERROR_MESSAGE) + "\r\n";
        }
        if (StringUtility.isEmpty(tmpConstraint.getFieldConstraintType())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_SELECT_FIELD_CONSTRAINT_TYPE) + "\r\n";
        }
        // check for name duplication
        if (formTemplate.getSectionTemplateList() != null) {
            for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
                if (secTmpl.getFieldTemplateList() != null) {
                    // Look for field 
                    for (FieldTemplate fTmpl : secTmpl.getFieldTemplateList()) {
                        if (fTmpl.getId().equals(tmpConstraint.getFieldTemplateId())) {
                            // Found field, look for constraint
                            if (fTmpl.getFieldConstraintList() != null) {
                                for (FieldConstraint fConstr : fTmpl.getFieldConstraintList()) {
                                    if (fConstr.getEntityAction() != EntityAction.DELETE && 
                                            !fConstr.getId().equalsIgnoreCase(tmpConstraint.getId()) &&
                                            StringUtility.empty(fConstr.getName()).equalsIgnoreCase(StringUtility.empty(tmpConstraint.getName()))) {
                                        errors += String.format(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_CONSTRAINT_NAME_DUPLICATION), tmpConstraint.getName()) + "\r\n";
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
       
        if (!errors.equals("")) {
            throw new Exception(errors);
        }

        tmpConstraint.setDisplayName(localizedConstraintNames.buildMultilingualString());
        tmpConstraint.setErrorMsg(localizedConstraintsError.buildMultilingualString());
        
        if (formTemplate.getSectionTemplateList() != null) {
            // Loop through sections 
            for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
                if (secTmpl.getFieldTemplateList() != null) {
                    // Look for field 
                    for (FieldTemplate fTmpl : secTmpl.getFieldTemplateList()) {
                        if (fTmpl.getId().equals(tmpConstraint.getFieldTemplateId())) {
                            // Found field, look for constraint
                            if (fTmpl.getFieldConstraintList() != null) {
                                for (FieldConstraint fConstr : fTmpl.getFieldConstraintList()) {
                                    if (fConstr.getId().equals(tmpConstraint.getId())) {
                                        // Found constraint
                                        MappingManager.getMapper().map(tmpConstraint, fConstr);
                                        return;
                                    }
                                }
                                // Otherwise add it
                                fTmpl.getFieldConstraintList().add(tmpConstraint);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public FieldConstraintOption[] getOptions(FieldConstraint fConstr) {
        if (fConstr == null) {
            return null;
        }
        if (fConstr.getFieldConstraintOptionList() == null) {
            fConstr.setFieldConstraintOptionList(new ArrayList<FieldConstraintOption>());
        }

        List<FieldConstraintOption> options = new ArrayList<>();
        for (FieldConstraintOption option : fConstr.getFieldConstraintOptionList()) {
            if (option.getEntityAction() == null || option.getEntityAction() != EntityAction.DELETE) {
                options.add(option);
            }
        }
        return options.toArray(new FieldConstraintOption[options.size()]);
    }

    public void editOption(FieldConstraintOption option, String constrId) {
        if (option == null) {
            tmpOption = new FieldConstraintOption();
            tmpOption.setId(UUID.randomUUID().toString());
            tmpOption.setItemOrder(1);
            tmpOption.setFieldConstraintId(constrId);
        } else {
            tmpOption = MappingManager.getMapper().map(option, FieldConstraintOption.class);
        }
        localizedOptionNames = new LocalizedValuesListBean(languageBean);
        localizedOptionNames.loadLocalizedValues(tmpOption.getDisplayName());
    }

    public void deleteOption(FieldConstraintOption option, FieldConstraint fConstr) {
        if (option != null && fConstr != null) {
            if (option.isNew()) {
                // Just delete from the list
                int i = 0;
                for (FieldConstraintOption option2 : fConstr.getFieldConstraintOptionList()) {
                    if (option2.getId().equals(option.getId())) {
                        fConstr.getFieldConstraintOptionList().remove(i);
                        return;
                    }
                    i += 1;
                }
            } else {
                // Set entity action
                option.setEntityAction(EntityAction.DELETE);
            }
        }
    }

    public void saveOption() throws Exception {
        if (tmpOption == null) {
            return;
        }

        // Check for errors
        String errors = "";
        if (StringUtility.isEmpty(tmpOption.getName())) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_NAME) + "\r\n";
        }
        if (!localizedOptionNames.hasValues()) {
            errors += msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_DISPLAY_NAME) + "\r\n";
        }
        // check for name duplication
        for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
            if (secTmpl.getFieldTemplateList() != null) {
                // Loop through fields 
                for (FieldTemplate fTmpl : secTmpl.getFieldTemplateList()) {
                    if (fTmpl.getFieldConstraintList() != null) {
                        // Loop through constraints
                        for (FieldConstraint fConstr : fTmpl.getFieldConstraintList()) {
                            if (fConstr.getId().equals(tmpOption.getFieldConstraintId())) {
                                // Found constaint, loop through options
                                if (fConstr.getFieldConstraintOptionList() != null) {
                                    for (FieldConstraintOption option : fConstr.getFieldConstraintOptionList()) {
                                        if (fConstr.getEntityAction() != EntityAction.DELETE && 
                                                !option.getId().equalsIgnoreCase(tmpOption.getId()) &&
                                                StringUtility.empty(option.getName()).equalsIgnoreCase(StringUtility.empty(tmpOption.getName()))) {
                                            errors += String.format(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_CONSTRAINT_OPTION_NAME_DUPLICATION), tmpOption.getName()) + "\r\n";
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!errors.equals("")) {
            throw new Exception(errors);
        }

        tmpOption.setDisplayName(localizedOptionNames.buildMultilingualString());
        
        if (formTemplate.getSectionTemplateList() == null) {
            return;
        }

        // Loop through sections 
        for (SectionTemplate secTmpl : formTemplate.getSectionTemplateList()) {
            if (secTmpl.getFieldTemplateList() != null) {
                // Loop through fields 
                for (FieldTemplate fTmpl : secTmpl.getFieldTemplateList()) {
                    if (fTmpl.getFieldConstraintList() != null) {
                        // Loop through constraints
                        for (FieldConstraint fConstr : fTmpl.getFieldConstraintList()) {
                            if (fConstr.getId().equals(tmpOption.getFieldConstraintId())) {
                                // Found constaint, loop through options
                                if (fConstr.getFieldConstraintOptionList() != null) {
                                    for (FieldConstraintOption option : fConstr.getFieldConstraintOptionList()) {
                                        if (option.getId().equals(tmpOption.getId())) {
                                            // Found option
                                            MappingManager.getMapper().map(tmpOption, option);
                                            Collections.sort(fConstr.getFieldConstraintOptionList(), new FormConstraintOptionSorter());
                                            return;
                                        }
                                    }
                                    // Otherwise add it
                                    fConstr.getFieldConstraintOptionList().add(tmpOption);
                                    Collections.sort(fConstr.getFieldConstraintOptionList(), new FormConstraintOptionSorter());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void saveForm() {
        try {
            // Check form
            if (StringUtility.isEmpty(formTemplate.getDisplayName())) {
                getContext().addMessage(null, new FacesMessage(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_FILL_DISPLAY_NAME)));
                return;
            }

            if (formTemplate.getSectionTemplateList() == null || getSections().length < 1) {
                getContext().addMessage(null, new FacesMessage(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_ADD_1_SECTION)));
                return;
            }

            // Loop though sections
            for (SectionTemplate sec : formTemplate.getSectionTemplateList()) {
                // Assign form name to the section to make sure it's the same
                sec.setFormTemplateName(formTemplate.getName());
                // Check fields
                if (sec.getEntityAction() == null && getFields(sec).length < 1) {
                    getContext().addMessage(null, new FacesMessage(String.format(msgProvider.getErrorMessage(ErrorKeys.FORMS_PAGE_ADD_1_FIELD), sec.getName())));
                    return;
                }
            }

            // Do save
            runUpdate(new Runnable() {
                @Override
                public void run() {
                    formTemplate = claimEjb.saveFormTemplate(formTemplate);
                }
            });

            try {
                getContext().getExternalContext().redirect(getRequest().getContextPath() + "/forms/Form.xhtml?id=" + formTemplate.getName() + "&action=saved");
            } catch (Exception e) {
                LogUtility.log(msgProvider.getMessage(ErrorKeys.GENERAL_REDIRECT_FAILED), e);
            }
        } catch (Exception e) {
            getContext().addMessage(null, new FacesMessage(processException(e, languageBean.getLocale()).getMessage()));
        }
    }
}
