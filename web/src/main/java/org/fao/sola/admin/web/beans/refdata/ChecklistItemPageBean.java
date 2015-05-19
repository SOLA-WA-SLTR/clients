package org.fao.sola.admin.web.beans.refdata;

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
import org.sola.admin.services.ejb.refdata.entities.CheckListItem;

/**
 * Contains methods and properties to manage {@link CheckListItem}
 */
@Named
@ViewScoped
public class ChecklistItemPageBean extends AbstractBackingBean {
    private CheckListItem checkListItem;
    private List<CheckListItem> checkListItemList;
    
    @Inject
    MessageProvider msgProvider;

    @Inject
    private LanguageBean languageBean;
    
    LocalizedValuesListBean localizedDisplayValues;
    LocalizedValuesListBean localizedDescriptionValues;

    @EJB
    RefDataAdminEJBLocal refEjb;

    public CheckListItem getCheckListItem() {
        return checkListItem;
    }

    public void setCheckListItem(CheckListItem checkListItem) {
        this.checkListItem = checkListItem;
    }

    public List<CheckListItem> getCheckListItemList() {
        return checkListItemList;
    }

    public LocalizedValuesListBean getLocalizedDisplayValues() {
        return localizedDisplayValues;
    }

    public LocalizedValuesListBean getLocalizedDescriptionValues() {
        return localizedDescriptionValues;
    }
    
    @PostConstruct
    private void init() {
        loadList();
    }
    
    private void loadList() {
        checkListItemList = refEjb.getCodeEntityList(CheckListItem.class, languageBean.getLocale());
    }
    
    public void loadEntity(String code) {
        if (StringUtility.isEmpty(code)) {
            try {
                checkListItem = new CheckListItem();
                checkListItem.setCode("");
            } catch (Exception ex) {
                LogUtility.log("Failed to instantiate reference data class", ex);
            }
        } else {
            checkListItem = refEjb.getCodeEntity(CheckListItem.class, code, null);
        }

        localizedDisplayValues = new LocalizedValuesListBean(languageBean);
        localizedDescriptionValues = new LocalizedValuesListBean(languageBean);
        
        localizedDisplayValues.loadLocalizedValues(checkListItem.getDisplayValue());
        localizedDescriptionValues.loadLocalizedValues(checkListItem.getDescription());
    }

    public void deleteEntity(AbstractCodeEntity entity) {
        entity.setEntityAction(EntityAction.DELETE);
        refEjb.saveCode(entity);
        loadList();
    }

    public void saveEntity() throws Exception {
        if (checkListItem != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(checkListItem.getCode())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_FILL_CODE) + "\r\n";
            }
            if (StringUtility.isEmpty(checkListItem.getStatus())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_SELECT_STATUS) + "\r\n";
            }
            if (localizedDisplayValues.getLocalizedValues() == null || localizedDisplayValues.getLocalizedValues().size() < 1
                    || StringUtility.isEmpty(localizedDisplayValues.getLocalizedValues().get(0).getLocalizedValue())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_FILL_DISPLAY_VALUE) + "\r\n";
            }

            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            checkListItem.setDisplayValue(localizedDisplayValues.buildMultilingualString());
            checkListItem.setDescription(localizedDescriptionValues.buildMultilingualString());
            refEjb.saveCode(checkListItem);
            loadList();
        }
    }
}
