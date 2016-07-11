package org.fao.sola.admin.web.beans.refdata;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.fao.sola.admin.web.beans.localization.LocalizedValuesListBean;
import org.sola.common.StringUtility;
import org.sola.common.logging.LogUtility;
import org.sola.services.common.EntityAction;
import org.sola.services.common.repository.entities.AbstractCodeEntity;
import org.sola.admin.services.ejb.refdata.businesslogic.RefDataAdminEJBLocal;
import org.sola.admin.services.ejb.refdata.entities.CheckListGroup;
import org.sola.admin.services.ejb.refdata.entities.CheckListItem;
import org.sola.admin.services.ejb.refdata.entities.CheckListItemInGroup;

/**
 * Contains methods and properties to manage {@link CheckListGroup}
 */
@Named
@ViewScoped
public class ChecklistGroupPageBean extends AbstractBackingBean {
    private CheckListGroup checkListGroup;
    private List<CheckListGroup> checkListGroupList;
    private CheckListItem[] checklistItems;
    private String[] selectedItemCodes;
    
    @Inject
    MessageProvider msgProvider;

    @Inject
    private LanguageBean languageBean;
    
    LocalizedValuesListBean localizedDisplayValues;
    LocalizedValuesListBean localizedDescriptionValues;

    @EJB
    RefDataAdminEJBLocal refEjb;

    public CheckListGroup getCheckListGroup() {
        return checkListGroup;
    }

    public List<CheckListGroup> getCheckListGroupList() {
        return checkListGroupList;
    }

    public CheckListItem[] getChecklistItems() {
        return checklistItems;
    }

    public String[] getSelectedItemCodes() {
        return selectedItemCodes;
    }

    public void setSelectedItemCodes(String[] selectedItemCodes) {
        this.selectedItemCodes = selectedItemCodes;
    }

    public LocalizedValuesListBean getLocalizedDisplayValues() {
        return localizedDisplayValues;
    }

    public LocalizedValuesListBean getLocalizedDescriptionValues() {
        return localizedDescriptionValues;
    }
    
    public ChecklistGroupPageBean(){
        super();
    }
    
    @PostConstruct
    private void init() {
        loadList();
        // Load supporting lists
        List<CheckListItem> checklistItemList = refEjb.getCodeEntityList(CheckListItem.class, languageBean.getLocale());
        if (checklistItemList != null) {
            checklistItemList = (ArrayList<CheckListItem>)((ArrayList)checklistItemList).clone();
            checklistItems = checklistItemList.toArray(new CheckListItem[checklistItemList.size()]);
        }
    }
    
    private void loadList() {
        checkListGroupList = refEjb.getCodeEntityList(CheckListGroup.class, languageBean.getLocale());
    }
    
    public void loadEntity(String code) {
        if (StringUtility.isEmpty(code)) {
            try {
                checkListGroup = new CheckListGroup();
                checkListGroup.setCode("");
                checkListGroup.setCheckListItems(new ArrayList<CheckListItemInGroup>());
            } catch (Exception ex) {
                LogUtility.log("Failed to instantiate reference data class", ex);
            }
        } else {
            checkListGroup = refEjb.getCodeEntity(CheckListGroup.class, code, null);
        }

        // Select/unselect source types
        List<CheckListItem> checklistItemsToSelect = new ArrayList<>();
        if (checkListGroup.getCheckListItems()!= null) {
            for (CheckListItemInGroup itm : checkListGroup.getCheckListItems()) {
                if (checklistItems != null) {
                    for (CheckListItem item : checklistItems) {
                        if (itm.getCheckListItemCode().equalsIgnoreCase(item.getCode())) {
                            checklistItemsToSelect.add(item);
                            break;
                        }
                    }
                }
            }
        }
        selectedItemCodes = new String[checklistItemsToSelect.size()];
        int i=0;
        for(CheckListItem item : checklistItemsToSelect){
            selectedItemCodes[i] = item.getCode();
            i+=1;
        }

        localizedDisplayValues = new LocalizedValuesListBean(languageBean);
        localizedDescriptionValues = new LocalizedValuesListBean(languageBean);
        
        localizedDisplayValues.loadLocalizedValues(checkListGroup.getDisplayValue());
        localizedDescriptionValues.loadLocalizedValues(checkListGroup.getDescription());
    }
    
    public void deleteEntity(AbstractCodeEntity entity) {
        entity.setEntityAction(EntityAction.DELETE);
        refEjb.saveCode(entity);
        loadList();
    }
    
    public void saveEntity() throws Exception {
        if (checkListGroup != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(checkListGroup.getCode())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_FILL_CODE) + "\r\n";
            }
            if (StringUtility.isEmpty(checkListGroup.getStatus())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_SELECT_STATUS) + "\r\n";
            }
            if (localizedDisplayValues.getLocalizedValues() == null || localizedDisplayValues.getLocalizedValues().size() < 1
                    || StringUtility.isEmpty(localizedDisplayValues.getLocalizedValues().get(0).getLocalizedValue())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_FILL_DISPLAY_VALUE) + "\r\n";
            }

            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            checkListGroup.setDisplayValue(localizedDisplayValues.buildMultilingualString());
            checkListGroup.setDescription(localizedDescriptionValues.buildMultilingualString());

            // Prepare checklist items related to checklist group
            // Delete
            if (checkListGroup.getCheckListItems()!= null) {
                for (CheckListItemInGroup itm : checkListGroup.getCheckListItems()) {
                    boolean found = false;
                    if (selectedItemCodes != null) {
                        for (String code : selectedItemCodes) {
                            if (itm.getCheckListItemCode().equalsIgnoreCase(code)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        itm.setEntityAction(EntityAction.DELETE);
                    }
                }
            }
            
            // Add
            if (selectedItemCodes != null) {
                for (String code : selectedItemCodes) {
                    boolean found = false;
                    if (checkListGroup.getCheckListItems()!= null) {
                        for (CheckListItemInGroup itm : checkListGroup.getCheckListItems()) {
                            if (itm.getCheckListItemCode().equalsIgnoreCase(code)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if(!found){
                        CheckListItemInGroup itm = new CheckListItemInGroup();
                        itm.setCheckListGroupCode(checkListGroup.getCode());
                        itm.setCheckListItemCode(code);
                        checkListGroup.getCheckListItems().add(itm);
                    }
                }
            }

            refEjb.saveCode(checkListGroup);
            loadList();
        }
    }
}
