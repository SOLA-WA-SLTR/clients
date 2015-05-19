package org.fao.sola.admin.web.beans.refdata;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.MessageBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.helpers.MessagesKeys;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.fao.sola.admin.web.beans.localization.LocalizedValuesListBean;
import org.sola.common.StringUtility;
import org.sola.common.logging.LogUtility;
import org.sola.services.common.EntityAction;
import org.sola.services.common.repository.entities.AbstractCodeEntity;
import org.sola.admin.services.ejb.refdata.businesslogic.RefDataAdminEJBLocal;
import org.sola.admin.services.ejb.refdata.entities.ApplicationStatusType;
import org.sola.admin.services.ejb.refdata.entities.Authority;
import org.sola.admin.services.ejb.refdata.entities.AvailabilityStatus;
import org.sola.admin.services.ejb.refdata.entities.BaUnitRelType;
import org.sola.admin.services.ejb.refdata.entities.BaUnitType;
import org.sola.admin.services.ejb.refdata.entities.BrSeverityType;
import org.sola.admin.services.ejb.refdata.entities.BrTechnicalType;
import org.sola.admin.services.ejb.refdata.entities.BrValidationTargetType;
import org.sola.admin.services.ejb.refdata.entities.CadastreObjectType;
import org.sola.admin.services.ejb.refdata.entities.ClaimStatus;
import org.sola.admin.services.ejb.refdata.entities.CommunicationType;
import org.sola.admin.services.ejb.refdata.entities.ConditionType;
import org.sola.admin.services.ejb.refdata.entities.ConfigPanelLauncher;
import org.sola.admin.services.ejb.refdata.entities.FieldConstraintType;
import org.sola.admin.services.ejb.refdata.entities.FieldType;
import org.sola.admin.services.ejb.refdata.entities.FieldValueType;
import org.sola.admin.services.ejb.refdata.entities.GenderType;
import org.sola.admin.services.ejb.refdata.entities.HierarchyLevel;
import org.sola.admin.services.ejb.refdata.entities.IdType;
import org.sola.admin.services.ejb.refdata.entities.LandUseType;
import org.sola.admin.services.ejb.refdata.entities.MapLayerType;
import org.sola.admin.services.ejb.refdata.entities.MortgageType;
import org.sola.admin.services.ejb.refdata.entities.NegotiateStatus;
import org.sola.admin.services.ejb.refdata.entities.NegotiateType;
import org.sola.admin.services.ejb.refdata.entities.NotationStatusType;
import org.sola.admin.services.ejb.refdata.entities.NotifyRelationshipType;
import org.sola.admin.services.ejb.refdata.entities.ObjectionStatus;
import org.sola.admin.services.ejb.refdata.entities.PanelLauncherGroup;
import org.sola.admin.services.ejb.refdata.entities.PartyRoleType;
import org.sola.admin.services.ejb.refdata.entities.PartyType;
import org.sola.admin.services.ejb.refdata.entities.PresentationFormType;
import org.sola.admin.services.ejb.refdata.entities.PublicDisplayStatus;
import org.sola.admin.services.ejb.refdata.entities.PublicDisplayType;
import org.sola.admin.services.ejb.refdata.entities.RegistrationStatusType;
import org.sola.admin.services.ejb.refdata.entities.RejectionReason;
import org.sola.admin.services.ejb.refdata.entities.RequestCategoryType;
import org.sola.admin.services.ejb.refdata.entities.RequestDisplayGroup;
import org.sola.admin.services.ejb.refdata.entities.Role;
import org.sola.admin.services.ejb.refdata.entities.RrrGroupType;
import org.sola.admin.services.ejb.refdata.entities.ServiceActionType;
import org.sola.admin.services.ejb.refdata.entities.ServiceStatusType;
import org.sola.admin.services.ejb.refdata.entities.TransactionStatusType;
import org.sola.admin.services.ejb.refdata.entities.TypeAction;
import org.sola.admin.services.ejb.refdata.entities.ValuationType;

/**
 * Generic reference data class to manage various reference data tables
 */
@Named
@ViewScoped
public class RefDataPage extends AbstractBackingBean {

    private AbstractCodeEntity refEntity;
    private List<AbstractCodeEntity> refEntityList;
    private Class refClass;
    private final String headerPrefix = "REFDATA_PAGE_";
    private String itemsHeader;
    private String itemHeader;

    @Inject
    MessageBean msg;

    @Inject
    MessageProvider msgProvider;

    @Inject
    private LanguageBean languageBean;
    
    LocalizedValuesListBean localizedDisplayValues;
    LocalizedValuesListBean localizedDescriptionValues;

    @EJB
    RefDataAdminEJBLocal refEjb;

    public AbstractCodeEntity getRefEntity() {
        return refEntity;
    }

    public void setRefEntity(AbstractCodeEntity refEntity) {
        this.refEntity = refEntity;
    }

    public List<AbstractCodeEntity> getRefEntityList() {
        return refEntityList;
    }

    public void setRefEntityList(List<AbstractCodeEntity> refEntityList) {
        this.refEntityList = refEntityList;
    }

    public String getItemsHeader() {
        return itemsHeader;
    }

    public void setItemsHeader(String itemsHeader) {
        this.itemsHeader = itemsHeader;
    }

    public String getItemHeader() {
        return itemHeader;
    }

    public void setItemHeader(String itemHeader) {
        this.itemHeader = itemHeader;
    }

    public LocalizedValuesListBean getLocalizedDisplayValues() {
        return localizedDisplayValues;
    }

    public LocalizedValuesListBean getLocalizedDescriptionValues() {
        return localizedDescriptionValues;
    }

    public RefDataPage() {
    }

    @PostConstruct
    private void init() {
        String type = getRequestParam("type");
        if (StringUtility.isEmpty(type)) {
            return;
        } else if (type.equalsIgnoreCase("role")) {
            refClass = Role.class;
        } else if (type.equalsIgnoreCase("app_status_type")){
            refClass = ApplicationStatusType.class;
        } else if (type.equalsIgnoreCase("availability_status")){
            refClass = AvailabilityStatus.class;
        } else if (type.equalsIgnoreCase("ba_unit_relation_type")){
            refClass = BaUnitRelType.class;
        } else if (type.equalsIgnoreCase("ba_unit_type")){
            refClass = BaUnitType.class;
        } else if (type.equalsIgnoreCase("br_severity_type")){
            refClass = BrSeverityType.class;
        } else if (type.equalsIgnoreCase("br_technical_type")){
            refClass = BrTechnicalType.class;
        } else  if (type.equalsIgnoreCase("br_validation_target_type")){
            refClass = BrValidationTargetType.class;
        } else if (type.equalsIgnoreCase("cadastre_object_type")){
            refClass = CadastreObjectType.class;
        } else if (type.equalsIgnoreCase("claim_status")){
            refClass = ClaimStatus.class;
        } else if (type.equalsIgnoreCase("communication_type")){
            refClass = CommunicationType.class;
        } else if (type.equalsIgnoreCase("condition_type")){
            refClass = ConditionType.class;
        } else if (type.equalsIgnoreCase("field_constraint_type")){
            refClass = FieldConstraintType.class;
        } else if (type.equalsIgnoreCase("field_type")){
            refClass = FieldType.class;
        } else if (type.equalsIgnoreCase("field_value_type")){
            refClass = FieldValueType.class;
        } else if (type.equalsIgnoreCase("gender_type")){
            refClass = GenderType.class;
        } else if (type.equalsIgnoreCase("hierarchy_level")){
            refClass = HierarchyLevel.class;
        }  else if (type.equalsIgnoreCase("id_type")){
            refClass = IdType.class;
        }  else if (type.equalsIgnoreCase("land_use_type")){
            refClass = LandUseType.class;
        }  else if (type.equalsIgnoreCase("mortgage_type")){
            refClass = MortgageType.class;
        }  else if (type.equalsIgnoreCase("party_role_type")){
            refClass = PartyRoleType.class;
        }  else if (type.equalsIgnoreCase("party_type")){
            refClass = PartyType.class;
        }  else if (type.equalsIgnoreCase("presentation_form_type")){
            refClass = PresentationFormType.class;
        }  else if (type.equalsIgnoreCase("reg_status_type")){
            refClass = RegistrationStatusType.class;
        }  else if (type.equalsIgnoreCase("rejection_reason")){
            refClass = RejectionReason.class;
        }  else if (type.equalsIgnoreCase("request_category_type")){
            refClass = RequestCategoryType.class;
        }  else if (type.equalsIgnoreCase("rrr_group_type")){
            refClass = RrrGroupType.class;
        }  else if (type.equalsIgnoreCase("service_action_type")){
            refClass = ServiceActionType.class;
        }  else if (type.equalsIgnoreCase("service_status_type")){
            refClass = ServiceStatusType.class;
        }  else if (type.equalsIgnoreCase("transaction_status_type")){
            refClass = TransactionStatusType.class;
        }  else if (type.equalsIgnoreCase("type_action")){
            refClass = TypeAction.class;
        }  else if (type.equalsIgnoreCase("panel_launcher_group")){
            refClass = PanelLauncherGroup.class;
        }  else if (type.equalsIgnoreCase("config_panel_launcher")){
            refClass = ConfigPanelLauncher.class;
        } else if (type.equalsIgnoreCase("MAP_LAYER_TYPE")){
            refClass = MapLayerType.class;
        } else if (type.equalsIgnoreCase("NOTATION_STATUS_TYPE")){
            refClass = NotationStatusType.class;
        } else if (type.equalsIgnoreCase("VALUATION_TYPE")){
            refClass = ValuationType.class;
        } else if (type.equalsIgnoreCase("REQUEST_DISPLAY_GROUP")){
            refClass = RequestDisplayGroup.class;
        } else if (type.equalsIgnoreCase("PUBLIC_DISPLAY_TYPE")){
            refClass = PublicDisplayType.class;
        } else if (type.equalsIgnoreCase("PUBLIC_DISPLAY_STATUS")){
            refClass = PublicDisplayStatus.class;
        } else if (type.equalsIgnoreCase("AUTHORITY")){
            refClass = Authority.class;
        } else if (type.equalsIgnoreCase("OBJECTION_STATUS")){
            refClass = ObjectionStatus.class;
        } else if (type.equalsIgnoreCase("NOTIFY_RELATIONSHIP_TYPE")){
            refClass = NotifyRelationshipType.class;
        } else if (type.equalsIgnoreCase("NEGOTIATE_TYPE")){
            refClass = NegotiateType.class;
        } else if (type.equalsIgnoreCase("NEGOTIATE_STATUS")){
            refClass = NegotiateStatus.class;
        }      

        itemsHeader = msgProvider.getMessage(headerPrefix + type.toUpperCase() + "S");
        itemHeader = msgProvider.getMessage(headerPrefix + type.toUpperCase());

        loadList();

        if (getRequestParam("action").equalsIgnoreCase("saved")) {
            msg.setSuccessMessage(msgProvider.getMessage(MessagesKeys.REFDATA_PAGE_SAVED));
        }
    }

    private void loadList() {
        refEntityList = refEjb.getCodeEntityList(refClass, languageBean.getLocale());
    }

    public void loadEntity(String code) {
        if (StringUtility.isEmpty(code)) {
            try {
                refEntity = (AbstractCodeEntity) refClass.newInstance();
                refEntity.setCode("");
            } catch (Exception ex) {
                LogUtility.log("Failed to instantiate reference data class", ex);
            }
        } else {
            refEntity = refEjb.getCodeEntity(refClass, code, null);
        }

        localizedDisplayValues = new LocalizedValuesListBean(languageBean);
        localizedDescriptionValues = new LocalizedValuesListBean(languageBean);
        
        localizedDisplayValues.loadLocalizedValues(refEntity.getDisplayValue());
        localizedDescriptionValues.loadLocalizedValues(refEntity.getDescription());
    }

    public void deleteEntity(AbstractCodeEntity entity) {
        entity.setEntityAction(EntityAction.DELETE);
        refEjb.saveCode(entity);
        loadList();
    }

    public void saveEntity() throws Exception {
        if (refEntity != null) {
            // Validate
            String errors = "";
            if (StringUtility.isEmpty(refEntity.getCode())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_FILL_CODE) + "\r\n";
            }
            if (StringUtility.isEmpty(refEntity.getStatus())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_SELECT_STATUS) + "\r\n";
            }
            if (localizedDisplayValues.getLocalizedValues() == null || localizedDisplayValues.getLocalizedValues().size() < 1
                    || StringUtility.isEmpty(localizedDisplayValues.getLocalizedValues().get(0).getLocalizedValue())) {
                errors += msgProvider.getErrorMessage(ErrorKeys.REFDATA_PAGE_FILL_DISPLAY_VALUE) + "\r\n";
            }

            if (!errors.equals("")) {
                throw new Exception(errors);
            }

            refEntity.setDisplayValue(localizedDisplayValues.buildMultilingualString());
            refEntity.setDescription(localizedDescriptionValues.buildMultilingualString());
            refEjb.saveCode(refEntity);
            loadList();
        }
    }
}
